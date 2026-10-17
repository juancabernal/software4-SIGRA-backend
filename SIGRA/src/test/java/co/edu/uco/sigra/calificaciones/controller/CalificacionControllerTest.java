package co.edu.uco.sigra.calificaciones.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.calificaciones.dto.CalificacionModificarRequestDTO;
import co.edu.uco.sigra.calificaciones.dto.CalificacionRequestDTO;
import co.edu.uco.sigra.calificaciones.dto.CalificacionResponseDTO;
import co.edu.uco.sigra.calificaciones.dto.EvaluacionCalificableDTO;
import co.edu.uco.sigra.calificaciones.dto.FilaPlanillaDTO;
import co.edu.uco.sigra.calificaciones.dto.HistorialCalificacionDTO;
import co.edu.uco.sigra.calificaciones.dto.PlanillaCalificacionesDTO;
import co.edu.uco.sigra.calificaciones.exception.CalificacionDuplicadaException;
import co.edu.uco.sigra.calificaciones.exception.CalificacionExceptionHandler;
import co.edu.uco.sigra.calificaciones.exception.CalificacionNoEncontradaException;
import co.edu.uco.sigra.calificaciones.exception.CalificacionReglaNegocioException;
import co.edu.uco.sigra.calificaciones.service.CalificacionService;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.common.exception.ManejadorErroresTransversal;
import co.edu.uco.sigra.estudiantes.exception.MatriculaNoEncontradaException;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionNoEncontradaException;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Capa web de calificaciones en modo standalone (sin contexto Spring, base de datos ni seguridad): rutas,
 * códigos HTTP, validación de los DTO y formato de error, con el manejador del módulo y el global.
 * La identidad (CurrentUser) se simula; el control por rol real lo prueba CalificacionIntegracionIT.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de calificaciones")
class CalificacionControllerTest {

    private static final UUID ID_EVALUACION = UUID.randomUUID();
    private static final UUID ID_CALIFICACION = UUID.randomUUID();
    private static final UUID ID_MATRICULA = UUID.randomUUID();
    private static final UUID ID_ESTUDIANTE = UUID.randomUUID();

    private static final String URL_PLANILLA = "/api/v1/evaluaciones/" + ID_EVALUACION + "/calificaciones";
    private static final String URL_CALIFICACION = "/api/v1/calificaciones/" + ID_CALIFICACION;

    private static final String OBSERVACION = "Buen desarrollo del parcial";
    private static final String JSON_STATUS = "$.status";
    private static final String JSON_MENSAJE = "$.mensaje";
    private static final String JSON_DETALLES = "$.detalles";

