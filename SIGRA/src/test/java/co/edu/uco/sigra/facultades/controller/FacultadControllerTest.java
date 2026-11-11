package co.edu.uco.sigra.facultades.controller;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.exception.AsercionesContratoError;
import co.edu.uco.sigra.facultades.dto.FacultadRequestDTO;
import co.edu.uco.sigra.facultades.dto.FacultadResponseDTO;
import co.edu.uco.sigra.facultades.exception.FacultadConProgramasActivosException;
import co.edu.uco.sigra.facultades.exception.FacultadNoEncontradaException;
import co.edu.uco.sigra.facultades.exception.NombreFacultadDuplicadoException;
import co.edu.uco.sigra.facultades.service.FacultadService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de capa web de {@link FacultadController}: deserialización, validación con Jakarta
 * Validation y traducción de excepciones de dominio a códigos HTTP. El servicio va simulado: no hace
 * falta base de datos. Los casos de contrato de error de peticiones mal formadas (415, 406, 409 de
 * integridad) los cubre {@code ContratoErrorFacultadesTest}; la autorización por rol, la prueba de
 * integración {@code FacultadIntegracionIT}.
 */
@WebMvcTest(controllers = FacultadController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FacultadController: deserialización, validación y códigos HTTP")
class FacultadControllerTest {

    private static final String RUTA = "/api/v1/facultades";

    private static final UUID ID = UUID.fromString("7c8d9e0f-1a2b-4c3d-8e4f-5a6b7c8d9e0f");

    private static final String CUERPO_VALIDO = """
            {
              "nombre": "Facultad de Ingeniería"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FacultadService facultadService;

    private static FacultadResponseDTO respuesta(UUID id, String nombre, EstadoRegistro estado) {
        return new FacultadResponseDTO(id, nombre, estado);
    }

    private static String cuerpoConNombre(String valorJson) {
        return "{\"nombre\": " + valorJson + "}";
    }

    // ------------------------------------------------------------------ POST

    @Nested
    @DisplayName("POST /api/v1/facultades")
    class Registrar {

        @Test
        @DisplayName("Cuerpo válido devuelve 201 con la facultad registrada")
        void cuerpoValidoDevuelve201() throws Exception {
            given(facultadService.registrar(any(FacultadRequestDTO.class)))
                    .willReturn(respuesta(ID, "Facultad de Ingeniería", EstadoRegistro.ACTIVO));

            mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.nombre").value("Facultad de Ingeniería"))
                    .andExpect(jsonPath("$.estado").value("ACTIVO"));
        }

        @Test
        @DisplayName("Pasa al servicio el nombre tal como llegó (el recorte lo hace el servicio)")
        void pasaElNombreAlServicio() throws Exception {
            given(facultadService.registrar(any(FacultadRequestDTO.class)))
                    .willReturn(respuesta(ID, "Facultad de Ingeniería", EstadoRegistro.ACTIVO));

            mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoConNombre("\"  Facultad de Ingeniería \"")))
                    .andExpect(status().isCreated());

            verify(facultadService).registrar(new FacultadRequestDTO("  Facultad de Ingeniería "));
        }

        @Test
        @DisplayName("Nombre vacío devuelve 400 señalando el campo y no llama al servicio")
        void nombreVacio() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoConNombre("\"\"")))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString()).contains("nombre");
            verifyNoInteractions(facultadService);
        }

        @Test
        @DisplayName("Nombre de más de 150 caracteres devuelve 400 señalando el campo")
        void nombreDemasiadoLargo() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoConNombre("\"" + "A".repeat(151) + "\"")))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString()).contains("nombre");
            verifyNoInteractions(facultadService);
        }

        @Test
        @DisplayName("Campo faltante (nombre) devuelve 400 señalando el campo")
        void nombreFaltante() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString()).contains("nombre");
            verifyNoInteractions(facultadService);
        }

        @Test
        @DisplayName("JSON mal formado devuelve 400 y no llama al servicio")
        void jsonMalFormado() throws Exception {
            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\": "))
                    .andReturn();

            assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
            verifyNoInteractions(facultadService);
        }

        @Test
        @DisplayName("Nombre ya registrado devuelve 409 sin detalles y con el nombre en el mensaje")
        void nombreDuplicado() throws Exception {
            given(facultadService.registrar(any(FacultadRequestDTO.class)))
                    .willThrow(new NombreFacultadDuplicadoException("Facultad de Ingeniería"));

            MvcResult resultado = mockMvc.perform(post(RUTA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);

            assertThat(cuerpo.get("mensaje").stringValue()).contains("Facultad de Ingeniería");
        }
    }

    // ------------------------------------------------------------------ GET lista

    @Nested
    @DisplayName("GET /api/v1/facultades")
    class Consultar {

        @Test
        @DisplayName("Sin filtros devuelve 200 con la lista que da el servicio")
        void sinFiltros() throws Exception {
            given(facultadService.consultar(null, null)).willReturn(List.of(
                    respuesta(ID, "Facultad de Derecho", EstadoRegistro.ACTIVO),
                    respuesta(UUID.randomUUID(), "Facultad de Medicina", EstadoRegistro.INACTIVO)));

            mockMvc.perform(get(RUTA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].nombre").value("Facultad de Derecho"))
                    .andExpect(jsonPath("$[1].estado").value("INACTIVO"));
        }

        @Test
        @DisplayName("Con filtros llama al servicio con el nombre y el estado recibidos")
        void conFiltros() throws Exception {
            given(facultadService.consultar("ingenieria", EstadoRegistro.ACTIVO)).willReturn(List.of(
                    respuesta(ID, "Facultad de Ingeniería", EstadoRegistro.ACTIVO)));

            mockMvc.perform(get(RUTA).param("nombre", "ingenieria").param("estado", "ACTIVO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(facultadService).consultar("ingenieria", EstadoRegistro.ACTIVO);
        }

        @Test
        @DisplayName("Sin facultades devuelve 200 con lista vacía")
        void listaVacia() throws Exception {
            given(facultadService.consultar(null, null)).willReturn(List.of());

            mockMvc.perform(get(RUTA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Estado que no existe devuelve 400 sin detalles y no llama al servicio")
        void estadoInexistente() throws Exception {
            MvcResult resultado = mockMvc.perform(get(RUTA).param("estado", "BORRADO")).andReturn();

            assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
            verifyNoInteractions(facultadService);
        }
    }

    // ------------------------------------------------------------------ GET por id

    @Nested
    @DisplayName("GET /api/v1/facultades/{id}")
    class ConsultarPorId {

        @Test
        @DisplayName("Facultad existente devuelve 200 con sus datos")
        void existente() throws Exception {
            given(facultadService.consultarPorId(ID))
                    .willReturn(respuesta(ID, "Facultad de Ingeniería", EstadoRegistro.ACTIVO));

            mockMvc.perform(get(RUTA + "/" + ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.nombre").value("Facultad de Ingeniería"));
        }

        @Test
        @DisplayName("Facultad inexistente devuelve 404 sin detalles")
        void inexistente() throws Exception {
            given(facultadService.consultarPorId(ID)).willThrow(new FacultadNoEncontradaException(ID));

            MvcResult resultado = mockMvc.perform(get(RUTA + "/" + ID)).andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.NOT_FOUND);
            assertThat(cuerpo.get("mensaje").stringValue()).contains(ID.toString());
        }

        @Test
        @DisplayName("Id que no es un UUID devuelve 400 sin detalles y no llama al servicio")
        void idNoUuid() throws Exception {
            MvcResult resultado = mockMvc.perform(get(RUTA + "/no-es-uuid")).andReturn();

            assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
            verifyNoInteractions(facultadService);
        }
    }

    // ------------------------------------------------------------------ PUT

    @Nested
    @DisplayName("PUT /api/v1/facultades/{id}")
    class Modificar {

        @Test
        @DisplayName("Cuerpo válido devuelve 200 con la facultad modificada")
        void cuerpoValido() throws Exception {
            given(facultadService.actualizarNombre(eq(ID), any(FacultadRequestDTO.class)))
                    .willReturn(respuesta(ID, "Facultad de Ciencias", EstadoRegistro.ACTIVO));

            mockMvc.perform(put(RUTA + "/" + ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoConNombre("\"Facultad de Ciencias\"")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.nombre").value("Facultad de Ciencias"));
        }

        @Test
        @DisplayName("Cuerpo inválido devuelve 400 y no llama al servicio")
        void cuerpoInvalido() throws Exception {
            MvcResult resultado = mockMvc.perform(put(RUTA + "/" + ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoConNombre("\"   \"")))
                    .andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(
                    resultado, HttpStatus.BAD_REQUEST);

            assertThat(cuerpo.get("detalles").toString()).contains("nombre");
            verifyNoInteractions(facultadService);
        }

        @Test
        @DisplayName("Nombre de otra facultad devuelve 409 sin detalles")
        void nombreDuplicado() throws Exception {
            given(facultadService.actualizarNombre(eq(ID), any(FacultadRequestDTO.class)))
                    .willThrow(new NombreFacultadDuplicadoException("Facultad de Medicina"));

            MvcResult resultado = mockMvc.perform(put(RUTA + "/" + ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoConNombre("\"Facultad de Medicina\"")))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("Facultad inexistente devuelve 404 sin detalles")
        void inexistente() throws Exception {
            given(facultadService.actualizarNombre(eq(ID), any(FacultadRequestDTO.class)))
                    .willThrow(new FacultadNoEncontradaException(ID));

            MvcResult resultado = mockMvc.perform(put(RUTA + "/" + ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Id que no es un UUID devuelve 400 sin detalles")
        void idNoUuid() throws Exception {
            MvcResult resultado = mockMvc.perform(put(RUTA + "/123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CUERPO_VALIDO))
                    .andReturn();

            assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
            verifyNoInteractions(facultadService);
        }
    }

    // ------------------------------------------------------------------ PATCH inactivar

    @Nested
    @DisplayName("PATCH /api/v1/facultades/{id}/inactivar")
    class Inactivar {

        @Test
        @DisplayName("Facultad sin programas activos devuelve 200 con estado INACTIVO")
        void sinProgramasActivos() throws Exception {
            given(facultadService.inactivar(ID))
                    .willReturn(respuesta(ID, "Facultad de Medicina", EstadoRegistro.INACTIVO));

            mockMvc.perform(patch(RUTA + "/" + ID + "/inactivar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.estado").value("INACTIVO"));
        }

        @Test
        @DisplayName("Facultad con programas activos devuelve 409 sin detalles y con el motivo")
        void conProgramasActivos() throws Exception {
            given(facultadService.inactivar(ID))
                    .willThrow(new FacultadConProgramasActivosException("Facultad de Ingeniería"));

            MvcResult resultado = mockMvc.perform(patch(RUTA + "/" + ID + "/inactivar")).andReturn();

            JsonNode cuerpo = AsercionesContratoError.verificarContratoSinDetalles(
                    resultado, HttpStatus.CONFLICT);
            assertThat(cuerpo.get("mensaje").stringValue())
                    .isEqualTo("No se puede inactivar la facultad 'Facultad de Ingeniería' porque tiene programas activos");
        }

        @Test
        @DisplayName("Facultad inexistente devuelve 404 sin detalles")
        void inexistente() throws Exception {
            given(facultadService.inactivar(ID)).willThrow(new FacultadNoEncontradaException(ID));

            MvcResult resultado = mockMvc.perform(patch(RUTA + "/" + ID + "/inactivar")).andReturn();

            AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Id que no es un UUID devuelve 400 sin detalles")
        void idNoUuid() throws Exception {
            MvcResult resultado = mockMvc.perform(patch(RUTA + "/abc/inactivar")).andReturn();

            assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
            verifyNoInteractions(facultadService);
        }
    }
}
