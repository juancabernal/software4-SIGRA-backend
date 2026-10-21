package co.edu.uco.sigra.estadisticas.service.impl;

import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.repository.CalificacionesNivelLogroRepository;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
import co.edu.uco.sigra.estadisticas.service.NivelLogroService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Recálculo programado del nivel de logro")
class RecalculoProgramadoNivelLogroTest {

    @Mock
    private CalificacionesNivelLogroRepository repository;

    @Mock
    private NivelLogroEstudianteRepository nivelLogroEstudianteRepository;

    @Mock
    private NivelLogroService nivelLogroService;

    @InjectMocks
    private RecalculoProgramadoNivelLogro job;

    @Test
    @DisplayName("Recalcula cada (RA, semestre en curso) con la identidad interna del sistema")
    void recalculaCadaPar() {
        UUID ra1 = UUID.randomUUID();
        UUID ra2 = UUID.randomUUID();
        UUID semestre = UUID.randomUUID();
        LocalDateTime ultimoCalculo = LocalDateTime.of(2024, 1, 5, 8, 0);
        LocalDateTime ultimaCalificacion = LocalDateTime.of(2024, 1, 10, 8, 0);
        when(repository.buscarParesConEvaluacionesEnSemestreEnCurso(LocalDate.now(ZoneId.of("America/Bogota"))))
                .thenReturn(List.<Object[]>of(new Object[]{ra1, semestre}, new Object[]{ra2, semestre}));
        when(repository.buscarUltimaCalificacionQueCuentaPorParEnSemestreEnCurso(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{ra1, semestre, ultimaCalificacion},
                        new Object[]{ra2, semestre, ultimaCalificacion}));
        when(nivelLogroEstudianteRepository.buscarUltimoCalculoPorPar())
                .thenReturn(List.<Object[]>of(new Object[]{ra1, semestre, ultimoCalculo},
                        new Object[]{ra2, semestre, ultimoCalculo}));

        job.recalcularSemestresEnCurso();

        verify(nivelLogroService).recalcular(ra1, semestre, RecalculoProgramadoNivelLogro.SISTEMA);
        verify(nivelLogroService).recalcular(ra2, semestre, RecalculoProgramadoNivelLogro.SISTEMA);
        assertThat(RecalculoProgramadoNivelLogro.SISTEMA.rol()).isEqualTo(RolUsuario.ADMINISTRADOR);
    }

    @Test
    @DisplayName("Un error en un (RA, semestre) no detiene el recálculo de los demás")
    void errorNoDetieneElResto() {
        UUID raConError = UUID.randomUUID();
        UUID raBien = UUID.randomUUID();
        UUID semestre = UUID.randomUUID();
        LocalDateTime ultimoCalculo = LocalDateTime.of(2024, 1, 5, 8, 0);
        LocalDateTime ultimaCalificacion = LocalDateTime.of(2024, 1, 10, 8, 0);
        when(repository.buscarParesConEvaluacionesEnSemestreEnCurso(any()))
                .thenReturn(List.<Object[]>of(new Object[]{raConError, semestre}, new Object[]{raBien, semestre}));
        when(repository.buscarUltimaCalificacionQueCuentaPorParEnSemestreEnCurso(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{raConError, semestre, ultimaCalificacion},
                        new Object[]{raBien, semestre, ultimaCalificacion}));
        when(nivelLogroEstudianteRepository.buscarUltimoCalculoPorPar())
                .thenReturn(List.<Object[]>of(new Object[]{raConError, semestre, ultimoCalculo},
                        new Object[]{raBien, semestre, ultimoCalculo}));
        when(nivelLogroService.recalcular(eq(raConError), eq(semestre), any()))
                .thenThrow(new IllegalArgumentException("dato corrupto"));

        job.recalcularSemestresEnCurso();

        verify(nivelLogroService).recalcular(raBien, semestre, RecalculoProgramadoNivelLogro.SISTEMA);
    }

    @Test
    @DisplayName("Sin semestres en curso con evaluaciones no recalcula nada")
    void sinPares() {
        when(repository.buscarParesConEvaluacionesEnSemestreEnCurso(any())).thenReturn(List.of());

        job.recalcularSemestresEnCurso();

        verify(nivelLogroService, never()).recalcular(any(), any(), any());
    }

