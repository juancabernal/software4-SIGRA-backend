package co.edu.uco.sigra.resultadosaprendizaje.controller;

import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeActualizarRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeCrearRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.exception.CodigoResultadoAprendizajeDuplicadoException;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeExceptionHandler;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeNoEncontradoException;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeReglaNegocioException;
import co.edu.uco.sigra.resultadosaprendizaje.service.ResultadoAprendizajeService;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de la capa web en modo standalone: no levanta contexto Spring, base de datos ni seguridad.
 * Verifica rutas, códigos HTTP, validaciones de los DTO y el formato de error, usando el manejador
 * del módulo (negocio) y el global (validaciones y errores genéricos), como en ejecución real.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de resultados de aprendizaje")
class ResultadoAprendizajeControllerTest {

    private static final UUID ID_ASIGNATURA = UUID.randomUUID();
    private static final UUID ID_RA = UUID.randomUUID();
    private static final String CODIGO = "RA-01";
    private static final String DESCRIPCION = "Analiza los requisitos de un sistema";

    private static final String URL_POR_ASIGNATURA = "/api/v1/asignaturas/" + ID_ASIGNATURA + "/resultados-aprendizaje";
    private static final String URL_RA = "/api/v1/resultados-aprendizaje";
    private static final String URL_RA_ID = URL_RA + "/" + ID_RA;

    private static final String JSON_MENSAJE = "$.mensaje";
    private static final String JSON_STATUS = "$.status";
    private static final String JSON_ESTADO = "$.estado";

    private static final String CUERPO_CREACION_VALIDO = """
            {"codigo":"RA-01","descripcion":"Analiza los requisitos de un sistema"}
            """;
    private static final String CUERPO_ACTUALIZACION_VALIDO = """
            {"descripcion":"Nueva descripción"}
            """;

