package co.edu.uco.sigra.evaluaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Vista del profesor: evaluaciones de un RA en un semestre con el porcentaje acumulado de las
 * ACTIVAS y el porcentaje que queda disponible hasta 100.
 */
@Schema(name = "EvaluacionesRaResumen", description = "Evaluaciones de un RA en un semestre con el porcentaje asignado y el disponible")
public record EvaluacionesRaResumenDTO(
        @Schema(description = "Identificador del RA", example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID resultadoAprendizajeId,
        @Schema(description = "Identificador del semestre consultado", example = "9d8c7b6a-5f4e-4d3c-b2a1-0f9e8d7c6b5a")
        UUID semestreId,
        @Schema(description = "Código del semestre consultado", example = "2026-2")
        String semestreCodigo,
        @Schema(description = "Suma de los porcentajes de las evaluaciones ACTIVAS", example = "60")
        BigDecimal porcentajeAsignado,
        @Schema(description = "Porcentaje que queda disponible hasta 100", example = "40")
        BigDecimal porcentajeDisponible,
        @Schema(description = "Evaluaciones del RA en el semestre, activas e inactivas")
        List<EvaluacionResponseDTO> evaluaciones
) {
}
