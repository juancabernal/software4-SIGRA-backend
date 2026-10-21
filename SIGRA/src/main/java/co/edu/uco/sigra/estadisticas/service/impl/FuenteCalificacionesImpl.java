package co.edu.uco.sigra.estadisticas.service.impl;

import co.edu.uco.sigra.estadisticas.repository.CalificacionesNivelLogroRepository;
import co.edu.uco.sigra.estadisticas.service.CalificacionPonderada;
import co.edu.uco.sigra.estadisticas.service.FuenteCalificaciones;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Implementación real de {@link FuenteCalificaciones} sobre las calificaciones registradas (RF-12) en las
 * evaluaciones (RF-10) de un (RA, semestre). Reemplaza a la implementación de prueba que leía
 * {@code registro_calificacion_prueba}.
 * <p>
 * Solo cuentan las evaluaciones ACTIVA (una INACTIVA no cuenta aunque tenga notas) y las matrículas ACTIVA
 * (las DESVINCULADA no cuentan). La nota ({@code NUMERIC(2,1)}) y el porcentaje ({@code NUMERIC(5,2)}) se
 * convierten a double con {@link BigDecimal#doubleValue()}: con esa precisión la conversión es exacta para
 * el cálculo, y el redondeo a 2 decimales lo sigue haciendo {@code NivelLogroServiceImpl} sobre el promedio.
 */
@Service
@RequiredArgsConstructor
public class FuenteCalificacionesImpl implements FuenteCalificaciones {

    private final CalificacionesNivelLogroRepository calificacionesNivelLogroRepository;

    @Override
    public List<CalificacionPonderada> listarCalificaciones(UUID resultadoAprendizajeId, UUID semestreId) {
        return calificacionesNivelLogroRepository
                .buscarCalificacionesQueCuentan(resultadoAprendizajeId, semestreId,
                        EstadoEvaluacion.ACTIVA, EstadoMatricula.ACTIVA)
                .stream()
                .map(FuenteCalificacionesImpl::aCalificacionPonderada)
                .toList();
    }

    static CalificacionPonderada aCalificacionPonderada(Object[] fila) {
        UUID estudianteId = (UUID) fila[0];
        BigDecimal nota = (BigDecimal) fila[1];
        BigDecimal porcentaje = (BigDecimal) fila[2];
        return new CalificacionPonderada(estudianteId, nota.doubleValue(), porcentaje.doubleValue());
    }
}
