package co.edu.uco.sigra.evaluaciones.controller;

import co.edu.uco.sigra.asignaturas.exception.AutenticacionRequeridaException;
import co.edu.uco.sigra.asignaturas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionActualizarRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionCrearRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionResponseDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionesRaResumenDTO;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionExceptionHandler;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionNoEncontradaException;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionReglaNegocioException;
import co.edu.uco.sigra.evaluaciones.exception.NombreEvaluacionDuplicadoException;
import co.edu.uco.sigra.evaluaciones.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de la capa web en modo standalone: no levanta contexto Spring, base de datos ni seguridad.
 * Verifica rutas, códigos HTTP, validaciones de los DTO y el formato de error, usando el manejador
 * del módulo (negocio, 401 y 403) y el global (validaciones y errores genéricos), como en ejecución real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de evaluaciones")
class EvaluacionControllerTest {

    private static final UUID ID_RA = UUID.randomUUID();
    private static final UUID ID_EVALUACION = UUID.randomUUID();
    private static final UUID ID_SEMESTRE = UUID.randomUUID();
    private static final UsuarioPrincipal PRINCIPAL =
            new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.PROFESOR);

    private static final String URL_POR_RA = "/api/v1/resultados-aprendizaje/" + ID_RA + "/evaluaciones";
    private static final String URL_EVALUACION = "/api/v1/evaluaciones/" + ID_EVALUACION;

    private static final String NOMBRE = "Parcial 1";
    private static final String DESCRIPCION = "Primer parcial del corte";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 10);

    private static final String JSON_MENSAJE = "$.mensaje";
    private static final String JSON_STATUS = "$.status";
    private static final String JSON_DETALLES = "$.detalles";

    private static final String CUERPO_VALIDO = cuerpo("Parcial 1", "\"2026-11-10\"", "30", "Primer parcial del corte");

    @Mock
    private EvaluacionService service;
    @Mock
    private CurrentUser currentUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        lenient().when(currentUser.required()).thenReturn(PRINCIPAL);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new EvaluacionController(service, currentUser))
                .setControllerAdvice(new EvaluacionExceptionHandler(), new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private static EvaluacionResponseDTO respuesta(String estado) {
        return new EvaluacionResponseDTO(ID_EVALUACION, ID_RA, ID_SEMESTRE, "2026-2", NOMBRE, FECHA,
                new BigDecimal("30"), DESCRIPCION, estado, false);
    }

    /** La fecha se pasa ya con comillas (o {@code null}) para poder probar fechas mal formadas. */
    private static String cuerpo(String nombre, String fecha, String porcentaje, String descripcion) {
        return """
                {"nombre":"%s","fechaTentativa":%s,"porcentaje":%s,"descripcion":"%s"}
                """.formatted(nombre, fecha, porcentaje, descripcion);
    }

    // ------------------------------------------------------------------ RF-10a

    @Nested
    @DisplayName("POST crear (RF-10a)")
    class Crear {

        @Test
        @DisplayName("Con datos válidos devuelve 201 y delega con el RA de la URL")
        void datosValidos() throws Exception {
            when(service.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenReturn(respuesta("ACTIVA"));

            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ID_EVALUACION.toString()))
                    .andExpect(jsonPath("$.resultadoAprendizajeId").value(ID_RA.toString()))
                    .andExpect(jsonPath("$.nombre").value(NOMBRE))
                    .andExpect(jsonPath("$.fechaTentativa").value("2026-11-10"))
                    .andExpect(jsonPath("$.porcentaje").value(30))
                    .andExpect(jsonPath("$.estado").value("ACTIVA"))
                    .andExpect(jsonPath("$.tieneCalificaciones").value(false));

            ArgumentCaptor<EvaluacionCrearRequestDTO> dto = ArgumentCaptor.forClass(EvaluacionCrearRequestDTO.class);
            verify(service).crear(eq(ID_RA), dto.capture(), any(UsuarioPrincipal.class));
            assertThat(dto.getValue().nombre()).isEqualTo(NOMBRE);
            assertThat(dto.getValue().fechaTentativa()).isEqualTo(FECHA);
            assertThat(dto.getValue().porcentaje()).isEqualByComparingTo("30");
            assertThat(dto.getValue().descripcion()).isEqualTo(DESCRIPCION);
        }

        @Test
        @DisplayName("Normaliza espacios en nombre y descripción antes de llegar al servicio")
        void normalizaEspacios() throws Exception {
            when(service.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenReturn(respuesta("ACTIVA"));

            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo("  Parcial    1 ", "\"2026-11-10\"", "30", "  Primer   parcial ")))
                    .andExpect(status().isCreated());

            ArgumentCaptor<EvaluacionCrearRequestDTO> dto = ArgumentCaptor.forClass(EvaluacionCrearRequestDTO.class);
            verify(service).crear(eq(ID_RA), dto.capture(), any(UsuarioPrincipal.class));
            assertThat(dto.getValue().nombre()).isEqualTo("Parcial 1");
            assertThat(dto.getValue().descripcion()).isEqualTo("Primer parcial");
        }

        @Test
        @DisplayName("Pasa al servicio la identidad que dejó el interceptor de roles")
        void pasaLaIdentidad() throws Exception {
            UsuarioPrincipal identidad = new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.PROFESOR);
            when(service.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenReturn(respuesta("ACTIVA"));

            mockMvc.perform(post(URL_POR_RA)
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isCreated());

            verify(service).crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any(UsuarioPrincipal.class));
        }

        @Test
        @DisplayName("Sin nombre devuelve 400 sin llamar al servicio")
        void sinNombre() throws Exception {
            rechazaCreacion(cuerpo("", "\"2026-11-10\"", "30", DESCRIPCION), "nombre");
        }

        @Test
        @DisplayName("Con un nombre de más de 30 caracteres devuelve 400 sin llamar al servicio")
        void nombreDemasiadoLargo() throws Exception {
            rechazaCreacion(cuerpo("N".repeat(31), "\"2026-11-10\"", "30", DESCRIPCION), "nombre");
        }

        @Test
        @DisplayName("Con < o > en el nombre devuelve 400 sin llamar al servicio")
        void nombreConCaracteresProhibidos() throws Exception {
            rechazaCreacion(cuerpo("<b>Parcial</b>", "\"2026-11-10\"", "30", DESCRIPCION), "nombre");
        }

        @Test
        @DisplayName("Sin fecha tentativa devuelve 400 sin llamar al servicio")
        void sinFecha() throws Exception {
            rechazaCreacion(cuerpo(NOMBRE, "null", "30", DESCRIPCION), "fechaTentativa");
        }

        @Test
        @DisplayName("Con una fecha que no es ISO (dd/MM/yyyy) devuelve 400 sin llamar al servicio")
        void fechaMalFormada() throws Exception {
            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(NOMBRE, "\"13/10/2026\"", "30", DESCRIPCION)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_STATUS).value(400));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Sin porcentaje devuelve 400 sin llamar al servicio")
        void sinPorcentaje() throws Exception {
            rechazaCreacion(cuerpo(NOMBRE, "\"2026-11-10\"", "null", DESCRIPCION), "porcentaje");
        }

        @Test
        @DisplayName("Con porcentaje 0 devuelve 400 sin llamar al servicio")
        void porcentajeCero() throws Exception {
            rechazaCreacion(cuerpo(NOMBRE, "\"2026-11-10\"", "0", DESCRIPCION), "porcentaje");
        }

        @Test
        @DisplayName("Con porcentaje negativo devuelve 400 sin llamar al servicio")
        void porcentajeNegativo() throws Exception {
            rechazaCreacion(cuerpo(NOMBRE, "\"2026-11-10\"", "-5", DESCRIPCION), "porcentaje");
        }

        @Test
        @DisplayName("Con porcentaje mayor que 100 devuelve 400 sin llamar al servicio")
        void porcentajeMayorACien() throws Exception {
            rechazaCreacion(cuerpo(NOMBRE, "\"2026-11-10\"", "100.01", DESCRIPCION), "porcentaje");
        }

        @Test
        @DisplayName("Con más de 2 decimales devuelve 400 sin llamar al servicio")
        void porcentajeConTresDecimales() throws Exception {
            rechazaCreacion(cuerpo(NOMBRE, "\"2026-11-10\"", "10.123", DESCRIPCION), "porcentaje");
        }

        @Test
        @DisplayName("Con porcentaje que no es número devuelve 400 sin llamar al servicio")
        void porcentajeNoNumerico() throws Exception {
            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(NOMBRE, "\"2026-11-10\"", "\"abc\"", DESCRIPCION)))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con porcentaje 100 y con 0.01 (límites) es válido")
        void porcentajesLimite() throws Exception {
            when(service.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenReturn(respuesta("ACTIVA"));

            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(NOMBRE, "\"2026-11-10\"", "100", DESCRIPCION)))
                    .andExpect(status().isCreated());
            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(NOMBRE, "\"2026-11-10\"", "0.01", DESCRIPCION)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Sin descripción devuelve 400 sin llamar al servicio")
        void sinDescripcion() throws Exception {
            rechazaCreacion(cuerpo(NOMBRE, "\"2026-11-10\"", "30", "   "), "descripcion");
        }

        @Test
        @DisplayName("Con una descripción de más de 100 caracteres devuelve 400 sin llamar al servicio")
        void descripcionDemasiadoLarga() throws Exception {
            rechazaCreacion(cuerpo(NOMBRE, "\"2026-11-10\"", "30", "D".repeat(101)), "descripcion");
        }

        @Test
        @DisplayName("Con < o > en la descripción devuelve 400 sin llamar al servicio")
        void descripcionConCaracteresProhibidos() throws Exception {
            rechazaCreacion(cuerpo(NOMBRE, "\"2026-11-10\"", "30", "<script>"), "descripcion");
        }

        @Test
        @DisplayName("Con varios campos obligatorios vacíos informa todos los errores")
        void variosErrores() throws Exception {
            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_DETALLES, hasItem(startsWith("nombre"))))
                    .andExpect(jsonPath(JSON_DETALLES, hasItem(startsWith("fechaTentativa"))))
                    .andExpect(jsonPath(JSON_DETALLES, hasItem(startsWith("porcentaje"))))
                    .andExpect(jsonPath(JSON_DETALLES, hasItem(startsWith("descripcion"))));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con JSON mal formado devuelve 400")
        void jsonMalFormado() throws Exception {
            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON).content("{nombre:"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con un id de RA que no es UUID devuelve 400 sin llamar al servicio")
        void idRaNoUuid() throws Exception {
            mockMvc.perform(post("/api/v1/resultados-aprendizaje/no-es-uuid/evaluaciones")
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Si el servicio rechaza por regla de negocio devuelve 400 con el mensaje")
        void reglaDeNegocio() throws Exception {
            when(service.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenThrow(EvaluacionReglaNegocioException.porcentajeExcedido(new BigDecimal("40")));

            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_STATUS).value(400))
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                    .andExpect(jsonPath(JSON_MENSAJE).value(
                            EvaluacionReglaNegocioException.porcentajeExcedido(new BigDecimal("40")).getMessage()));
        }

        @Test
        @DisplayName("Si el RA no existe devuelve 404")
        void raNoExiste() throws Exception {
            when(service.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenThrow(new ResultadoAprendizajeNoEncontradoException(ID_RA));

            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_STATUS).value(404));
        }

        @Test
        @DisplayName("Si el nombre está duplicado devuelve 409")
        void nombreDuplicado() throws Exception {
            when(service.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenThrow(new NombreEvaluacionDuplicadoException());

            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath(JSON_MENSAJE).value(NombreEvaluacionDuplicadoException.MENSAJE));
        }

        @Test
        @DisplayName("Si la base rechaza por integridad (carrera de nombres) devuelve 409")
        void violacionDeIntegridad() throws Exception {
            when(service.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenThrow(new DataIntegrityViolationException("uk_evaluacion_ra_semestre_nombre"));

            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath(JSON_MENSAJE).value(NombreEvaluacionDuplicadoException.MENSAJE));
        }

        @Test
        @DisplayName("Si el profesor no es dueño de la asignatura devuelve 403")
        void sinPermiso() throws Exception {
            when(service.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenThrow(new PermisoInsuficienteException());

            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath(JSON_STATUS).value(403));
        }

        @Test
        @DisplayName("Si falta autenticación devuelve 401 con WWW-Authenticate: Bearer")
        void sinAutenticacion() throws Exception {
            when(service.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenThrow(new AutenticacionRequeridaException());

            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"));
        }

        private void rechazaCreacion(String cuerpo, String campo) throws Exception {
            mockMvc.perform(post(URL_POR_RA).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_STATUS).value(400))
                    .andExpect(jsonPath(JSON_DETALLES, hasItem(startsWith(campo))));
            verifyNoInteractions(service);
        }
    }

    // ------------------------------------------------------------------ Consultas

    @Nested
    @DisplayName("GET consultar")
    class Consultar {

        private EvaluacionesRaResumenDTO resumen() {
            return new EvaluacionesRaResumenDTO(ID_RA, ID_SEMESTRE, "2026-2", new BigDecimal("30"),
                    new BigDecimal("70"), List.of(respuesta("ACTIVA")));
        }

        @Test
        @DisplayName("Por RA sin semestreId devuelve 200 con el % acumulado y delega semestre nulo")
        void porRaSinSemestre() throws Exception {
            when(service.consultarPorResultadoAprendizaje(eq(ID_RA), isNull(), any(UsuarioPrincipal.class))).thenReturn(resumen());

            mockMvc.perform(get(URL_POR_RA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultadoAprendizajeId").value(ID_RA.toString()))
                    .andExpect(jsonPath("$.semestreCodigo").value("2026-2"))
                    .andExpect(jsonPath("$.porcentajeAsignado").value(30))
                    .andExpect(jsonPath("$.porcentajeDisponible").value(70))
                    .andExpect(jsonPath("$.evaluaciones.length()").value(1))
                    .andExpect(jsonPath("$.evaluaciones[0].nombre").value(NOMBRE));
        }

        @Test
        @DisplayName("Por RA con semestreId lo pasa al servicio")
        void porRaConSemestre() throws Exception {
            when(service.consultarPorResultadoAprendizaje(eq(ID_RA), eq(ID_SEMESTRE), any(UsuarioPrincipal.class)))
                    .thenReturn(resumen());

            mockMvc.perform(get(URL_POR_RA).param("semestreId", ID_SEMESTRE.toString()))
                    .andExpect(status().isOk());

            verify(service).consultarPorResultadoAprendizaje(eq(ID_RA), eq(ID_SEMESTRE), any(UsuarioPrincipal.class));
        }

        @Test
        @DisplayName("Con un semestreId que no es UUID devuelve 400 sin llamar al servicio")
        void semestreIdNoUuid() throws Exception {
            mockMvc.perform(get(URL_POR_RA).param("semestreId", "xyz"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con un semestre inexistente devuelve 404")
        void semestreNoExiste() throws Exception {
            when(service.consultarPorResultadoAprendizaje(eq(ID_RA), eq(ID_SEMESTRE), any(UsuarioPrincipal.class)))
                    .thenThrow(new SemestreNoEncontradoException(ID_SEMESTRE));

            mockMvc.perform(get(URL_POR_RA).param("semestreId", ID_SEMESTRE.toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Sin semestre activo devuelve 400")
        void sinSemestreActivo() throws Exception {
            when(service.consultarPorResultadoAprendizaje(eq(ID_RA), isNull(), any(UsuarioPrincipal.class)))
                    .thenThrow(EvaluacionReglaNegocioException.sinSemestreActivo());

            mockMvc.perform(get(URL_POR_RA)).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Por id devuelve 200 con la evaluación")
        void porId() throws Exception {
            when(service.consultarPorId(eq(ID_EVALUACION), any(UsuarioPrincipal.class))).thenReturn(respuesta("ACTIVA"));

            mockMvc.perform(get(URL_EVALUACION))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID_EVALUACION.toString()))
                    .andExpect(jsonPath("$.semestreId").value(ID_SEMESTRE.toString()));
        }

        @Test
        @DisplayName("Por id inexistente devuelve 404")
        void porIdNoExiste() throws Exception {
            when(service.consultarPorId(eq(ID_EVALUACION), any(UsuarioPrincipal.class)))
                    .thenThrow(new EvaluacionNoEncontradaException(ID_EVALUACION));

            mockMvc.perform(get(URL_EVALUACION))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_STATUS).value(404));
        }

        @Test
        @DisplayName("Por id que no es UUID devuelve 400 sin llamar al servicio")
        void porIdNoUuid() throws Exception {
            mockMvc.perform(get("/api/v1/evaluaciones/abc")).andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Por id de una evaluación de otra asignatura devuelve 403")
        void porIdSinPermiso() throws Exception {
            when(service.consultarPorId(eq(ID_EVALUACION), any(UsuarioPrincipal.class))).thenThrow(new PermisoInsuficienteException());

            mockMvc.perform(get(URL_EVALUACION)).andExpect(status().isForbidden());
        }
    }

    // ------------------------------------------------------------------ RF-10b

    @Nested
    @DisplayName("PUT modificar (RF-10b)")
    class Modificar {

        @Test
        @DisplayName("Con datos válidos devuelve 200 y delega con el id de la URL")
        void datosValidos() throws Exception {
            when(service.modificar(eq(ID_EVALUACION), any(EvaluacionActualizarRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenReturn(respuesta("ACTIVA"));

            mockMvc.perform(put(URL_EVALUACION).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value(NOMBRE));

            ArgumentCaptor<EvaluacionActualizarRequestDTO> dto =
                    ArgumentCaptor.forClass(EvaluacionActualizarRequestDTO.class);
            verify(service).modificar(eq(ID_EVALUACION), dto.capture(), any(UsuarioPrincipal.class));
            assertThat(dto.getValue().fechaTentativa()).isEqualTo(FECHA);
            assertThat(dto.getValue().porcentaje()).isEqualByComparingTo("30");
        }

        @Test
        @DisplayName("Con campos inválidos devuelve 400 sin llamar al servicio")
        void camposInvalidos() throws Exception {
            mockMvc.perform(put(URL_EVALUACION).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_DETALLES, hasItem(startsWith("nombre"))))
                    .andExpect(jsonPath(JSON_DETALLES, hasItem(startsWith("fechaTentativa"))))
                    .andExpect(jsonPath(JSON_DETALLES, hasItem(startsWith("porcentaje"))))
                    .andExpect(jsonPath(JSON_DETALLES, hasItem(startsWith("descripcion"))));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con porcentaje mayor que 100 devuelve 400 sin llamar al servicio")
        void porcentajeMayorACien() throws Exception {
            mockMvc.perform(put(URL_EVALUACION).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo(NOMBRE, "\"2026-11-10\"", "101", DESCRIPCION)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_DETALLES, hasItem(startsWith("porcentaje"))));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con calificaciones registradas y fecha o porcentaje cambiados devuelve 400 con el mensaje")
        void bloqueadaPorCalificaciones() throws Exception {
            when(service.modificar(eq(ID_EVALUACION), any(EvaluacionActualizarRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenThrow(EvaluacionReglaNegocioException.camposBloqueadosPorCalificaciones());

            mockMvc.perform(put(URL_EVALUACION).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value(
                            EvaluacionReglaNegocioException.camposBloqueadosPorCalificaciones().getMessage()));
        }

        @Test
        @DisplayName("Si no existe devuelve 404")
        void noExiste() throws Exception {
            when(service.modificar(eq(ID_EVALUACION), any(EvaluacionActualizarRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenThrow(new EvaluacionNoEncontradaException(ID_EVALUACION));

            mockMvc.perform(put(URL_EVALUACION).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Si el nombre está duplicado devuelve 409")
        void nombreDuplicado() throws Exception {
            when(service.modificar(eq(ID_EVALUACION), any(EvaluacionActualizarRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenThrow(new NombreEvaluacionDuplicadoException());

            mockMvc.perform(put(URL_EVALUACION).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Sin permiso sobre la asignatura devuelve 403")
        void sinPermiso() throws Exception {
            when(service.modificar(eq(ID_EVALUACION), any(EvaluacionActualizarRequestDTO.class), any(UsuarioPrincipal.class)))
                    .thenThrow(new PermisoInsuficienteException());

            mockMvc.perform(put(URL_EVALUACION).contentType(MediaType.APPLICATION_JSON).content(CUERPO_VALIDO))
                    .andExpect(status().isForbidden());
        }
    }

    // ------------------------------------------------------------------ RF-10c

    @Nested
    @DisplayName("PATCH inactivar / reactivar (RF-10c)")
    class InactivarReactivar {

        @Test
        @DisplayName("Inactivar devuelve 200 con estado INACTIVA")
        void inactivar() throws Exception {
            when(service.inactivar(eq(ID_EVALUACION), any(UsuarioPrincipal.class))).thenReturn(respuesta("INACTIVA"));

            mockMvc.perform(patch(URL_EVALUACION + "/inactivar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("INACTIVA"));
        }

        @Test
        @DisplayName("Reactivar devuelve 200 con estado ACTIVA")
        void reactivar() throws Exception {
            when(service.reactivar(eq(ID_EVALUACION), any(UsuarioPrincipal.class))).thenReturn(respuesta("ACTIVA"));

            mockMvc.perform(patch(URL_EVALUACION + "/reactivar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("ACTIVA"));
        }

        @Test
        @DisplayName("Inactivar una evaluación ya inactiva devuelve 400")
        void yaInactiva() throws Exception {
            when(service.inactivar(eq(ID_EVALUACION), any(UsuarioPrincipal.class)))
                    .thenThrow(EvaluacionReglaNegocioException.yaInactiva());

            mockMvc.perform(patch(URL_EVALUACION + "/inactivar"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value(EvaluacionReglaNegocioException.yaInactiva().getMessage()));
        }

        @Test
        @DisplayName("Reactivar que excedería 100% devuelve 400")
        void reactivarExcede() throws Exception {
            when(service.reactivar(eq(ID_EVALUACION), any(UsuarioPrincipal.class)))
                    .thenThrow(EvaluacionReglaNegocioException.porcentajeExcedido(new BigDecimal("20")));

            mockMvc.perform(patch(URL_EVALUACION + "/reactivar")).andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Inactivar o reactivar una evaluación inexistente devuelve 404")
        void noExiste() throws Exception {
            when(service.inactivar(eq(ID_EVALUACION), any(UsuarioPrincipal.class)))
                    .thenThrow(new EvaluacionNoEncontradaException(ID_EVALUACION));
            when(service.reactivar(eq(ID_EVALUACION), any(UsuarioPrincipal.class)))
                    .thenThrow(new EvaluacionNoEncontradaException(ID_EVALUACION));

            mockMvc.perform(patch(URL_EVALUACION + "/inactivar")).andExpect(status().isNotFound());
            mockMvc.perform(patch(URL_EVALUACION + "/reactivar")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Con un id que no es UUID devuelve 400 sin llamar al servicio")
        void idNoUuid() throws Exception {
            mockMvc.perform(patch("/api/v1/evaluaciones/abc/inactivar")).andExpect(status().isBadRequest());
            mockMvc.perform(patch("/api/v1/evaluaciones/abc/reactivar")).andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Sin permiso sobre la asignatura devuelve 403")
        void sinPermiso() throws Exception {
            when(service.inactivar(eq(ID_EVALUACION), any(UsuarioPrincipal.class))).thenThrow(new PermisoInsuficienteException());

            mockMvc.perform(patch(URL_EVALUACION + "/inactivar")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Un método no soportado (DELETE) devuelve 405: las evaluaciones no se eliminan")
        void deleteNoSoportado() throws Exception {
            mockMvc.perform(delete(URL_EVALUACION)).andExpect(status().isMethodNotAllowed());
            verifyNoInteractions(service);
        }
    }
}
