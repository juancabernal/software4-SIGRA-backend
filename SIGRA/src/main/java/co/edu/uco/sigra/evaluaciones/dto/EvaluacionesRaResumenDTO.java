package co.edu.uco.sigra.evaluaciones.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Vista del profesor: evaluaciones de un RA en un semestre con el porcentaje acumulado de las
 * ACTIVAS y el porcentaje que queda disponible hasta 100.
 */
public record EvaluacionesRaResumenDTO(
        UUID resultadoAprendizajeId,
        UUID semestreId,
        String semestreCodigo,
        BigDecimal porcentajeAsignado,
        BigDecimal porcentajeDisponible,
        List<EvaluacionResponseDTO> evaluaciones
) {
}
