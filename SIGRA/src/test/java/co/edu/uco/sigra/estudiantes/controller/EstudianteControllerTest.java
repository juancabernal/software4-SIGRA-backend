package co.edu.uco.sigra.estudiantes.controller;

import co.edu.uco.sigra.estudiantes.dto.AsignaturaDeEstudianteDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteResponseDTO;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.exception.DocumentoNoModificableException;
import co.edu.uco.sigra.estudiantes.exception.EstudianteNoEncontradoException;
import co.edu.uco.sigra.estudiantes.exception.EstudianteYaRegistradoException;
import co.edu.uco.sigra.estudiantes.mapper.MatriculaMapper;
import co.edu.uco.sigra.estudiantes.service.EstudianteService;
import co.edu.uco.sigra.estudiantes.service.MatriculaService;
import co.edu.uco.sigra.common.exception.AsercionesContratoError;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de capa web con contexto de Spring (RF-09a): verifica que
 * {@link EstudianteController} deserializa, valida con Jakarta Validation y traduce las
 * excepciones de dominio al contrato de error estándar, apoyándose en
 * {@link co.edu.uco.sigra.estudiantes.exception.ManejadorErroresEstudiantes}.
 *
 * <p>{@code @WebMvcTest} con {@code EstudianteService} mockeado es el nivel mínimo suficiente:
 * no hace falta base de datos para comprobar deserialización, validación y códigos HTTP, pero sí
 * hace falta el contexto de Spring para que la validación de {@code @Valid} y el advice real
 * entren en juego — con mocks puros (standalone) no se ejercita el {@code RestControllerAdvice}
 * ni el contrato de error completo.
 */