    @Test
    @DisplayName("Recalcula un par cuya calificación es posterior al último cálculo")
    void recalculaParConCalificacionMasNuevaQueElUltimoCalculo() {
        UUID ra = UUID.randomUUID();
        UUID semestre = UUID.randomUUID();
        LocalDateTime ultimoCalculo = LocalDateTime.now().minusDays(2);
        LocalDateTime ultimaCalificacion = LocalDateTime.now().minusDays(1);
        when(repository.buscarParesConEvaluacionesEnSemestreEnCurso(any()))
                .thenReturn(List.<Object[]>of(new Object[]{ra, semestre}));
        when(repository.buscarUltimaCalificacionQueCuentaPorParEnSemestreEnCurso(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{ra, semestre, ultimaCalificacion}));
        when(nivelLogroEstudianteRepository.buscarUltimoCalculoPorPar())
                .thenReturn(List.<Object[]>of(new Object[]{ra, semestre, ultimoCalculo}));

        job.recalcularSemestresEnCurso();

        verify(nivelLogroService).recalcular(ra, semestre, RecalculoProgramadoNivelLogro.SISTEMA);
    }

    @Test
    @DisplayName("Recalcula un par con calificaciones y sin cálculo previo")
    void recalculaParSinCalculoPrevio() {
        UUID ra = UUID.randomUUID();
        UUID semestre = UUID.randomUUID();
        when(repository.buscarParesConEvaluacionesEnSemestreEnCurso(any()))
                .thenReturn(List.<Object[]>of(new Object[]{ra, semestre}));
        when(repository.buscarUltimaCalificacionQueCuentaPorParEnSemestreEnCurso(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{ra, semestre, LocalDateTime.now()}));
        when(nivelLogroEstudianteRepository.buscarUltimoCalculoPorPar()).thenReturn(List.of());

        job.recalcularSemestresEnCurso();

        verify(nivelLogroService).recalcular(ra, semestre, RecalculoProgramadoNivelLogro.SISTEMA);
    }

    @Test
    @DisplayName("No recalcula un par cuyo último cálculo es posterior a su última calificación")
    void noRecalculaParSinCambios() {
        UUID ra = UUID.randomUUID();
        UUID semestre = UUID.randomUUID();
        LocalDateTime ultimaCalificacion = LocalDateTime.now().minusDays(2);
        LocalDateTime ultimoCalculo = LocalDateTime.now().minusDays(1);
        when(repository.buscarParesConEvaluacionesEnSemestreEnCurso(any()))
                .thenReturn(List.<Object[]>of(new Object[]{ra, semestre}));
        when(repository.buscarUltimaCalificacionQueCuentaPorParEnSemestreEnCurso(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{ra, semestre, ultimaCalificacion}));
        when(nivelLogroEstudianteRepository.buscarUltimoCalculoPorPar())
                .thenReturn(List.<Object[]>of(new Object[]{ra, semestre, ultimoCalculo}));

        job.recalcularSemestresEnCurso();

        verify(nivelLogroService, never()).recalcular(eq(ra), eq(semestre), any());
    }

    @Test
    @DisplayName("No recalcula un par sin calificaciones")
    void noRecalculaParSinCalificaciones() {
        UUID ra = UUID.randomUUID();
        UUID semestre = UUID.randomUUID();
        when(repository.buscarParesConEvaluacionesEnSemestreEnCurso(any()))
                .thenReturn(List.<Object[]>of(new Object[]{ra, semestre}));
        when(repository.buscarUltimaCalificacionQueCuentaPorParEnSemestreEnCurso(any(), any(), any()))
                .thenReturn(List.of());
        when(nivelLogroEstudianteRepository.buscarUltimoCalculoPorPar()).thenReturn(List.of());

        job.recalcularSemestresEnCurso();

        verify(nivelLogroService, never()).recalcular(eq(ra), eq(semestre), any());
    }

    @Test
    @DisplayName("En la ejecución número 12 recalcula todos los pares aunque ninguno haya cambiado")
    void recalculaTodoEnElBarridoCompleto() {
        UUID ra = UUID.randomUUID();
        UUID semestre = UUID.randomUUID();
        when(repository.buscarParesConEvaluacionesEnSemestreEnCurso(any()))
                .thenReturn(List.<Object[]>of(new Object[]{ra, semestre}));
        when(repository.buscarUltimaCalificacionQueCuentaPorParEnSemestreEnCurso(any(), any(), any()))
                .thenReturn(List.of());
        when(nivelLogroEstudianteRepository.buscarUltimoCalculoPorPar()).thenReturn(List.of());

        for (int ejecucion = 1; ejecucion <= 12; ejecucion++) {
            job.recalcularSemestresEnCurso();
        }

        verify(nivelLogroService, times(1)).recalcular(ra, semestre, RecalculoProgramadoNivelLogro.SISTEMA);
    }
}
