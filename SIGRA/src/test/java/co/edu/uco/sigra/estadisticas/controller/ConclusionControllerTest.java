package co.edu.uco.sigra.estadisticas.controller;

import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.dto.ConclusionFiltroDTO;
import co.edu.uco.sigra.estadisticas.dto.ConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.GenerarConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.exception.ConclusionNoEncontradaException;
import co.edu.uco.sigra.estadisticas.exception.EstadisticasExceptionHandler;
import co.edu.uco.sigra.estadisticas.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.estadisticas.service.ConclusionService;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la capa web en modo standalone (sin contexto Spring, base de datos ni seguridad),
 * igual que ResultadoAprendizajeControllerTest: rutas, códigos HTTP y formato de error.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de conclusiones automáticas (RF-16)")
class ConclusionControllerTest {

    private static final UUID ID_RA = UUID.randomUUID();
    private static final UUID ID_SEMESTRE = UUID.randomUUID();
    private static final UUID ID_ASIGNATURA = UUID.randomUUID();
    private static final UUID ID_CONCLUSION = UUID.randomUUID();
    private static final UsuarioPrincipal PRINCIPAL =
            new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ADMINISTRADOR);

    private static final String URL_GENERAR =
            "/api/v1/resultados-aprendizaje/" + ID_RA + "/semestres/" + ID_SEMESTRE + "/conclusiones";
    private static final String URL_CONSULTAR = "/api/v1/conclusiones";
    private static final String URL_POR_ID = URL_CONSULTAR + "/" + ID_CONCLUSION;

    @Mock
    private ConclusionService service;
    @Mock
    private CurrentUser currentUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(currentUser.required()).thenReturn(PRINCIPAL);
        mockMvc = MockMvcBuilders.standaloneSetup(new ConclusionController(service, currentUser))
                .setControllerAdvice(new EstadisticasExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    private static ConclusionResponseDTO conclusion() {
        return new ConclusionResponseDTO(ID_CONCLUSION, ID_RA, "RA-01", ID_ASIGNATURA, ID_SEMESTRE, "2025-2",
                "Nivel de logro insuficiente, se recomienda acción de mejora", true, LocalDateTime.now(), 50.0,
                EstadoConclusion.VIGENTE.name());
    }

    @Nested
    @DisplayName("POST generar")
    class Generar {

        @Test
        @DisplayName("Cuando se genera devuelve 200 con la conclusión")
        void generada() throws Exception {
            when(service.generarConclusion(eq(ID_RA), eq(ID_SEMESTRE), any()))
                    .thenReturn(new GenerarConclusionResponseDTO(true, "Se generó una nueva conclusión.", conclusion()));

            mockMvc.perform(post(URL_GENERAR))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.generada").value(true))
                    .andExpect(jsonPath("$.conclusion.estado").value("VIGENTE"))
                    .andExpect(jsonPath("$.conclusion.resultadoAprendizajeId").value(ID_RA.toString()));
        }

        @Test
        @DisplayName("Cuando no se genera (datos insuficientes) devuelve 200 con generada=false")
        void noGenerada() throws Exception {
            when(service.generarConclusion(eq(ID_RA), eq(ID_SEMESTRE), any()))
                    .thenReturn(new GenerarConclusionResponseDTO(false, "Datos insuficientes.", null));

            mockMvc.perform(post(URL_GENERAR))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.generada").value(false))
                    .andExpect(jsonPath("$.conclusion").doesNotExist());
        }

        @Test
        @DisplayName("Con un resultado de aprendizaje inexistente devuelve 404")
        void raInexistente() throws Exception {
            when(service.generarConclusion(eq(ID_RA), eq(ID_SEMESTRE), any()))
                    .thenThrow(new ResultadoAprendizajeNoEncontradoException(ID_RA));

            mockMvc.perform(post(URL_GENERAR))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Con un semestre inexistente devuelve 404")
        void semestreInexistente() throws Exception {
            when(service.generarConclusion(eq(ID_RA), eq(ID_SEMESTRE), any()))
                    .thenThrow(new SemestreNoEncontradoException(ID_SEMESTRE));

            mockMvc.perform(post(URL_GENERAR))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Con un id de RA que no es UUID devuelve 400")
        void idInvalido() throws Exception {
            mockMvc.perform(post("/api/v1/resultados-aprendizaje/abc/semestres/" + ID_SEMESTRE + "/conclusiones"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET consultar")
    class Consultar {

        @Test
        @DisplayName("Sin identidad se rechaza antes de consultar la lista")
        void sinFiltros() throws Exception {
            when(service.consultarConclusiones(any(ConclusionFiltroDTO.class), any()))
                    .thenReturn(List.of(conclusion()));

            mockMvc.perform(get(URL_CONSULTAR))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Por id existente devuelve 200")
        void porId() throws Exception {
            when(service.consultarPorId(eq(ID_CONCLUSION), any())).thenReturn(conclusion());

            mockMvc.perform(get(URL_POR_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID_CONCLUSION.toString()));
        }

        @Test
        @DisplayName("Por id inexistente devuelve 404")
        void porIdInexistente() throws Exception {
            when(service.consultarPorId(eq(ID_CONCLUSION), any()))
                    .thenThrow(new ConclusionNoEncontradaException(ID_CONCLUSION));

            mockMvc.perform(get(URL_POR_ID))
                    .andExpect(status().isNotFound());
        }
    }
}