@WebMvcTest(controllers = EstudianteController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EstudianteController: deserialización, validación y códigos HTTP (RF-09a)")
class EstudianteControllerTest {

    private static final String RUTA = "/api/v1/estudiantes";

    private static final String CUERPO_VALIDO = """
            {
              "tipoDocumentoId": "6b1f9d7e-5c3a-4f21-9e8b-2d4a7c6e1f05",
              "numeroDocumento": "1098765432",
              "nombreCompleto": "Ana Gómez",
              "correoInstitucional": "ana.gomez@uco.net.co"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EstudianteService estudianteService;

    @MockitoBean
    private MatriculaService matriculaService;

    @MockitoBean
    private MatriculaMapper matriculaMapper;

    private static EstudianteResponseDTO respuestaDeAna(UUID id) {
        return new EstudianteResponseDTO(
                id, "Cédula de ciudadanía", "1098765432", "Ana Gómez", "ana.gomez@uco.net.co", "ACTIVO");
    }

    @Nested
    @DisplayName("POST /api/v1/estudiantes")
    class Registrar {

        @Test
        @DisplayName("Cuerpo válido devuelve 201 con el estudiante registrado")
        void cuerpoValidoDevuelve201() throws Exception {
            UUID id = UUID.fromString("1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d");
            given(estudianteService.registrar(any(EstudianteRequestDTO.class)))
                    .willReturn(respuestaDeAna(id));

            mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.numeroDocumento").value("1098765432"))
                    .andExpect(jsonPath("$.correoInstitucional").value("ana.gomez@uco.net.co"))
                    .andExpect(jsonPath("$.estado").value("ACTIVO"));
        }

        @Test
        @DisplayName("Número de documento con letras devuelve 400 con el campo señalado en detalles")
        void numeroDocumentoConLetrasDevuelve400() throws Exception {
            String cuerpoInvalido = """
                    {
                      "tipoDocumentoId": "6b1f9d7e-5c3a-4f21-9e8b-2d4a7c6e1f05",
                      "numeroDocumento": "ABC123",
                      "nombreCompleto": "Ana Gómez",
                      "correoInstitucional": "ana.gomez@uco.net.co"
                    }
                    """;

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoInvalido))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString())
                    .as("El detalle debe nombrar el campo inválido para que el cliente sepa qué corregir")
                    .contains("numeroDocumento");
        }

        @Test
        @DisplayName("Campo faltante (tipoDocumentoId) devuelve 400 con el campo señalado en detalles")
        void campoFaltanteDevuelve400() throws Exception {
            String cuerpoInvalido = """
                    {
                      "numeroDocumento": "1098765432",
                      "nombreCompleto": "Ana Gómez",
                      "correoInstitucional": "ana.gomez@uco.net.co"
                    }
                    """;

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoInvalido))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString())
                    .as("El detalle debe nombrar el campo faltante")
                    .contains("tipoDocumentoId");
        }

        @Test
        @DisplayName("Documento ya registrado devuelve 409 sin detalles")
        void documentoYaRegistradoDevuelve409() throws Exception {
            given(estudianteService.registrar(any(EstudianteRequestDTO.class)))
                    .willThrow(EstudianteYaRegistradoException.porDocumento("1098765432"));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);

            assertThat(cuerpo.get("mensaje").stringValue())
                    .as("El mensaje debe traer el documento duplicado")
                    .contains("1098765432");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/estudiantes")
    class Consultar {

        @Test
        @DisplayName("Sin filtro devuelve 200 con la lista que da el servicio")
        void sinFiltroDevuelve200ConLaLista() throws Exception {
            UUID id = UUID.fromString("2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e");
            given(estudianteService.consultar(null)).willReturn(List.of(respuestaDeAna(id)));

            mockMvc.perform(get(RUTA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(id.toString()));
        }

        @Test
        @DisplayName("Con filtro llama al servicio con el valor recibido")
        void conFiltroLlamaAlServicioConEseValor() throws Exception {
            given(estudianteService.consultar(eq("ana"))).willReturn(List.of());

            mockMvc.perform(get(RUTA).param("filtro", "ana"))
                    .andExpect(status().isOk());

            verify(estudianteService).consultar("ana");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/estudiantes/{id}")
    class ConsultarPorId {

        @Test
        @DisplayName("Id existente devuelve 200 con el estudiante")
        void idExistenteDevuelve200() throws Exception {
            UUID id = UUID.fromString("3c4d5e6f-7a8b-4c9d-0e1f-2a3b4c5d6e7f");
            given(estudianteService.consultarPorId(id)).willReturn(respuestaDeAna(id));

            mockMvc.perform(get(RUTA + "/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()));
        }

        @Test
        @DisplayName("Id inexistente devuelve 404 sin detalles")
        void idInexistenteDevuelve404() throws Exception {
            UUID id = UUID.fromString("4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f80");
            given(estudianteService.consultarPorId(id))
                    .willThrow(EstudianteNoEncontradoException.porId(id));

            MvcResult resultado = mockMvc.perform(get(RUTA + "/" + id)).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/estudiantes/{id}")
    class Modificar {

        @Test
        @DisplayName("Cuerpo válido devuelve 200 con el estudiante actualizado")
        void cuerpoValidoDevuelve200() throws Exception {
            UUID id = UUID.fromString("5e6f7a8b-9c0d-4e1f-2a3b-4c5d6e7f8091");
            given(estudianteService.modificar(eq(id), any(EstudianteRequestDTO.class)))
                    .willReturn(respuestaDeAna(id));

            mockMvc.perform(put(RUTA + "/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()));
        }

        @Test
        @DisplayName("Documento no modificable devuelve 400 sin detalles (regla de negocio, no @Valid)")
        void documentoNoModificableDevuelve400SinDetalles() throws Exception {
            UUID id = UUID.fromString("6f7a8b9c-0d1e-4f2a-3b4c-5d6e7f809122");
            given(estudianteService.modificar(eq(id), any(EstudianteRequestDTO.class)))
                    .willThrow(DocumentoNoModificableException.paraEstudiante());

            MvcResult resultado = mockMvc.perform(put(RUTA + "/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/estudiantes/{id}")
    class Inactivar {

        @Test
        @DisplayName("Devuelve 204 sin cuerpo")
        void devuelve204SinCuerpo() throws Exception {
            UUID id = UUID.fromString("7a8b9c0d-1e2f-4a3b-4c5d-6e7f80912233");

            mockMvc.perform(delete(RUTA + "/" + id))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/estudiantes/{id}/asignaturas")
    class ConsultarAsignaturas {

        @Test
        @DisplayName("Historial de matrículas de un estudiante devuelve 200 con una entrada por matrícula")
        void historialDeMatriculasDevuelve200ConUnaEntradaPorMatricula() throws Exception {
            UUID id = UUID.fromString("8b9c0d1e-2f3a-4b4c-5d6e-7f8091223344");
            List<Matricula> matriculas = List.of(new Matricula(), new Matricula());
            List<AsignaturaDeEstudianteDTO> asignaturas = List.of(
                    new AsignaturaDeEstudianteDTO(
                            UUID.fromString("9c0d1e2f-3a4b-4c5d-6e7f-809122334455"),
                            "Cálculo I", "2026-1", "ACTIVO"),
                    new AsignaturaDeEstudianteDTO(
                            UUID.fromString("0d1e2f3a-4b5c-4d6e-7f80-912233445566"),
                            "Álgebra Lineal", "2025-2", "INACTIVO"));

            given(matriculaService.listarAsignaturasDeEstudiante(id)).willReturn(matriculas);
            given(matriculaMapper.toAsignaturaDeEstudianteDTOList(matriculas)).willReturn(asignaturas);

            mockMvc.perform(get(RUTA + "/" + id + "/asignaturas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].matriculaId").value("9c0d1e2f-3a4b-4c5d-6e7f-809122334455"))
                    .andExpect(jsonPath("$[0].asignaturaNombre").value("Cálculo I"))
                    .andExpect(jsonPath("$[0].semestreCodigo").value("2026-1"))
                    .andExpect(jsonPath("$[0].estado").value("ACTIVO"))
                    .andExpect(jsonPath("$[1].matriculaId").value("0d1e2f3a-4b5c-4d6e-7f80-912233445566"))
                    .andExpect(jsonPath("$[1].asignaturaNombre").value("Álgebra Lineal"))
                    .andExpect(jsonPath("$[1].semestreCodigo").value("2025-2"))
                    .andExpect(jsonPath("$[1].estado").value("INACTIVO"));
        }

        @Test
        @DisplayName("Estudiante sin matrículas devuelve 200 con lista vacía")
        void estudianteSinMatriculasDevuelve200ConListaVacia() throws Exception {
            UUID id = UUID.fromString("1e2f3a4b-5c6d-4e7f-8091-223344556677");

            given(matriculaService.listarAsignaturasDeEstudiante(id)).willReturn(List.of());
            given(matriculaMapper.toAsignaturaDeEstudianteDTOList(List.of())).willReturn(List.of());

            mockMvc.perform(get(RUTA + "/" + id + "/asignaturas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Consulta sobre un estudiante inexistente devuelve 404 sin detalles")
        void estudianteInexistenteDevuelve404() throws Exception {
            UUID id = UUID.fromString("2f3a4b5c-6d7e-4f80-9122-334455667788");

            given(matriculaService.listarAsignaturasDeEstudiante(id))
                    .willThrow(EstudianteNoEncontradoException.porId(id));

            MvcResult resultado = mockMvc.perform(get(RUTA + "/" + id + "/asignaturas")).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }
    }
}
