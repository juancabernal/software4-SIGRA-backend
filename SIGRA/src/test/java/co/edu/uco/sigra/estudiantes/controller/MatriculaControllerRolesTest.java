package co.edu.uco.sigra.estudiantes.controller;

import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.asignaturas.seguridad.ResolvedorIdentidad;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.exception.AsercionesContratoError;
import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.common.exception.ManejadorErroresTransversal;
import co.edu.uco.sigra.estudiantes.exception.ManejadorErroresEstudiantes;
import co.edu.uco.sigra.estudiantes.mapper.MatriculaMapperImpl;
import co.edu.uco.sigra.estudiantes.seguridad.ControlAccesoMatriculaInterceptor;
import co.edu.uco.sigra.estudiantes.service.MatriculaService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extremo a extremo de la capa web del control de acceso por rol de matrículas (spec
 * «Los endpoints de matrícula son de ADMIN y PROFESOR», cambio {@code matricula-desvinculacion}):
 * {@link ControlAccesoMatriculaInterceptor} real, los manejadores de errores reales y un
 * {@link ResolvedorIdentidad} mockeado que entrega la identidad según el token de prueba
 * («Bearer admin», etc.), igual que {@code AsignaturaControllerRolesTest} hace para su módulo.
 *
 * <p>Este archivo cubre solo el interruptor <strong>encendido</strong>: con el interruptor apagado
 * (su valor por defecto) la evidencia de no-regresión es que {@code MatriculaControllerTest}, que
 * no toca la propiedad, sigue pasando sin cambios.
 */
@DisplayName("MatriculaController con control de acceso por rol (interruptor encendido)")
class MatriculaControllerRolesTest {

    private static final String URL = "/api/v1/matriculas";
    private static final Map<String, IdentidadActual> TOKENS = Map.of(
            "admin", new IdentidadActual(UUID.randomUUID(), RolUsuario.ADMINISTRADOR),
            "profesor", new IdentidadActual(UUID.randomUUID(), RolUsuario.PROFESOR),
            "estudiante", new IdentidadActual(UUID.randomUUID(), RolUsuario.ESTUDIANTE));

    private final MatriculaService matriculaService = mock(MatriculaService.class);
    private final ResolvedorIdentidad resolvedor = mock(ResolvedorIdentidad.class);

