package co.edu.uco.sigra.programas.controller;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.service.ProgramaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de la capa web en modo standalone: no levanta contexto Spring, base de datos ni seguridad.
 * El manejador global atiende los errores de este controlador (no hay manejador de módulo).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de programas académicos")
class ProgramaAcademicoControllerTest {

    private static final String URL = "/api/v1/programas";

    @Mock
    private ProgramaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProgramaAcademicoController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static ProgramaResponseDTO programa(String codigo, String nombre, EstadoRegistro estado) {
        return new ProgramaResponseDTO(UUID.randomUUID(), codigo, nombre, estado);
    }

    @Test
    @DisplayName("Sin filtro devuelve 200 con la lista de programas")
    void listaDeProgramas() throws Exception {
        when(service.consultar(null, null, null)).thenReturn(List.of(
                programa("DER", "Derecho", EstadoRegistro.ACTIVO),
                programa("MED", "Medicina", EstadoRegistro.INACTIVO)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("DER"))
                .andExpect(jsonPath("$[0].nombre").value("Derecho"))
                .andExpect(jsonPath("$[0].estado").value("ACTIVO"))
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    @DisplayName("Sin programas devuelve 200 y una lista vacía")
    void listaVacia() throws Exception {
        when(service.consultar(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("El filtro de estado llega al servicio")
    void filtroLlegaAlServicio() throws Exception {
        when(service.consultar(null, null, EstadoRegistro.ACTIVO)).thenReturn(List.of());

        mockMvc.perform(get(URL).param("estado", "ACTIVO"))
                .andExpect(status().isOk());

        verify(service).consultar(null, null, EstadoRegistro.ACTIVO);
    }

    @Test
    @DisplayName("Un estado inválido devuelve 400 desde el manejador global y no llama al servicio")
    void estadoInvalido() throws Exception {
        mockMvc.perform(get(URL).param("estado", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.mensaje").value("El valor del parámetro 'estado' no es válido"));

        verifyNoInteractions(service);
    }
}
