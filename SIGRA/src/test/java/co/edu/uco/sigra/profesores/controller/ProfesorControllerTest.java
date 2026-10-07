package co.edu.uco.sigra.profesores.controller;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.exception.AsercionesContratoError;
import co.edu.uco.sigra.profesores.dto.AsignaturaProfesorResponseDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorRequestDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorResponseDTO;
import co.edu.uco.sigra.profesores.exception.CampoNoModificableException;
import co.edu.uco.sigra.profesores.exception.CorreoDuplicadoException;
import co.edu.uco.sigra.profesores.exception.DocumentoDuplicadoException;
import co.edu.uco.sigra.profesores.exception.ProfesorConAsignacionesActivasException;
import co.edu.uco.sigra.profesores.exception.ProfesorNoEncontradoException;
import co.edu.uco.sigra.profesores.exception.TipoDocumentoNoEncontradoException;
import co.edu.uco.sigra.profesores.service.ProfesorService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de capa web de {@link ProfesorController}: deserialización, validación con Jakarta
 * Validation y traducción de excepciones de dominio a códigos HTTP. El servicio va simulado:
 * no hace falta base de datos. Los casos de contrato de error de peticiones mal formadas
 * (415, 406, 409 de integridad) ya los cubre {@code ContratoErrorProfesoresTest}.
 */
