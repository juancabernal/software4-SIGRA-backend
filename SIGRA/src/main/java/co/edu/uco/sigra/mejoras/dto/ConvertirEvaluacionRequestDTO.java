package co.edu.uco.sigra.mejoras.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

import static co.edu.uco.sigra.evaluaciones.dto.ReglasEntradaEvaluacion.FECHA_OBLIGATORIA;
import static co.edu.uco.sigra.evaluaciones.dto.ReglasEntradaEvaluacion.PORCENTAJE_DECIMALES;
import static co.edu.uco.sigra.evaluaciones.dto.ReglasEntradaEvaluacion.PORCENTAJE_MAX;
import static co.edu.uco.sigra.evaluaciones.dto.ReglasEntradaEvaluacion.PORCENTAJE_MIN;
import static co.edu.uco.sigra.evaluaciones.dto.ReglasEntradaEvaluacion.PORCENTAJE_OBLIGATORIO;
import static co.edu.uco.sigra.evaluaciones.dto.ReglasEntradaEvaluacion.PORCENTAJE_RANGO;

/**
 * Datos para convertir una acción de mejora en evaluación (RF-18): el Profesor indica la nueva fecha
 * tentativa y el porcentaje. El nombre y la descripción se heredan de la acción de mejora. Las reglas
 * de formato son las mismas que las de crear una evaluación (RF-11).
 */
@Schema(name = "ConvertirEvaluacionRequest", description = "Fecha tentativa y porcentaje de la evaluación que resulta de convertir la acción de mejora")
public record ConvertirEvaluacionRequestDTO(

        @Schema(description = "Fecha tentativa de la evaluación; debe estar dentro del semestre activo", example = "2026-05-15")
        @NotNull(message = FECHA_OBLIGATORIA)
        LocalDate fechaTentativa,

        @Schema(description = "Porcentaje que aporta la evaluación a la nota final (mayor que 0 y máximo 100, hasta 2 decimales)",
                example = "20.0")
        @NotNull(message = PORCENTAJE_OBLIGATORIO)
        @DecimalMin(value = PORCENTAJE_MIN, inclusive = false, message = PORCENTAJE_RANGO)
        @DecimalMax(value = PORCENTAJE_MAX, message = PORCENTAJE_RANGO)
        @Digits(integer = 3, fraction = 2, message = PORCENTAJE_DECIMALES)
        BigDecimal porcentaje
) {
}
