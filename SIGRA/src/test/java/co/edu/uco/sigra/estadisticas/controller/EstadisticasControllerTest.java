package co.edu.uco.sigra.estadisticas.controller;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.estadisticas.dto.DistribucionNivelLogroDTO;
import co.edu.uco.sigra.estadisticas.dto.EstadisticasResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.GenerarConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RangoNivelLogroResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RecalculoEstadisticasResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.exception.EstadisticasExceptionHandler;
import co.edu.uco.sigra.estadisticas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.estadisticas.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.estadisticas.service.NivelLogroService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Capa web en modo standalone (sin contexto Spring, base de datos ni seguridad), igual que
 * ConclusionControllerTest: rutas, códigos HTTP y formato JSON del contrato.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de nivel de logro y estadísticas (RF-14 / RF-15)")
class EstadisticasControllerTest {

    private static final UUID ID_RA = UUID.randomUUID();
    private static final UUID ID_SEMESTRE = UUID.randomUUID();
    private static final UUID ID_ASIGNATURA = UUID.randomUUID();
    private static final String URL =
            "/api/v1/resultados-aprendizaje/" + ID_RA + "/semestres/" + ID_SEMESTRE + "/estadisticas";
    private static final String URL_RECALCULO = URL + "/recalculo";

    private static final UsuarioPrincipal PRINCIPAL =
            new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ADMINISTRADOR);

    @Mock
    private NivelLogroService service;

    @Mock
    private CurrentUser currentUser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(currentUser.required()).thenReturn(PRINCIPAL);
        mockMvc = MockMvcBuilders.standaloneSetup(new EstadisticasController(service, currentUser))
                .setControllerAdvice(new EstadisticasExceptionHandler(), new GlobalExceptionHandler())
                .build();
    }

    private static EstadisticasResultadoAprendizajeResponseDTO estadisticas() {
        return new EstadisticasResultadoAprendizajeResponseDTO(ID_RA, "RA-01", ID_ASIGNATURA, ID_SEMESTRE, "2026-2",
                10, 8, 1, 1, 3.45, 69.0,
                List.of(new DistribucionNivelLogroDTO(NivelLogro.BAJO, 2, 25.0),
                        new DistribucionNivelLogroDTO(NivelLogro.MEDIO, 2, 25.0),
                        new DistribucionNivelLogroDTO(NivelLogro.ALTO, 3, 37.5),
                        new DistribucionNivelLogroDTO(NivelLogro.SOBRESALIENTE, 1, 12.5)),
                LocalDateTime.of(2026, 10, 13, 12, 0));
    }

    @Nested
    @DisplayName("GET estadísticas")
    class Consultar {

        @Test
        @DisplayName("Devuelve 200 con el contrato acordado para el frontend")
        void ok() throws Exception {
            when(service.consultarEstadisticas(eq(ID_RA), eq(ID_SEMESTRE), eq(PRINCIPAL))).thenReturn(estadisticas());

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resultadoAprendizajeId").value(ID_RA.toString()))
                    .andExpect(jsonPath("$.semestreId").value(ID_SEMESTRE.toString()))
                    .andExpect(jsonPath("$.totalEstudiantes").value(10))
                    .andExpect(jsonPath("$.evaluados").value(8))
                    .andExpect(jsonPath("$.sinDatosSuficientes").value(1))
                    .andExpect(jsonPath("$.sinEvaluar").value(1))
                    .andExpect(jsonPath("$.promedioNota").value(3.45))
                    .andExpect(jsonPath("$.promedioPorcentaje").value(69.0))
                    .andExpect(jsonPath("$.distribucion.length()").value(4))
                    .andExpect(jsonPath("$.distribucion[0].nivel").value("BAJO"))
                    .andExpect(jsonPath("$.distribucion[2].cantidad").value(3))
                    .andExpect(jsonPath("$.distribucion[2].porcentaje").value(37.5));
        }

        @Test
        @DisplayName("RA inexistente: 404")
        void raInexistente() throws Exception {
            when(service.consultarEstadisticas(eq(ID_RA), eq(ID_SEMESTRE), eq(PRINCIPAL)))
                    .thenThrow(new ResultadoAprendizajeNoEncontradoException(ID_RA));

            mockMvc.perform(get(URL)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Semestre inexistente: 404")
        void semestreInexistente() throws Exception {
            when(service.consultarEstadisticas(eq(ID_RA), eq(ID_SEMESTRE), eq(PRINCIPAL)))
                    .thenThrow(new SemestreNoEncontradoException(ID_SEMESTRE));

            mockMvc.perform(get(URL)).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Fuera del alcance del profesor: 403")
        void sinPermiso() throws Exception {
            when(service.consultarEstadisticas(eq(ID_RA), eq(ID_SEMESTRE), eq(PRINCIPAL)))
                    .thenThrow(new PermisoInsuficienteException());

            mockMvc.perform(get(URL)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Id de RA que no es UUID: 400")
        void idInvalido() throws Exception {
            mockMvc.perform(get("/api/v1/resultados-aprendizaje/abc/semestres/" + ID_SEMESTRE + "/estadisticas"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("POST recálculo: 200 con estadísticas y la respuesta de la conclusión (RF-16)")
    void recalculo() throws Exception {
        when(service.recalcular(eq(ID_RA), eq(ID_SEMESTRE), eq(PRINCIPAL))).thenReturn(new RecalculoEstadisticasResponseDTO(
                estadisticas(), new GenerarConclusionResponseDTO(false, "Dentro del umbral.", null)));

        mockMvc.perform(post(URL_RECALCULO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadisticas.evaluados").value(8))
                .andExpect(jsonPath("$.conclusion.generada").value(false));
    }

    @Test
    @DisplayName("GET catálogo de niveles: 200 con los 4 rangos")
    void catalogo() throws Exception {
        when(service.consultarCatalogo()).thenReturn(List.of(
                new RangoNivelLogroResponseDTO(NivelLogro.BAJO, 0.0, 3.0, false),
                new RangoNivelLogroResponseDTO(NivelLogro.MEDIO, 3.0, 4.0, false),
                new RangoNivelLogroResponseDTO(NivelLogro.ALTO, 4.0, 4.5, false),
                new RangoNivelLogroResponseDTO(NivelLogro.SOBRESALIENTE, 4.5, 5.0, true)));

        mockMvc.perform(get("/api/v1/niveles-logro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[3].nivel").value("SOBRESALIENTE"))
                .andExpect(jsonPath("$[3].limiteIncluido").value(true));
    }
}
