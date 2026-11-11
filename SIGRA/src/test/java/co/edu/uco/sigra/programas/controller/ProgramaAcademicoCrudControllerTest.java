package co.edu.uco.sigra.programas.controller;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.exception.AsercionesContratoError;
import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.facultades.exception.FacultadInactivaException;
import co.edu.uco.sigra.facultades.exception.FacultadNoEncontradaException;
import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.dto.ProgramaUpdateDTO;
import co.edu.uco.sigra.programas.exception.CodigoProgramaDuplicadoException;
import co.edu.uco.sigra.programas.exception.NombreProgramaDuplicadoException;
import co.edu.uco.sigra.programas.exception.ProgramaAcademicoNoEncontradoException;
import co.edu.uco.sigra.programas.exception.ProgramaConAsignaturasActivasException;
import co.edu.uco.sigra.programas.exception.ProgramaExceptionHandler;
import co.edu.uco.sigra.programas.service.ProgramaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas web en modo standalone de los endpoints de escritura de programas académicos.
 * Usa los dos manejadores reales: el del módulo para el dominio y el global para la validación.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de programas académicos: registro, modificación e inactivación")
class ProgramaAcademicoCrudControllerTest {

    private static final String URL = "/api/v1/programas";
    private static final UUID FACULTAD_ID = UUID.fromString("7c8d9e0f-1a2b-4c3d-8e4f-5a6b7c8d9e0f");
    private static final String JSON_VALIDO =
            "{\"nombre\":\"Derecho\",\"codigo\":\"DER1\",\"facultadId\":\"" + FACULTAD_ID + "\"}";