    @SuppressWarnings("unchecked")
    private MockMvc mockMvc() {
        when(resolvedor.resolver(any())).thenAnswer(inv -> {
            String encabezado = ((HttpServletRequest) inv.getArgument(0)).getHeader("Authorization");
            return encabezado == null ? Optional.empty()
                    : Optional.ofNullable(TOKENS.get(encabezado.replace("Bearer ", "")));
        });
        ObjectProvider<ResolvedorIdentidad> proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(resolvedor);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(new MatriculaController(matriculaService, new MatriculaMapperImpl()))
                .addInterceptors(new ControlAccesoMatriculaInterceptor(proveedor, true))
                .setControllerAdvice(new ManejadorErroresEstudiantes(), new ManejadorErroresTransversal(),
                        new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private static String cuerpoMatricular(UUID estudianteId, UUID asignaturaId, UUID semestreId) {
        return """
                {
                  "estudianteId": "%s",
                  "asignaturaId": "%s",
                  "semestreId": "%s"
                }
                """.formatted(estudianteId, asignaturaId, semestreId);
    }

    private static String cuerpoRegistrarYMatricular(UUID asignaturaId, UUID semestreId) {
        return """
                {
                  "estudiante": {
                    "tipoDocumentoId": "%s",
                    "numeroDocumento": "1098765432",
                    "nombreCompleto": "Ana Gómez",
                    "correoInstitucional": "ana.gomez@uco.net.co"
                  },
                  "asignaturaId": "%s",
                  "semestreId": "%s"
                }
                """.formatted(UUID.randomUUID(), asignaturaId, semestreId);
    }

    @Nested
    @DisplayName("El rechazo por rol no filtra si el recurso existe")
    class RechazoPorRolNoFiltraExistencia {

        private final MockMvc mvc = mockMvc();

        @Test
        @DisplayName("ESTUDIANTE que desvincula una matrícula inexistente recibe 403, no 404, y el servicio no se invoca")
        void desvincularMatriculaInexistenteDevuelve403() throws Exception {
            UUID idInexistente = UUID.randomUUID();

            MvcResult resultado = mvc.perform(delete(URL + "/" + idInexistente)
                            .header("Authorization", "Bearer estudiante"))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.FORBIDDEN);
            verifyNoInteractions(matriculaService);
        }

        @Test
        @DisplayName("ESTUDIANTE que consulta matriculados con asignatura y semestre inexistentes recibe 403, no 404")
        void consultarMatriculadosConReferenciasInexistentesDevuelve403() throws Exception {
            MvcResult resultado = mvc.perform(get(URL)
                            .header("Authorization", "Bearer estudiante")
                            .param("asignaturaId", UUID.randomUUID().toString())
                            .param("semestreId", UUID.randomUUID().toString()))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.FORBIDDEN);
            verifyNoInteractions(matriculaService);
        }

        @Test
        @DisplayName("ESTUDIANTE que usa el alta en un paso con asignatura y semestre inexistentes recibe 403, no 404")
        void registrarYMatricularConReferenciasInexistentesDevuelve403() throws Exception {
            MvcResult resultado = mvc.perform(post(URL + "/con-estudiante")
                            .header("Authorization", "Bearer estudiante")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoRegistrarYMatricular(UUID.randomUUID(), UUID.randomUUID())))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.FORBIDDEN);
            verifyNoInteractions(matriculaService);
        }

        @Test
        @DisplayName("ESTUDIANTE que matricula recibe 403 sin invocar el servicio")
        void matricularDevuelve403() throws Exception {
            MvcResult resultado = mvc.perform(post(URL)
                            .header("Authorization", "Bearer estudiante")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoMatricular(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.FORBIDDEN);
            verifyNoInteractions(matriculaService);
        }
    }

    @Nested
    @DisplayName("Sin identidad válida, 401; contrato de error sin fuga de detalle técnico")
    class SinIdentidadValida {

        private final MockMvc mvc = mockMvc();

        @Test
        @DisplayName("Sin token responde 401 con el contrato de error y sin cabecera WWW-Authenticate")
        void sinTokenDevuelve401() throws Exception {
            MvcResult resultado = mvc.perform(get(URL)
                            .param("asignaturaId", UUID.randomUUID().toString())
                            .param("semestreId", UUID.randomUUID().toString()))
                    .andExpect(header().doesNotExist("WWW-Authenticate"))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.UNAUTHORIZED);
            verifyNoInteractions(matriculaService);
        }

        @Test
        @DisplayName("Un token inválido también responde 401 y el servicio no se invoca")
        void tokenInvalidoDevuelve401() throws Exception {
            mvc.perform(delete(URL + "/" + UUID.randomUUID())
                            .header("Authorization", "Bearer no-es-un-token"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(matriculaService);
        }
    }

    @Nested
    @DisplayName("Con un rol permitido, el control deja pasar la petición")
    class CaminoFelizConRolPermitido {

        private final MockMvc mvc = mockMvc();

        @Test
        @DisplayName("Un ADMINISTRADOR consulta matriculados y el control lo deja pasar hasta el servicio")
        void administradorConsultaMatriculados() throws Exception {
            UUID asignaturaId = UUID.randomUUID();
            UUID semestreId = UUID.randomUUID();
            when(matriculaService.listarMatriculados(asignaturaId, semestreId, false)).thenReturn(List.of());

            mvc.perform(get(URL)
                            .header("Authorization", "Bearer admin")
                            .param("asignaturaId", asignaturaId.toString())
                            .param("semestreId", semestreId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(matriculaService).listarMatriculados(asignaturaId, semestreId, false);
        }

        @Test
        @DisplayName("Un PROFESOR desvincula una matrícula y el control lo deja pasar hasta el servicio")
        void profesorDesvincula() throws Exception {
            UUID idMatricula = UUID.randomUUID();

            mvc.perform(delete(URL + "/" + idMatricula)
                            .header("Authorization", "Bearer profesor"))
                    .andExpect(status().isNoContent());

            verify(matriculaService).desvincular(idMatricula, false);
        }
    }
}
