package co.edu.uco.sigra.estadisticas.service;

import co.edu.uco.sigra.estadisticas.repository.CalificacionesNivelLogroRepository;
import co.edu.uco.sigra.estadisticas.service.impl.FuenteCalificacionesImpl;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Fuente real de calificaciones del nivel de logro. Las reglas de filtro (evaluación ACTIVA, matrícula ACTIVA,
 * RA y semestre pedidos) viven en la consulta JPQL y se verifican contra PostgreSQL en NivelLogroIntegracionIT
 * (IE-06); aquí se prueba que la fuente las pide y que convierte bien los tipos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Fuente real de calificaciones del nivel de logro (RF-14 / RF-15)")
class FuenteCalificacionesImplTest {

    private static final UUID RA = UUID.randomUUID();
    private static final UUID SEMESTRE = UUID.randomUUID();

    @Mock
    private CalificacionesNivelLogroRepository repository;

    @InjectMocks
    private FuenteCalificacionesImpl fuente;

    @Test
    @DisplayName("Pide solo evaluaciones ACTIVA y matrículas ACTIVA del RA y semestre")
    void pideSoloActivas() {
        when(repository.buscarCalificacionesQueCuentan(any(), any(), any(), any())).thenReturn(List.of());

        fuente.listarCalificaciones(RA, SEMESTRE);

        verify(repository).buscarCalificacionesQueCuentan(eq(RA), eq(SEMESTRE), eq(EstadoEvaluacion.ACTIVA),
                eq(EstadoMatricula.ACTIVA));
    }

    @Test
    @DisplayName("Convierte nota NUMERIC(2,1) y porcentaje NUMERIC(5,2) de BigDecimal a double sin perder precisión")
    void convierteTipos() {
        UUID estudiante = UUID.randomUUID();
        when(repository.buscarCalificacionesQueCuentan(eq(RA), eq(SEMESTRE), any(), any())).thenReturn(List.of(
                new Object[]{estudiante, new BigDecimal("4.5"), new BigDecimal("33.33")},
                new Object[]{estudiante, new BigDecimal("0.0"), new BigDecimal("100.00")}));

        List<CalificacionPonderada> calificaciones = fuente.listarCalificaciones(RA, SEMESTRE);

        assertThat(calificaciones).containsExactly(
                new CalificacionPonderada(estudiante, 4.5, 33.33),
                new CalificacionPonderada(estudiante, 0.0, 100.0));
    }

    @Test
    @DisplayName("Sin calificaciones devuelve una lista vacía")
    void sinCalificaciones() {
        when(repository.buscarCalificacionesQueCuentan(any(), any(), any(), any())).thenReturn(List.of());

        assertThat(fuente.listarCalificaciones(RA, SEMESTRE)).isEmpty();
    }

    @Test
    @DisplayName("Una nota fuera de 0–5 en la base no se convierte en un nivel: se rechaza")
    void notaCorruptaSeRechaza() {
        when(repository.buscarCalificacionesQueCuentan(any(), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{UUID.randomUUID(), new BigDecimal("7.0"), new BigDecimal("10")}));

        assertThatThrownBy(() -> fuente.listarCalificaciones(RA, SEMESTRE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