    @Mock
    private ProgramaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProgramaAcademicoController(service))
                .setControllerAdvice(new ProgramaExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    private static ProgramaResponseDTO programa(String codigo, String nombre, EstadoRegistro estado) {
        return new ProgramaResponseDTO(UUID.randomUUID(), codigo, nombre, FACULTAD_ID, "Facultad de Ingeniería", estado);
    }

    @Test
    @DisplayName("Registrar un programa válido devuelve 201 con el programa creado")
    void registraDevuelve201() throws Exception {
        when(service.registrar(any())).thenReturn(programa("DER1", "Derecho", EstadoRegistro.ACTIVO));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("DER1"))
                .andExpect(jsonPath("$.facultadId").value(FACULTAD_ID.toString()))
                .andExpect(jsonPath("$.facultadNombre").value("Facultad de Ingeniería"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    @DisplayName("Registrar sin facultad devuelve 400")
    void registraSinFacultad() throws Exception {
        String json = "{\"nombre\":\"Derecho\",\"codigo\":\"DER1\"}";

        AsercionesContratoError.verificarContratoConDetalles(
                mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json)),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Registrar con una facultad inexistente devuelve 404")
    void registraFacultadInexistente() throws Exception {
        when(service.registrar(any())).thenThrow(new FacultadNoEncontradaException(FACULTAD_ID));

        AsercionesContratoError.verificarContratoSinDetalles(
                mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO)),
                HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Registrar en una facultad inactiva devuelve 409")
    void registraFacultadInactiva() throws Exception {
        when(service.registrar(any())).thenThrow(new FacultadInactivaException("Facultad de Ingeniería"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "No se puede asociar un programa a la facultad 'Facultad de Ingeniería' porque está inactiva"));
    }

    @Test
    @DisplayName("Registrar sin nombre devuelve 400 con el detalle del campo")
    void registraSinNombre() throws Exception {
        String json = "{\"nombre\":\"\",\"codigo\":\"DER1\",\"facultadId\":\"" + FACULTAD_ID + "\"}";

        AsercionesContratoError.verificarContratoConDetalles(
                mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json)),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Registrar con código fuera de formato devuelve 400")
    void registraCodigoInvalido() throws Exception {
        String json = "{\"nombre\":\"Derecho\",\"codigo\":\"D R\",\"facultadId\":\"" + FACULTAD_ID + "\"}";

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Registrar un nombre duplicado devuelve 409 sin detalles técnicos")
    void registraNombreDuplicado() throws Exception {
        when(service.registrar(any())).thenThrow(new NombreProgramaDuplicadoException("Derecho"));

        AsercionesContratoError.verificarContratoSinDetalles(
                mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO)),
                HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Registrar un código duplicado devuelve 409")
    void registraCodigoDuplicado() throws Exception {
        when(service.registrar(any())).thenThrow(new CodigoProgramaDuplicadoException("DER1"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("Consultar un id inexistente devuelve 404")
    void consultaIdInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.consultarPorId(id)).thenThrow(new ProgramaAcademicoNoEncontradoException(id));

        AsercionesContratoError.verificarContratoSinDetalles(
                mockMvc.perform(get(URL + "/" + id)), HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Modificar el nombre devuelve 200 con el programa actualizado")
    void modificaNombre() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.actualizar(any(), any())).thenReturn(
                new ProgramaResponseDTO(id, "DER1", "Derecho Civil", FACULTAD_ID, "Facultad de Ingeniería", EstadoRegistro.ACTIVO));

        mockMvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Derecho Civil\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Derecho Civil"))
                .andExpect(jsonPath("$.codigo").value("DER1"));
    }

    @Test
    @DisplayName("Modificar con facultadId pasa la nueva facultad al servicio")
    void modificaFacultad() throws Exception {
        UUID id = UUID.randomUUID();
        UUID nuevaFacultad = UUID.randomUUID();
        when(service.actualizar(any(), any())).thenReturn(
                new ProgramaResponseDTO(id, "DER1", "Derecho", nuevaFacultad, "Facultad de Derecho", EstadoRegistro.ACTIVO));

        mockMvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Derecho\",\"facultadId\":\"" + nuevaFacultad + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facultadId").value(nuevaFacultad.toString()))
                .andExpect(jsonPath("$.facultadNombre").value("Facultad de Derecho"));

        verify(service).actualizar(id, new ProgramaUpdateDTO("Derecho", nuevaFacultad));
    }

    @Test
    @DisplayName("Modificar sin facultadId la envía nula al servicio (conserva la actual)")
    void modificaSinFacultad() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.actualizar(any(), any())).thenReturn(programa("DER1", "Derecho", EstadoRegistro.ACTIVO));

        mockMvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Derecho\"}"))
                .andExpect(status().isOk());

        verify(service).actualizar(id, new ProgramaUpdateDTO("Derecho", null));
    }

    @Test
    @DisplayName("Modificar hacia una facultad inexistente devuelve 404")
    void modificaFacultadInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.actualizar(any(), any())).thenThrow(new FacultadNoEncontradaException(FACULTAD_ID));

        AsercionesContratoError.verificarContratoSinDetalles(
                mockMvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Derecho\",\"facultadId\":\"" + FACULTAD_ID + "\"}")),
                HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Modificar hacia una facultad inactiva devuelve 409")
    void modificaFacultadInactiva() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.actualizar(any(), any())).thenThrow(new FacultadInactivaException("Facultad de Artes"));

        mockMvc.perform(put(URL + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Derecho\",\"facultadId\":\"" + FACULTAD_ID + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "No se puede asociar un programa a la facultad 'Facultad de Artes' porque está inactiva"));
    }

    @Test
    @DisplayName("Inactivar devuelve 200 con el estado INACTIVO")
    void inactiva() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.inactivar(id)).thenReturn(
                new ProgramaResponseDTO(id, "DER1", "Derecho", FACULTAD_ID, "Facultad de Ingeniería", EstadoRegistro.INACTIVO));

        mockMvc.perform(patch(URL + "/" + id + "/inactivar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    @DisplayName("Inactivar un programa con asignaturas activas devuelve 409")
    void inactivaConAsignaturasActivas() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.inactivar(id)).thenThrow(new ProgramaConAsignaturasActivasException("Derecho"));

        mockMvc.perform(patch(URL + "/" + id + "/inactivar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "No se puede inactivar el programa 'Derecho' porque tiene asignaturas activas"));
    }
}
