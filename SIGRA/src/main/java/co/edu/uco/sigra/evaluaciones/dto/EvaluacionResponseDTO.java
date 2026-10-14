package co.edu.uco.sigra.evaluaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos de salida de una evaluación. {@code tieneCalificaciones} le indica al frontend si la fecha
 * tentativa y el porcentaje están bloqueados (RF-10b).
 */
public record EvaluacionResponseDTO(
        UUID id,
        UUID resultadoAprendizajeId,
        UUID semestreId,
        String semestreCodigo,
        String nombre,
        LocalDate fechaTentativa,
        BigDecimal porcentaje,
        String descripcion,
        String estado,
        boolean tieneCalificaciones
) {
}
