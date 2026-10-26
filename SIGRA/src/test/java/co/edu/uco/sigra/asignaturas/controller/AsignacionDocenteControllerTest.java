package co.edu.uco.sigra.asignaturas.controller;

import co.edu.uco.sigra.asignaturas.dto.AsignacionDocenteResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignacionNoRealizadaDTO;
import co.edu.uco.sigra.asignaturas.dto.ResultadoAsignacionMasivaDTO;
import co.edu.uco.sigra.asignaturas.exception.AsignacionDocenteNoEncontradaException;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.service.AsignacionDocenteService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.exception.AsercionesContratoError;
import co.edu.uco.sigra.profesores.exception.ProfesorNoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de capa web de {@link AsignacionDocenteController}: deserialización, validación con
 * Jakarta Validation y traducción de excepciones de dominio a códigos HTTP. El servicio va
 * simulado: no hace falta base de datos. Los casos de contrato de error de peticiones mal formadas
 * (415, 406, 409 de integridad) ya los cubre {@code ContratoErrorAsignacionDocenteTest}; el RBAC
 * real (JWT, filtros y @PreAuthorize) ya lo cubre {@code AsignacionDocenteSecurityWebTest}.
 */
@WebMvcTest(controllers = AsignacionDocenteController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AsignacionDocenteController: deserialización, validación y códigos HTTP")
class AsignacionDocenteControllerTest {

    private static final String RUTA = "/api/v1/asignaciones-docentes";

    private static final UUID PROFESOR_ID = UUID.fromString("b3d4e5f6-7a8b-49c0-91d2-3e4f5a6b7c8d");
    private static final UUID ASIGNATURA_ID = UUID.fromString("c4e5f6a7-8b9c-4d01-a2e3-4f5a6b7c8d9e");

    private static final String CUERPO_VALIDO = """
            {
              "profesorIds": ["b3d4e5f6-7a8b-49c0-91d2-3e4f5a6b7c8d"],
              "asignaturaIds": ["c4e5f6a7-8b9c-4d01-a2e3-4f5a6b7c8d9e"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsignacionDocenteService service;

    private static ResultadoAsignacionMasivaDTO resultadoConUnaCreada() {
        AsignacionDocenteResponseDTO creada = new AsignacionDocenteResponseDTO(
                UUID.randomUUID(), PROFESOR_ID, "Ana Gómez", ASIGNATURA_ID, "MAT-101", "Cálculo",
                EstadoRegistro.ACTIVO, LocalDate.now());
        return new ResultadoAsignacionMasivaDTO(List.of(creada), List.of(), List.of());
    }

    // ------------------------------------------------------------------ POST

    @Nested
    @DisplayName("POST /api/v1/asignaciones-docentes")
    class Asignar {

        @Test
        @DisplayName("Cuerpo válido devuelve 201 con el resumen de la asignación masiva")
        void cuerpoValidoDevuelve201() throws Exception {
            given(service.asignarMasivo(any())).willReturn(resultadoConUnaCreada());

            mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.creadas.length()").value(1))
                    .andExpect(jsonPath("$.creadas[0].profesorId").value(PROFESOR_ID.toString()))
                    .andExpect(jsonPath("$.creadas[0].asignaturaId").value(ASIGNATURA_ID.toString()))
                    .andExpect(jsonPath("$.omitidas").isArray())
                    .andExpect(jsonPath("$.omitidas.length()").value(0))
                    .andExpect(jsonPath("$.rechazadas").isArray())
                    .andExpect(jsonPath("$.rechazadas.length()").value(0));
        }

        @Test
        @DisplayName("Cuerpo válido con omitidas y rechazadas las refleja en la respuesta")
        void cuerpoValidoConOmitidasYRechazadas() throws Exception {
            ResultadoAsignacionMasivaDTO resultado = new ResultadoAsignacionMasivaDTO(
                    List.of(),
                    List.of(new AsignacionNoRealizadaDTO(PROFESOR_ID, ASIGNATURA_ID,
                            "El profesor ya está asignado a esta asignatura")),
                    List.of(new AsignacionNoRealizadaDTO(PROFESOR_ID, ASIGNATURA_ID,
                            "El profesor está inactivo")));
            given(service.asignarMasivo(any())).willReturn(resultado);

            mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.creadas.length()").value(0))
                    .andExpect(jsonPath("$.omitidas.length()").value(1))
                    .andExpect(jsonPath("$.omitidas[0].motivo").value("El profesor ya está asignado a esta asignatura"))
                    .andExpect(jsonPath("$.rechazadas.length()").value(1))
                    .andExpect(jsonPath("$.rechazadas[0].motivo").value("El profesor está inactivo"));
        }

        @Test
        @DisplayName("Listas vacías devuelven 400 señalando el campo y no llaman al servicio")
        void rechazaListasVacias() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"profesorIds\":[],\"asignaturaIds\":[]}"))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString()).contains("profesorIds", "asignaturaIds");
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Elemento nulo en profesorIds devuelve 400 señalando el campo")
        void rechazaProfesorIdNulo() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"profesorIds\":[null],\"asignaturaIds\":[\"" + ASIGNATURA_ID + "\"]}"))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString()).contains("profesorIds");
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Elemento nulo en asignaturaIds devuelve 400 señalando el campo")
        void rechazaAsignaturaIdNulo() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"profesorIds\":[\"" + PROFESOR_ID + "\"],\"asignaturaIds\":[null]}"))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString()).contains("asignaturaIds");
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("JSON mal formado devuelve 400 sin detalles y no llama al servicio")
        void rechazaJsonMalFormado() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"profesorIds\":[}"))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.BAD_REQUEST);
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Profesor inexistente devuelve 404 sin detalles")
        void profesorInexistente() throws Exception {
            given(service.asignarMasivo(any())).willThrow(new ProfesorNoEncontradoException(PROFESOR_ID));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Asignatura inexistente devuelve 404 sin detalles")
        void asignaturaInexistente() throws Exception {
            given(service.asignarMasivo(any())).willThrow(new AsignaturaNoEncontradaException(ASIGNATURA_ID));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }
    }

    // ------------------------------------------------------------------ PATCH

    @Nested
    @DisplayName("PATCH /api/v1/asignaciones-docentes/{id}/desasignar")
    class Desasignar {

        @Test
        @DisplayName("Id existente devuelve 204 sin cuerpo y delega en el servicio")
        void devuelve204() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(patch(RUTA + "/" + id + "/desasignar"))
                    .andExpect(status().isNoContent());

            verify(service).desasignar(id);
        }

        @Test
        @DisplayName("Id inexistente devuelve 404 sin detalles")
        void idInexistente() throws Exception {
            UUID id = UUID.randomUUID();
            willThrow(new AsignacionDocenteNoEncontradaException(id)).given(service).desasignar(id);

            MvcResult resultado = mockMvc.perform(patch(RUTA + "/" + id + "/desasignar")).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Id que no es un UUID devuelve 400 sin detalles y no llama al servicio")
        void idMalFormado() throws Exception {
            MvcResult resultado = mockMvc.perform(patch(RUTA + "/no-es-uuid/desasignar")).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.BAD_REQUEST);
            verifyNoInteractions(service);
        }
    }
}
