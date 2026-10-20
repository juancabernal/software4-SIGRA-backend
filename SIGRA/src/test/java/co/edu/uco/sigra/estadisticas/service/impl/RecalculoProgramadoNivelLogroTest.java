package co.edu.uco.sigra.estadisticas.service.impl;

import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.repository.CalificacionesNivelLogroRepository;
import co.edu.uco.sigra.estadisticas.service.NivelLogroService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Recálculo programado del nivel de logro")
class RecalculoProgramadoNivelLogroTest {

    @Mock
    private CalificacionesNivelLogroRepository repository;

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
        when(repository.buscarParesConEvaluacionesEnSemestreEnCurso(LocalDate.now(ZoneId.of("America/Bogota"))))
                .thenReturn(List.of(new Object[]{ra1, semestre}, new Object[]{ra2, semestre}));

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
        when(repository.buscarParesConEvaluacionesEnSemestreEnCurso(any()))
                .thenReturn(List.of(new Object[]{raConError, semestre}, new Object[]{raBien, semestre}));
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
}