@WebMvcTest(controllers = ProfesorController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProfesorController: deserialización, validación y códigos HTTP")
class ProfesorControllerTest {

    private static final String RUTA = "/api/v1/profesores";

    private static final UUID ID = UUID.fromString("1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d");

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
    private ProfesorService profesorService;

    private static ProfesorResponseDTO respuestaDeAna(UUID id) {
        return new ProfesorResponseDTO(
                id, "Cédula de Ciudadanía", "1098765432", "Ana Gómez", "ana.gomez@uco.net.co", "ACTIVO", false);
    }

    private static String cuerpoCon(String campo, String valorJson) {
        // Reemplaza un campo del cuerpo válido por otro valor JSON (ya con comillas si es texto).
        return CUERPO_VALIDO.replaceAll("\"" + campo + "\": \"[^\"]*\"", "\"" + campo + "\": " + valorJson);
    }

    // ------------------------------------------------------------------ POST

    @Nested
    @DisplayName("POST /api/v1/profesores")
    class Registrar {

        @Test
        @DisplayName("Cuerpo válido devuelve 201 con el profesor registrado")
        void cuerpoValidoDevuelve201() throws Exception {
            given(profesorService.registrarProfesor(any(ProfesorRequestDTO.class)))
                    .willReturn(respuestaDeAna(ID));

            mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.numeroDocumento").value("1098765432"))
                    .andExpect(jsonPath("$.nombreCompleto").value("Ana Gómez"))
                    .andExpect(jsonPath("$.correoInstitucional").value("ana.gomez@uco.net.co"))
                    .andExpect(jsonPath("$.estado").value("ACTIVO"))
                    .andExpect(jsonPath("$.tieneAsignacionesActivas").value(false));
        }

        @Test
        @DisplayName("Número de documento con letras devuelve 400 señalando el campo y no llama al servicio")
        void numeroDocumentoConLetras() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCon("numeroDocumento", "\"ABC123\"")))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString()).contains("numeroDocumento");
            verifyNoInteractions(profesorService);
        }

        @Test
        @DisplayName("Correo de dominio externo devuelve 400 señalando el campo")
        void correoExterno() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCon("correoInstitucional", "\"ana@gmail.com\"")))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString()).contains("correoInstitucional");
            verifyNoInteractions(profesorService);
        }

        @Test
        @DisplayName("Nombre vacío devuelve 400 señalando el campo")
        void nombreVacio() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCon("nombreCompleto", "\"\"")))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString()).contains("nombreCompleto");
            verifyNoInteractions(profesorService);
        }

        @Test
        @DisplayName("Campo faltante (tipoDocumentoId) devuelve 400 señalando el campo")
        void tipoDocumentoFaltante() throws Exception {
            String cuerpoSinTipo = """
                    {
                      "numeroDocumento": "1098765432",
                      "nombreCompleto": "Ana Gómez",
                      "correoInstitucional": "ana.gomez@uco.net.co"
                    }
                    """;

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoSinTipo))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString()).contains("tipoDocumentoId");
            verifyNoInteractions(profesorService);
        }

        @Test
        @DisplayName("Documento ya registrado devuelve 409 sin detalles y con el documento en el mensaje")
        void documentoDuplicado() throws Exception {
            given(profesorService.registrarProfesor(any(ProfesorRequestDTO.class)))
                    .willThrow(new DocumentoDuplicadoException("1098765432"));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);

            assertThat(cuerpo.get("mensaje").stringValue()).contains("1098765432");
        }

        @Test
        @DisplayName("Correo ya registrado devuelve 409 sin detalles")
        void correoDuplicado() throws Exception {
            given(profesorService.registrarProfesor(any(ProfesorRequestDTO.class)))
                    .willThrow(new CorreoDuplicadoException("ana.gomez@uco.net.co"));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Tipo de documento inexistente devuelve 404 sin detalles")
        void tipoDocumentoInexistente() throws Exception {
            given(profesorService.registrarProfesor(any(ProfesorRequestDTO.class)))
                    .willThrow(new TipoDocumentoNoEncontradoException(UUID.randomUUID()));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }
    }

    // ------------------------------------------------------------------ GET lista

    @Nested
    @DisplayName("GET /api/v1/profesores")
    class Consultar {

        @Test
        @DisplayName("Sin filtro devuelve 200 con la lista que da el servicio")
        void sinFiltro() throws Exception {
            given(profesorService.consultar(null)).willReturn(List.of(respuestaDeAna(ID)));

            mockMvc.perform(get(RUTA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(ID.toString()));
        }

        @Test
        @DisplayName("Con filtro llama al servicio con el valor recibido")
        void conFiltro() throws Exception {
            given(profesorService.consultar("ana")).willReturn(List.of());

            mockMvc.perform(get(RUTA).param("filtro", "ana"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(profesorService).consultar("ana");
        }
    }

    // ------------------------------------------------------------------ GET asignaturas

    @Nested
    @DisplayName("GET /api/v1/profesores/{id}/asignaturas")
    class ConsultarAsignaturas {

        private AsignaturaProfesorResponseDTO asignatura() {
            return new AsignaturaProfesorResponseDTO(
                    UUID.fromString("2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e"),
                    UUID.fromString("3c4d5e6f-7a8b-4c9d-0e1f-2a3b4c5d6e7f"),
                    "MAT-101", "Cálculo", EstadoRegistro.ACTIVO);
        }

        @Test
        @DisplayName("Sin estado devuelve 200 con las asignaturas del profesor")
        void sinEstado() throws Exception {
            given(profesorService.consultarAsignaturas(ID, null)).willReturn(List.of(asignatura()));

            mockMvc.perform(get(RUTA + "/" + ID + "/asignaturas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].codigoAsignatura").value("MAT-101"))
                    .andExpect(jsonPath("$[0].nombreAsignatura").value("Cálculo"))
                    .andExpect(jsonPath("$[0].estadoAsignatura").value("ACTIVO"));
        }

        @Test
        @DisplayName("Con estado=INACTIVO lo pasa al servicio como EstadoRegistro")
        void conEstado() throws Exception {
            given(profesorService.consultarAsignaturas(ID, EstadoRegistro.INACTIVO)).willReturn(List.of());

            mockMvc.perform(get(RUTA + "/" + ID + "/asignaturas").param("estado", "INACTIVO"))
                    .andExpect(status().isOk());

            verify(profesorService).consultarAsignaturas(ID, EstadoRegistro.INACTIVO);
        }

        @Test
        @DisplayName("Estado que no existe devuelve 400 sin detalles")
        void estadoInvalido() throws Exception {
            MvcResult resultado = mockMvc.perform(get(RUTA + "/" + ID + "/asignaturas")
                            .param("estado", "NO_EXISTE"))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.BAD_REQUEST);
            verifyNoInteractions(profesorService);
        }

        @Test
        @DisplayName("Profesor inexistente devuelve 404 sin detalles")
        void profesorInexistente() throws Exception {
            given(profesorService.consultarAsignaturas(ID, null))
                    .willThrow(new ProfesorNoEncontradoException(ID));

            MvcResult resultado = mockMvc.perform(get(RUTA + "/" + ID + "/asignaturas")).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }
    }

    // ------------------------------------------------------------------ PUT

    @Nested
    @DisplayName("PUT /api/v1/profesores/{id}")
    class Modificar {

        @Test
        @DisplayName("Cuerpo válido devuelve 200 con el profesor modificado")
        void cuerpoValido() throws Exception {
            given(profesorService.modificarProfesor(any(UUID.class), any(ProfesorRequestDTO.class)))
                    .willReturn(respuestaDeAna(ID));

            mockMvc.perform(put(RUTA + "/" + ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()));

            verify(profesorService).modificarProfesor(any(UUID.class), any(ProfesorRequestDTO.class));
        }

        @Test
        @DisplayName("Cuerpo inválido devuelve 400 y no llama al servicio")
        void cuerpoInvalido() throws Exception {
            MvcResult resultado = mockMvc.perform(put(RUTA + "/" + ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCon("numeroDocumento", "\"12\"")))
                    .andReturn();

            AsercionesContratoError.verificarContratoConDetalles(resultado, HttpStatus.BAD_REQUEST);
            verifyNoInteractions(profesorService);
        }

        @Test
        @DisplayName("Cambiar el documento devuelve 400 sin detalles")
        void documentoNoModificable() throws Exception {
            given(profesorService.modificarProfesor(any(UUID.class), any(ProfesorRequestDTO.class)))
                    .willThrow(new CampoNoModificableException("numeroDocumento"));

            MvcResult resultado = mockMvc.perform(put(RUTA + "/" + ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("mensaje").stringValue()).contains("numeroDocumento");
        }

        @Test
        @DisplayName("Profesor inexistente devuelve 404 sin detalles")
        void profesorInexistente() throws Exception {
            given(profesorService.modificarProfesor(any(UUID.class), any(ProfesorRequestDTO.class)))
                    .willThrow(new ProfesorNoEncontradoException(ID));

            MvcResult resultado = mockMvc.perform(put(RUTA + "/" + ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Id que no es un UUID devuelve 400 sin detalles")
        void idMalFormado() throws Exception {
            MvcResult resultado = mockMvc.perform(put(RUTA + "/no-es-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.BAD_REQUEST);
            verifyNoInteractions(profesorService);
        }
    }

    // ------------------------------------------------------------------ DELETE

    @Nested
    @DisplayName("DELETE /api/v1/profesores/{id}")
    class Inactivar {

        @Test
        @DisplayName("Profesor sin asignaciones activas devuelve 204 sin cuerpo")
        void devuelve204() throws Exception {
            mockMvc.perform(delete(RUTA + "/" + ID))
                    .andExpect(status().isNoContent());

            verify(profesorService).inactivar(ID);
        }

        @Test
        @DisplayName("Profesor con asignaciones activas devuelve 400 sin detalles")
        void conAsignacionesActivas() throws Exception {
            willThrow(new ProfesorConAsignacionesActivasException(ID))
                    .given(profesorService).inactivar(ID);

            MvcResult resultado = mockMvc.perform(delete(RUTA + "/" + ID)).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Profesor inexistente devuelve 404 sin detalles")
        void profesorInexistente() throws Exception {
            willThrow(new ProfesorNoEncontradoException(ID))
                    .given(profesorService).inactivar(ID);

            MvcResult resultado = mockMvc.perform(delete(RUTA + "/" + ID)).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }
    }
}