    private static final UsuarioPrincipal PROFESOR = new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.PROFESOR);

    @Mock
    private CalificacionService service;

    @Mock
    private CurrentUser currentUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        // La identidad la resuelve CurrentUser desde el token; aquí se simula (lenient: los casos que fallan
        // en la validación del DTO no llegan a pedirla).
        lenient().when(currentUser.required()).thenReturn(PROFESOR);
        mockMvc = MockMvcBuilders.standaloneSetup(new CalificacionController(service, currentUser))
                .setControllerAdvice(new CalificacionExceptionHandler(), new ManejadorErroresTransversal(),
                        new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private static CalificacionResponseDTO respuesta(boolean fueraDeFecha) {
        return new CalificacionResponseDTO(ID_CALIFICACION, ID_EVALUACION, ID_MATRICULA, ID_ESTUDIANTE,
                "Ana Gómez", new BigDecimal("4.5"), OBSERVACION, LocalDateTime.of(2026, 10, 15, 10, 0), null,
                fueraDeFecha);
    }

    private static String registro(String matriculaId, String nota, String observacion) {
        return """
                {"matriculaId":%s,"nota":%s,"observacion":%s}
                """.formatted(matriculaId, nota, observacion);
    }

    private static String modificacion(String nota, String observacion) {
        return """
                {"nota":%s,"observacion":%s}
                """.formatted(nota, observacion);
    }

    private static String texto(String valor) {
        return "\"" + valor + "\"";
    }

    private static final String REGISTRO_VALIDO = registro(texto(ID_MATRICULA.toString()), "4.5", texto(OBSERVACION));

    // ------------------------------------------------------------------ Registrar

    @Nested
    @DisplayName("POST registrar (RF-12)")
    class Registrar {

        @Test
        @DisplayName("Con datos válidos devuelve 201 con fueraDeFecha y delega con la evaluación de la URL")
        void datosValidos() throws Exception {
            when(service.registrar(eq(ID_EVALUACION), any(CalificacionRequestDTO.class), eq(PROFESOR)))
                    .thenReturn(respuesta(true));

            mockMvc.perform(post(URL_PLANILLA).contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ID_CALIFICACION.toString()))
                    .andExpect(jsonPath("$.nota").value(4.5))
                    .andExpect(jsonPath("$.fueraDeFecha").value(true))
                    .andExpect(jsonPath("$.estudianteNombre").value("Ana Gómez"));

            ArgumentCaptor<CalificacionRequestDTO> dto = ArgumentCaptor.forClass(CalificacionRequestDTO.class);
            verify(service).registrar(eq(ID_EVALUACION), dto.capture(), eq(PROFESOR));
            assertThat(dto.getValue().matriculaId()).isEqualTo(ID_MATRICULA);
            assertThat(dto.getValue().nota()).isEqualByComparingTo("4.5");
        }

        @Test
        @DisplayName("Recorta la observación antes de llegar al servicio")
        void recortaObservacion() throws Exception {
            when(service.registrar(eq(ID_EVALUACION), any(CalificacionRequestDTO.class), eq(PROFESOR)))
                    .thenReturn(respuesta(false));

            mockMvc.perform(post(URL_PLANILLA).contentType(MediaType.APPLICATION_JSON)
                            .content(registro(texto(ID_MATRICULA.toString()), "4.5", texto("   " + OBSERVACION + "  "))))
                    .andExpect(status().isCreated());

            ArgumentCaptor<CalificacionRequestDTO> dto = ArgumentCaptor.forClass(CalificacionRequestDTO.class);
            verify(service).registrar(eq(ID_EVALUACION), dto.capture(), eq(PROFESOR));
            assertThat(dto.getValue().observacion()).isEqualTo(OBSERVACION);
        }

        @Test
        @DisplayName("Pasa al servicio la identidad del token (CurrentUser)")
        void pasaLaIdentidad() throws Exception {
            when(service.registrar(eq(ID_EVALUACION), any(CalificacionRequestDTO.class), eq(PROFESOR)))
                    .thenReturn(respuesta(false));

            mockMvc.perform(post(URL_PLANILLA).contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                    .andExpect(status().isCreated());

            verify(currentUser).required();
        }

        @Test
        @DisplayName("Los extremos 0.0 y 5.0 son válidos")
        void extremosValidos() throws Exception {
            when(service.registrar(eq(ID_EVALUACION), any(CalificacionRequestDTO.class), eq(PROFESOR)))
                    .thenReturn(respuesta(false));

            mockMvc.perform(post(URL_PLANILLA).contentType(MediaType.APPLICATION_JSON)
                            .content(registro(texto(ID_MATRICULA.toString()), "0.0", texto(OBSERVACION))))
                    .andExpect(status().isCreated());
            mockMvc.perform(post(URL_PLANILLA).contentType(MediaType.APPLICATION_JSON)
                            .content(registro(texto(ID_MATRICULA.toString()), "5.0", texto(OBSERVACION))))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Nota mayor que 5.0: 400")
        void notaMayor() throws Exception {
            rechaza(registro(texto(ID_MATRICULA.toString()), "5.1", texto(OBSERVACION)), "nota");
        }

        @Test
        @DisplayName("Nota negativa: 400")
        void notaNegativa() throws Exception {
            rechaza(registro(texto(ID_MATRICULA.toString()), "-0.1", texto(OBSERVACION)), "nota");
        }

        @Test
        @DisplayName("Nota con dos decimales: 400")
        void notaDosDecimales() throws Exception {
            rechaza(registro(texto(ID_MATRICULA.toString()), "4.25", texto(OBSERVACION)), "nota");
        }

        @Test
        @DisplayName("Sin nota: 400")
        void sinNota() throws Exception {
            rechaza(registro(texto(ID_MATRICULA.toString()), "null", texto(OBSERVACION)), "nota");
        }

        @Test
        @DisplayName("Observación de 9 caracteres: 400")
        void observacionCorta() throws Exception {
            rechaza(registro(texto(ID_MATRICULA.toString()), "4.0", texto("123456789")), "observacion");
        }

        @Test
        @DisplayName("Observación de 10 caracteres con espacios alrededor no alcanza el mínimo si se recortan")
        void observacionConRelleno() throws Exception {
            rechaza(registro(texto(ID_MATRICULA.toString()), "4.0", texto("     corta     ")), "observacion");
        }

        @Test
        @DisplayName("Observación vacía: 400")
        void observacionVacia() throws Exception {
            rechaza(registro(texto(ID_MATRICULA.toString()), "4.0", texto("")), "observacion");
        }

        @Test
        @DisplayName("Observación con etiquetas HTML: 400")
        void observacionConEtiquetas() throws Exception {
            rechaza(registro(texto(ID_MATRICULA.toString()), "4.0", texto("<b>Muy buen trabajo</b>")), "observacion");
        }

        @Test
        @DisplayName("Observación de más de 500 caracteres: 400")
        void observacionLarga() throws Exception {
            rechaza(registro(texto(ID_MATRICULA.toString()), "4.0", texto("x".repeat(501))), "observacion");
        }

        @Test
        @DisplayName("Sin matrícula: 400")
        void sinMatricula() throws Exception {
            rechaza(registro("null", "4.0", texto(OBSERVACION)), "matriculaId");
        }

        @Test
        @DisplayName("Duplicada: 409")
        void duplicada() throws Exception {
            when(service.registrar(eq(ID_EVALUACION), any(CalificacionRequestDTO.class), eq(PROFESOR)))
                    .thenThrow(new CalificacionDuplicadaException());

            mockMvc.perform(post(URL_PLANILLA).contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath(JSON_MENSAJE).value(CalificacionDuplicadaException.MENSAJE));
        }

        @Test
        @DisplayName("Carrera contra el índice único: 409 con el mismo mensaje, sin detalles técnicos")
        void carrera() throws Exception {
            when(service.registrar(eq(ID_EVALUACION), any(CalificacionRequestDTO.class), eq(PROFESOR)))
                    .thenThrow(new DataIntegrityViolationException("uk_calificacion_evaluacion_matricula"));

            mockMvc.perform(post(URL_PLANILLA).contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath(JSON_MENSAJE).value(CalificacionDuplicadaException.MENSAJE));
        }

        @Test
        @DisplayName("Matrícula no activa: 400 con el mensaje de la regla")
        void reglaNegocio() throws Exception {
            when(service.registrar(eq(ID_EVALUACION), any(CalificacionRequestDTO.class), eq(PROFESOR)))
                    .thenThrow(CalificacionReglaNegocioException.matriculaNoActiva());

            mockMvc.perform(post(URL_PLANILLA).contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE, startsWith("La matrícula del estudiante no está activa")));
        }

        @Test
        @DisplayName("Evaluación o matrícula inexistentes: 404")
        void noEncontradas() throws Exception {
            when(service.registrar(eq(ID_EVALUACION), any(CalificacionRequestDTO.class), eq(PROFESOR)))
                    .thenThrow(new EvaluacionNoEncontradaException(ID_EVALUACION))
                    .thenThrow(MatriculaNoEncontradaException.porId(ID_MATRICULA));

            mockMvc.perform(post(URL_PLANILLA).contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                    .andExpect(status().isNotFound());
            mockMvc.perform(post(URL_PLANILLA).contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Asignatura que no está a cargo del profesor: 403 del manejador global")
        void sinAlcance() throws Exception {
            when(service.registrar(eq(ID_EVALUACION), any(CalificacionRequestDTO.class), eq(PROFESOR)))
                    .thenThrow(new AccessDeniedException("La asignatura de la evaluación no está asignada a este profesor"));

            mockMvc.perform(post(URL_PLANILLA).contentType(MediaType.APPLICATION_JSON).content(REGISTRO_VALIDO))
                    .andExpect(status().isForbidden());
        }

        private void rechaza(String cuerpo, String campo) throws Exception {
            mockMvc.perform(post(URL_PLANILLA).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_STATUS).value(400))
                    .andExpect(jsonPath(JSON_DETALLES, hasItem(startsWith(campo))));
            verifyNoInteractions(service);
        }
    }

    // ------------------------------------------------------------------ Modificar

    @Nested
    @DisplayName("PUT modificar (RF-13)")
    class Modificar {

        @Test
        @DisplayName("Con datos válidos devuelve 200 y delega con el id de la URL")
        void datosValidos() throws Exception {
            when(service.modificar(eq(ID_CALIFICACION), any(CalificacionModificarRequestDTO.class), eq(PROFESOR)))
                    .thenReturn(respuesta(false));

            mockMvc.perform(put(URL_CALIFICACION).contentType(MediaType.APPLICATION_JSON)
                            .content(modificacion("4.5", texto(OBSERVACION))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID_CALIFICACION.toString()));
        }

        @Test
        @DisplayName("Nota fuera de rango: 400 sin llamar al servicio")
        void notaInvalida() throws Exception {
            mockMvc.perform(put(URL_CALIFICACION).contentType(MediaType.APPLICATION_JSON)
                            .content(modificacion("7", texto(OBSERVACION))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_DETALLES, hasItem(startsWith("nota"))));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Calificación inexistente: 404")
        void inexistente() throws Exception {
            when(service.modificar(eq(ID_CALIFICACION), any(CalificacionModificarRequestDTO.class), eq(PROFESOR)))
                    .thenThrow(new CalificacionNoEncontradaException(ID_CALIFICACION));

            mockMvc.perform(put(URL_CALIFICACION).contentType(MediaType.APPLICATION_JSON)
                            .content(modificacion("4.5", texto(OBSERVACION))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Id con formato inválido: 400")
        void idInvalido() throws Exception {
            mockMvc.perform(put("/api/v1/calificaciones/abc").contentType(MediaType.APPLICATION_JSON)
                            .content(modificacion("4.5", texto(OBSERVACION))))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }
    }

    // ------------------------------------------------------------------ Consultas

    @Nested
    @DisplayName("GET evaluaciones, planilla e historial")
    class Consultas {

        @Test
        @DisplayName("Evaluaciones del RA en el semestre: 200")
        void evaluaciones() throws Exception {
            UUID raId = UUID.randomUUID();
            UUID semestreId = UUID.randomUUID();
            when(service.listarEvaluaciones(raId, semestreId)).thenReturn(List.of(new EvaluacionCalificableDTO(
                    ID_EVALUACION, "Parcial 1", LocalDate.of(2026, 10, 20), new BigDecimal("30"), "ACTIVA")));

            mockMvc.perform(get("/api/v1/calificaciones/evaluaciones")
                            .param("resultadoAprendizajeId", raId.toString())
                            .param("semestreId", semestreId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].nombre").value("Parcial 1"));
        }

        @Test
        @DisplayName("Evaluaciones sin semestre: 400")
        void evaluacionesSinSemestre() throws Exception {
            mockMvc.perform(get("/api/v1/calificaciones/evaluaciones")
                            .param("resultadoAprendizajeId", UUID.randomUUID().toString()))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Planilla: 200 con filas y totales")
        void planilla() throws Exception {
            FilaPlanillaDTO fila = new FilaPlanillaDTO(ID_MATRICULA, ID_ESTUDIANTE, "1001001001", "Ana Gómez",
                    true, null, null, null, null, null, null);
            when(service.consultarPlanilla(eq(ID_EVALUACION), eq(PROFESOR))).thenReturn(new PlanillaCalificacionesDTO(
                    ID_EVALUACION, "Parcial 1", LocalDate.of(2026, 10, 20), new BigDecimal("30"), "ACTIVA",
                    UUID.randomUUID(), "2026-2", UUID.randomUUID(), "ASG-101", "Software", "RA1",
                    1, 0, List.of(fila)));

            mockMvc.perform(get(URL_PLANILLA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.evaluacionNombre").value("Parcial 1"))
                    .andExpect(jsonPath("$.totalEstudiantes").value(1))
                    .andExpect(jsonPath("$.filas[0].nombreCompleto").value("Ana Gómez"))
                    .andExpect(jsonPath("$.filas[0].calificacionId").doesNotExist());
        }

        @Test
        @DisplayName("Historial: 200 con los eventos")
        void historial() throws Exception {
            when(service.consultarHistorial(eq(ID_CALIFICACION), eq(PROFESOR))).thenReturn(List.of(
                    new HistorialCalificacionDTO(UUID.randomUUID(), "REGISTRO", null, new BigDecimal("4.0"), null,
                            OBSERVACION, false, PROFESOR.id(), "PROFESOR", LocalDateTime.of(2026, 10, 15, 10, 0))));

            mockMvc.perform(get(URL_CALIFICACION + "/historial"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].tipoEvento").value("REGISTRO"))
                    .andExpect(jsonPath("$[0].notaNueva").value(4.0));
        }

        @Test
        @DisplayName("Planilla de una evaluación inexistente: 404")
        void planillaInexistente() throws Exception {
            when(service.consultarPlanilla(eq(ID_EVALUACION), eq(PROFESOR)))
                    .thenThrow(new EvaluacionNoEncontradaException(ID_EVALUACION));

            mockMvc.perform(get(URL_PLANILLA)).andExpect(status().isNotFound());
        }
    }
}