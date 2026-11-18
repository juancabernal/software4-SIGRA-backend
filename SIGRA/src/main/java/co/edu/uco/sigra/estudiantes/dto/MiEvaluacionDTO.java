package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MiEvaluacionDTO(
        @Schema(description = "Nombre de la evaluación")
        String nombre,
        @Schema(description = "Descripción de la evaluación")
        String descripcion,
        @Schema(description = "Fecha tentativa de la evaluación")
        LocalDate fecha,
        @Schema(description = "Porcentaje de la evaluación")
        BigDecimal porcentaje,
        @Schema(description = "Nota; null si no existe calificación")
        BigDecimal nota,
        @Schema(description = "Observación; null si no existe calificación")
        String observacion
) {
}
