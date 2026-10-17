package co.edu.uco.sigra.mejoras.controller;

import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.exception.ConclusionNoEncontradaException;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionReglaNegocioException;
import co.edu.uco.sigra.evaluaciones.exception.NombreEvaluacionDuplicadoException;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraResponseDTO;
import co.edu.uco.sigra.mejoras.exception.AccionMejoraNoEncontradaException;
import co.edu.uco.sigra.mejoras.exception.AccionNoConvertibleException;
import co.edu.uco.sigra.mejoras.exception.AccionYaConvertidaException;
import co.edu.uco.sigra.mejoras.exception.ConclusionNoVigenteException;
import co.edu.uco.sigra.mejoras.exception.MejorasExceptionHandler;
import co.edu.uco.sigra.mejoras.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.mejoras.service.AccionMejoraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeMethodInterceptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa web en modo standalone (sin contexto Spring, base de datos ni seguridad),
 * igual que ConclusionControllerTest: rutas, códigos HTTP y camino feliz de RF-17/RF-18.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de acciones de mejora (RF-17/RF-18)")
class AccionMejoraControllerTest {

    private static final UUID ID_RA = UUID.randomUUID();
    private static final UUID ID_ASIGNATURA = UUID.randomUUID();
    private static final UUID ID_CONCLUSION = UUID.randomUUID();
    private static final UUID ID_ACCION = UUID.randomUUID();
    private static final UUID ID_EVALUACION = UUID.randomUUID();
    private static final UUID ID_SEMESTRE = UUID.randomUUID();
    private static final UsuarioPrincipal PRINCIPAL =
            new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.PROFESOR);

    private static final String URL_REGISTRAR = "/api/v1/conclusiones/" + ID_CONCLUSION + "/acciones-mejora";
    private static final String URL_CONVERTIR = "/api/v1/acciones-mejora/" + ID_ACCION + "/conversion";

    private static final String CUERPO_REGISTRO_VALIDO = """
            {"nombre":"Capacitación","descripcion":"Taller de refuerzo"}
            """;
    private static final String CUERPO_CONVERTIR_VALIDO = """
            {"fechaTentativa":"2026-10-20","porcentaje":30.0}
            """;

    @Mock
    private AccionMejoraService service;
    @Mock
    private CurrentUser currentUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(currentUser.required()).thenReturn(PRINCIPAL);
        mockMvc = MockMvcBuilders.standaloneSetup(new AccionMejoraController(service, currentUser))
                .setControllerAdvice(new MejorasExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    private static AccionMejoraResponseDTO respuesta(boolean convertida, UUID evaluacionGeneradaId) {
        return new AccionMejoraResponseDTO(ID_ACCION, "Capacitación", "Taller de refuerzo", LocalDateTime.now(),
                ID_RA, "RA-01", ID_ASIGNATURA, ID_CONCLUSION, ID_SEMESTRE, "2025-2", evaluacionGeneradaId,
                convertida);
    }

    @Nested
    @DisplayName("POST registrar (RF-17)")
    class Registrar {

        @Test
        @DisplayName("Con datos válidos devuelve 200 con la acción creada")
        void datosValidos() throws Exception {
            when(service.registrarAccionMejora(eq(ID_CONCLUSION), any(), any()))
                    .thenReturn(respuesta(false, null));

            mockMvc.perform(post(URL_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_REGISTRO_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value("Capacitación"))
                    .andExpect(jsonPath("$.convertida").value(false));
        }

        @Test
        @DisplayName("Sobre una conclusión no vigente devuelve 400")
        void conclusionNoVigente() throws Exception {
            when(service.registrarAccionMejora(eq(ID_CONCLUSION), any(), any()))
                    .thenThrow(new ConclusionNoVigenteException(ID_CONCLUSION));

            mockMvc.perform(post(URL_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_REGISTRO_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("Sobre una conclusión inexistente devuelve 404")
        void conclusionInexistente() throws Exception {
            when(service.registrarAccionMejora(eq(ID_CONCLUSION), any(), any()))
                    .thenThrow(new ConclusionNoEncontradaException(ID_CONCLUSION));

            mockMvc.perform(post(URL_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_REGISTRO_VALIDO))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Con un nombre de 31 caracteres devuelve 400 sin llamar al servicio")
        void nombreDe31CaracteresDevuelve400() throws Exception {
            String nombre31 = "A".repeat(31);

            mockMvc.perform(post(URL_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\":\"" + nombre31 + "\",\"descripcion\":\"Taller de refuerzo\"}"))
                    .andExpect(status().isBadRequest());
            org.mockito.Mockito.verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con rol ADMINISTRADOR devuelve 403 (RF-17/RBAC: solo el Profesor registra)")
        void administradorNoPuedeRegistrar() throws Exception {
            UsuarioPrincipal admin = new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ADMINISTRADOR);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    admin, null, List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))));
            try {
                ProxyFactory fabrica = new ProxyFactory(new AccionMejoraController(service, currentUser));
                fabrica.setProxyTargetClass(true);
                fabrica.addAdvice(AuthorizationManagerBeforeMethodInterceptor.preAuthorize());
                AccionMejoraController conSeguridad = (AccionMejoraController) fabrica.getProxy();
                MockMvc mockMvcSeguro = MockMvcBuilders.standaloneSetup(conSeguridad)
                        .setControllerAdvice(new MejorasExceptionHandler(), new GlobalExceptionHandler())
                        .build();

                mockMvcSeguro.perform(post(URL_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                                .content(CUERPO_REGISTRO_VALIDO))
                        .andExpect(status().isForbidden());
                verifyNoInteractions(service);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    @Nested
    @DisplayName("POST convertir en evaluación (RF-18)")
    class Convertir {

        @Test
        @DisplayName("Con datos válidos devuelve 200 con la acción convertida")
        void datosValidos() throws Exception {
            when(service.convertirEnEvaluacion(eq(ID_ACCION), any(), any()))
                    .thenReturn(respuesta(true, ID_EVALUACION));

            mockMvc.perform(post(URL_CONVERTIR).contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_CONVERTIR_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.convertida").value(true))
                    .andExpect(jsonPath("$.evaluacionGeneradaId").value(ID_EVALUACION.toString()));
        }

        @Test
        @DisplayName("Sobre una acción inexistente devuelve 404")
        void accionInexistente() throws Exception {
            when(service.convertirEnEvaluacion(eq(ID_ACCION), any(), any()))
                    .thenThrow(new AccionMejoraNoEncontradaException(ID_ACCION));

            mockMvc.perform(post(URL_CONVERTIR).contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_CONVERTIR_VALIDO))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Sobre una acción ya convertida devuelve 409")
        void accionYaConvertida() throws Exception {
            when(service.convertirEnEvaluacion(eq(ID_ACCION), any(), any()))
                    .thenThrow(new AccionYaConvertidaException(ID_ACCION));

            mockMvc.perform(post(URL_CONVERTIR).contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_CONVERTIR_VALIDO))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("CONFLICT"));
        }

        @Test
        @DisplayName("Si el servicio de evaluaciones rechaza el porcentaje (más del 100%) devuelve 400 con el disponible")
        void porcentajeExcedeLimite() throws Exception {
            when(service.convertirEnEvaluacion(eq(ID_ACCION), any(), any()))
                    .thenThrow(EvaluacionReglaNegocioException.porcentajeExcedido(new BigDecimal("20")));

            mockMvc.perform(post(URL_CONVERTIR).contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_CONVERTIR_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString(
                            "Porcentaje disponible: 20%")));
        }

        @Test
        @DisplayName("Si el nombre o la descripción heredados no cumplen RF-11 devuelve 400")
        void accionNoConvertible() throws Exception {
            when(service.convertirEnEvaluacion(eq(ID_ACCION), any(), any()))
                    .thenThrow(new AccionNoConvertibleException("El nombre debe tener máximo 30 caracteres"));

            mockMvc.perform(post(URL_CONVERTIR).contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_CONVERTIR_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }

        @Test
        @DisplayName("Si ya existe una evaluación con ese nombre en el RA y semestre devuelve 409")
        void nombreEvaluacionDuplicado() throws Exception {
            when(service.convertirEnEvaluacion(eq(ID_ACCION), any(), any()))
                    .thenThrow(new NombreEvaluacionDuplicadoException());

            mockMvc.perform(post(URL_CONVERTIR).contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_CONVERTIR_VALIDO))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("CONFLICT"));
        }

        @Test
        @DisplayName("Si el servicio de evaluaciones niega el permiso (profesor sin asignación) devuelve 403")
        void sinPermisoDesdeEvaluaciones() throws Exception {
            when(service.convertirEnEvaluacion(eq(ID_ACCION), any(), any()))
                    .thenThrow(new co.edu.uco.sigra.asignaturas.exception.PermisoInsuficienteException());

            mockMvc.perform(post(URL_CONVERTIR).contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_CONVERTIR_VALIDO))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("Sin fecha tentativa devuelve 400 sin llamar al servicio")
        void sinFechaTentativa() throws Exception {
            mockMvc.perform(post(URL_CONVERTIR).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"porcentaje\":30.0}"))
                    .andExpect(status().isBadRequest());
            org.mockito.Mockito.verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Sin permiso sobre la asignatura devuelve 403")
        void sinPermiso() throws Exception {
            when(service.convertirEnEvaluacion(eq(ID_ACCION), any(), any()))
                    .thenThrow(new PermisoInsuficienteException());

            mockMvc.perform(post(URL_CONVERTIR).contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_CONVERTIR_VALIDO))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }

        @Test
        @DisplayName("Con un porcentaje no numérico devuelve 400 sin llamar al servicio")
        void porcentajeInvalido() throws Exception {
            mockMvc.perform(post(URL_CONVERTIR).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"fechaTentativa\":\"2026-10-20\",\"porcentaje\":\"abc\"}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