    @Mock
    private ResultadoAprendizajeService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ResultadoAprendizajeController(service))
                .setControllerAdvice(new ResultadoAprendizajeExceptionHandler(), new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private static ResultadoAprendizajeResponseDTO respuesta(EstadoRegistro estado) {
        return new ResultadoAprendizajeResponseDTO(ID_RA, ID_ASIGNATURA, CODIGO, DESCRIPCION, estado.name());
    }

    private static String cuerpoCreacion(String codigo, String descripcion) {
        return """
                {"codigo":"%s","descripcion":"%s"}
                """.formatted(codigo, descripcion);
    }

    // ------------------------------------------------------------------ RF-06a

    @Nested
    @DisplayName("POST registrar (RF-06a)")
    class Registrar {

        @Test
        @DisplayName("Con datos válidos devuelve 201 y delega con la asignatura de la URL")
        void datosValidos() throws Exception {
            when(service.registrar(eq(ID_ASIGNATURA), any(ResultadoAprendizajeCrearRequestDTO.class)))
                    .thenReturn(respuesta(EstadoRegistro.ACTIVO));

            mockMvc.perform(post(URL_POR_ASIGNATURA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREACION_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.codigo").value(CODIGO))
                    .andExpect(jsonPath("$.asignaturaId").value(ID_ASIGNATURA.toString()))
                    .andExpect(jsonPath(JSON_ESTADO).value("ACTIVO"));

            ArgumentCaptor<ResultadoAprendizajeCrearRequestDTO> dto = ArgumentCaptor.forClass(ResultadoAprendizajeCrearRequestDTO.class);
            verify(service).registrar(eq(ID_ASIGNATURA), dto.capture());
            assertThat(dto.getValue().codigo()).isEqualTo(CODIGO);
            assertThat(dto.getValue().descripcion()).isEqualTo(DESCRIPCION);
        }

        @Test
        @DisplayName("Con espacios internos en el código devuelve 400 sin llamar al servicio")
        void codigoConEspaciosInternos() throws Exception {
            mockMvc.perform(post(URL_POR_ASIGNATURA).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCreacion("RA 01", DESCRIPCION)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_STATUS).value(400))
                    .andExpect(jsonPath("$.detalles[0]").value(startsWith("codigo")));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con un código de más de 10 caracteres devuelve 400 sin llamar al servicio")
        void codigoDemasiadoLargo() throws Exception {
            mockMvc.perform(post(URL_POR_ASIGNATURA).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCreacion("RA-00000001", DESCRIPCION)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles[0]").value(startsWith("codigo")));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con una descripción de más de 500 caracteres devuelve 400 sin llamar al servicio")
        void descripcionDemasiadoLarga() throws Exception {
            mockMvc.perform(post(URL_POR_ASIGNATURA).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCreacion(CODIGO, "x".repeat(501))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles[0]").value(startsWith("descripcion")));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con código y descripción vacíos devuelve 400 con el detalle de ambos campos")
        void camposVacios() throws Exception {
            mockMvc.perform(post(URL_POR_ASIGNATURA).contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCreacion("", "")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con un JSON mal formado devuelve 400 sin llamar al servicio")
        void jsonMalFormado() throws Exception {
            mockMvc.perform(post(URL_POR_ASIGNATURA).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"codigo\":\"RA-01\","))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).exists());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con un id de asignatura que no es UUID devuelve 400 sin llamar al servicio")
        void asignaturaIdInvalido() throws Exception {
            mockMvc.perform(post("/api/v1/asignaturas/abc/resultados-aprendizaje")
                            .contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREACION_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_STATUS).value(400));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con un código repetido devuelve 409")
        void codigoDuplicado() throws Exception {
            when(service.registrar(eq(ID_ASIGNATURA), any()))
                    .thenThrow(new CodigoResultadoAprendizajeDuplicadoException(CODIGO));

            mockMvc.perform(post(URL_POR_ASIGNATURA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREACION_VALIDO))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("CONFLICT"));
        }

        @Test
        @DisplayName("Si la base de datos rechaza un código duplicado (carrera) devuelve 409")
        void codigoDuplicadoPorCarrera() throws Exception {
            when(service.registrar(eq(ID_ASIGNATURA), any()))
                    .thenThrow(new DataIntegrityViolationException("uk_ra_asignatura_codigo"));

            mockMvc.perform(post(URL_POR_ASIGNATURA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREACION_VALIDO))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Con el máximo de RA activos alcanzado devuelve 400 con el mensaje de negocio")
        void maximoAlcanzado() throws Exception {
            ResultadoAprendizajeReglaNegocioException ex =
                    ResultadoAprendizajeReglaNegocioException.maximoActivosAlcanzado(7);
            when(service.registrar(eq(ID_ASIGNATURA), any())).thenThrow(ex);

            mockMvc.perform(post(URL_POR_ASIGNATURA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREACION_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value(ex.getMessage()));
        }

        @Test
        @DisplayName("Con una asignatura inexistente devuelve 404")
        void asignaturaInexistente() throws Exception {
            when(service.registrar(eq(ID_ASIGNATURA), any()))
                    .thenThrow(new AsignaturaNoEncontradaException(ID_ASIGNATURA));

            mockMvc.perform(post(URL_POR_ASIGNATURA).contentType(MediaType.APPLICATION_JSON).content(CUERPO_CREACION_VALIDO))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_STATUS).value(404));
        }
    }

    // ------------------------------------------------------------------ RF-06b

    @Nested
    @DisplayName("GET consultar (RF-06b)")
    class Consultar {

        @Test
        @DisplayName("Por asignatura sin filtro devuelve 200 con la lista")
        void porAsignatura() throws Exception {
            when(service.consultarPorAsignatura(ID_ASIGNATURA, null))
                    .thenReturn(List.of(respuesta(EstadoRegistro.ACTIVO), respuesta(EstadoRegistro.INACTIVO)));

            mockMvc.perform(get(URL_POR_ASIGNATURA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Por asignatura con ?estado=ACTIVO convierte el filtro al enum")
        void porAsignaturaConFiltro() throws Exception {
            when(service.consultarPorAsignatura(ID_ASIGNATURA, EstadoRegistro.ACTIVO))
                    .thenReturn(List.of(respuesta(EstadoRegistro.ACTIVO)));

            mockMvc.perform(get(URL_POR_ASIGNATURA).param("estado", "ACTIVO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].estado").value("ACTIVO"));
        }

        @Test
        @DisplayName("Por asignatura sin RA devuelve 200 con una lista vacía")
        void porAsignaturaVacia() throws Exception {
            when(service.consultarPorAsignatura(ID_ASIGNATURA, null)).thenReturn(List.of());

            mockMvc.perform(get(URL_POR_ASIGNATURA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Con un estado inválido devuelve 400 sin llamar al servicio")
        void estadoInvalido() throws Exception {
            mockMvc.perform(get(URL_POR_ASIGNATURA).param("estado", "OTRO"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Por asignatura inexistente devuelve 404")
        void porAsignaturaInexistente() throws Exception {
            when(service.consultarPorAsignatura(ID_ASIGNATURA, null))
                    .thenThrow(new AsignaturaNoEncontradaException(ID_ASIGNATURA));

            mockMvc.perform(get(URL_POR_ASIGNATURA))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Todos con filtro de estado devuelve 200")
        void todos() throws Exception {
            when(service.consultarTodos(EstadoRegistro.INACTIVO)).thenReturn(List.of(respuesta(EstadoRegistro.INACTIVO)));

            mockMvc.perform(get(URL_RA).param("estado", "INACTIVO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Por id existente devuelve 200")
        void porId() throws Exception {
            when(service.consultarPorId(ID_RA)).thenReturn(respuesta(EstadoRegistro.ACTIVO));

            mockMvc.perform(get(URL_RA_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID_RA.toString()));
        }

        @Test
        @DisplayName("Por id inexistente devuelve 404")
        void porIdInexistente() throws Exception {
            when(service.consultarPorId(ID_RA)).thenThrow(new ResultadoAprendizajeNoEncontradoException(ID_RA));

            mockMvc.perform(get(URL_RA_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_STATUS).value(404));
        }

        @Test
        @DisplayName("Por id que no es UUID devuelve 400 sin llamar al servicio")
        void porIdInvalido() throws Exception {
            mockMvc.perform(get(URL_RA + "/abc"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }
    }

    // ------------------------------------------------------------------ RF-06c

    @Nested
    @DisplayName("PUT actualizar descripción (RF-06c)")
    class ActualizarDescripcion {

        @Test
        @DisplayName("Con una descripción válida devuelve 200")
        void valida() throws Exception {
            when(service.actualizarDescripcion(eq(ID_RA), any(ResultadoAprendizajeActualizarRequestDTO.class)))
                    .thenReturn(respuesta(EstadoRegistro.ACTIVO));

            mockMvc.perform(put(URL_RA_ID).contentType(MediaType.APPLICATION_JSON).content(CUERPO_ACTUALIZACION_VALIDO))
                    .andExpect(status().isOk());

            ArgumentCaptor<ResultadoAprendizajeActualizarRequestDTO> dto =
                    ArgumentCaptor.forClass(ResultadoAprendizajeActualizarRequestDTO.class);
            verify(service).actualizarDescripcion(eq(ID_RA), dto.capture());
            assertThat(dto.getValue().descripcion()).isEqualTo("Nueva descripción");
        }

        @Test
        @DisplayName("Con una descripción vacía devuelve 400 sin llamar al servicio")
        void descripcionVacia() throws Exception {
            mockMvc.perform(put(URL_RA_ID).contentType(MediaType.APPLICATION_JSON).content("{\"descripcion\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles[0]").value(startsWith("descripcion")));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con un RA inactivo devuelve 400 con el mensaje de negocio")
        void raInactivo() throws Exception {
            ResultadoAprendizajeReglaNegocioException ex =
                    ResultadoAprendizajeReglaNegocioException.descripcionDeInactivo(CODIGO);
            when(service.actualizarDescripcion(eq(ID_RA), any())).thenThrow(ex);

            mockMvc.perform(put(URL_RA_ID).contentType(MediaType.APPLICATION_JSON).content(CUERPO_ACTUALIZACION_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value(ex.getMessage()));
        }
    }

    @Nested
    @DisplayName("PATCH inactivar y reactivar")
    class CambiarEstado {

        @Test
        @DisplayName("Inactivar devuelve 200 con el RA en INACTIVO")
        void inactivar() throws Exception {
            when(service.inactivar(ID_RA)).thenReturn(respuesta(EstadoRegistro.INACTIVO));

            mockMvc.perform(patch(URL_RA_ID + "/inactivar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_ESTADO).value("INACTIVO"));
        }

        @Test
        @DisplayName("Inactivar por debajo del mínimo devuelve 400 con el mensaje de negocio")
        void inactivarBajoElMinimo() throws Exception {
            ResultadoAprendizajeReglaNegocioException ex =
                    ResultadoAprendizajeReglaNegocioException.minimoActivosRequerido(5);
            when(service.inactivar(ID_RA)).thenThrow(ex);

            mockMvc.perform(patch(URL_RA_ID + "/inactivar"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value(ex.getMessage()));
        }

        @Test
        @DisplayName("Reactivar devuelve 200 con el RA en ACTIVO")
        void reactivar() throws Exception {
            when(service.reactivar(ID_RA)).thenReturn(respuesta(EstadoRegistro.ACTIVO));

            mockMvc.perform(patch(URL_RA_ID + "/reactivar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_ESTADO).value("ACTIVO"));
        }

        @Test
        @DisplayName("Reactivar en una asignatura inactiva devuelve 400")
        void reactivarAsignaturaInactiva() throws Exception {
            when(service.reactivar(ID_RA)).thenThrow(ResultadoAprendizajeReglaNegocioException.asignaturaInactiva());

            mockMvc.perform(patch(URL_RA_ID + "/reactivar"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Un error inesperado devuelve 500 con un mensaje genérico")
        void errorInesperado() throws Exception {
            when(service.reactivar(ID_RA)).thenThrow(new IllegalStateException("fallo interno"));

            mockMvc.perform(patch(URL_RA_ID + "/reactivar"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath(JSON_MENSAJE).value("Ocurrió un error inesperado en el servidor"));
        }
    }
}
