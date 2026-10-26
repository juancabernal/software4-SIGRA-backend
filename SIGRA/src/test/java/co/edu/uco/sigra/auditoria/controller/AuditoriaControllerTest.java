package co.edu.uco.sigra.auditoria.controller;

import co.edu.uco.sigra.auditoria.dto.PaginaAuditoriaDTO;
import co.edu.uco.sigra.auditoria.dto.RegistroAuditoriaDTO;
import co.edu.uco.sigra.auditoria.entity.TipoEntidadAuditoria;
import co.edu.uco.sigra.auditoria.exception.AuditoriaExceptionHandler;
import co.edu.uco.sigra.auditoria.exception.RangoFechasInvalidoException;
import co.edu.uco.sigra.auditoria.service.AuditoriaService;
import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.common.exception.ManejadorErroresTransversal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Capa web del reporte de auditoría en modo standalone; la seguridad real la prueba AuditoriaIntegracionIT. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de auditoría")
class AuditoriaControllerTest {

    private static final String URL = "/api/v1/auditoria";

    @Mock
    private AuditoriaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuditoriaController(service))
                .setControllerAdvice(new AuditoriaExceptionHandler(), new ManejadorErroresTransversal(),
                        new GlobalExceptionHandler())
                .build();
    }

    private static PaginaAuditoriaDTO pagina() {
        RegistroAuditoriaDTO registro = new RegistroAuditoriaDTO(UUID.randomUUID(),
                LocalDateTime.of(2026, 10, 20, 9, 30), "CAMBIO_ESTADO", "ASIGNATURA", UUID.randomUUID(),
                UUID.randomUUID(), "ADMINISTRADOR", "Cambio de estado: inactivar (nuevo estado: INACTIVA)");
        return new PaginaAuditoriaDTO(List.of(registro), 0, 20, 1, 1);
    }

    @Test
    @DisplayName("Sin parámetros: página 0 de 20 registros, sin filtros")
    void valoresPorDefecto() throws Exception {
        when(service.consultar(isNull(), isNull(), isNull(), eq(0), eq(20))).thenReturn(pagina());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].accion").value("CAMBIO_ESTADO"))
                .andExpect(jsonPath("$.contenido[0].tipoEntidad").value("ASIGNATURA"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("Pasa al servicio el rango de fechas, el tipo de entidad y la paginación")
    void conFiltros() throws Exception {
        when(service.consultar(any(), any(), any(), anyInt(), anyInt())).thenReturn(pagina());

        mockMvc.perform(get(URL)
                        .param("desde", "2026-10-01")
                        .param("hasta", "2026-10-20")
                        .param("tipoEntidad", "CALIFICACION")
                        .param("pagina", "2")
                        .param("tamano", "50"))
                .andExpect(status().isOk());

        verify(service).consultar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20),
                TipoEntidadAuditoria.CALIFICACION, 2, 50);
    }

    @Test
    @DisplayName("Tipo de entidad desconocido o fecha mal formada: 400 sin llamar al servicio")
    void parametrosInvalidos() throws Exception {
        mockMvc.perform(get(URL).param("tipoEntidad", "NO_EXISTE")).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).param("desde", "20/10/2026")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Rango de fechas invertido: 400 con el mensaje de la regla")
    void rangoInvertido() throws Exception {
        when(service.consultar(any(), any(), any(), anyInt(), anyInt())).thenThrow(new RangoFechasInvalidoException());

        mockMvc.perform(get(URL).param("desde", "2026-10-21").param("hasta", "2026-10-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(new RangoFechasInvalidoException().getMessage()));
    }
}