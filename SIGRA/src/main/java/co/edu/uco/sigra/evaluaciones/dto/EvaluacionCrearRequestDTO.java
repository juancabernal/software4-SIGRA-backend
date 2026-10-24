package co.edu.uco.sigra.evaluaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

import static co.edu.uco.sigra.evaluaciones.dto.ReglasEntradaEvaluacion.*;

/**
 * Datos de entrada para crear una evaluación (RF-10a / RF-11).
 * El RA llega en la URL y el semestre lo asigna el sistema (el semestre activo).
 * El nombre y la descripción se normalizan aquí, antes de validarse (ver {@link ReglasEntradaEvaluacion}).
 */
@Schema(name = "EvaluacionCrearRequest", description = "Datos para crear una evaluación de un RA en el semestre activo")
public record EvaluacionCrearRequestDTO(

        @Schema(description = "Nombre de la evaluación (máximo 30 caracteres). Único por RA y semestre, sin distinguir mayúsculas", example = "Parcial 1")
        @NotBlank(message = NOMBRE_OBLIGATORIO)
        @Size(max = NOMBRE_MAX, message = NOMBRE_LONGITUD)
        @Pattern(regexp = PATRON_TEXTO, message = NOMBRE_CARACTERES)
        String nombre,

        @Schema(description = "Fecha tentativa: posterior a hoy y dentro del semestre activo (AAAA-MM-DD)", example = "2026-11-20")
        @NotNull(message = FECHA_OBLIGATORIA)
        LocalDate fechaTentativa,

        @Schema(description = "Porcentaje de la evaluación: mayor que 0 y máximo 100, hasta 2 decimales. La suma de las activas del RA no puede superar 100", example = "30")
        @NotNull(message = PORCENTAJE_OBLIGATORIO)
        @DecimalMin(value = PORCENTAJE_MIN, inclusive = false, message = PORCENTAJE_RANGO)
        @DecimalMax(value = PORCENTAJE_MAX, message = PORCENTAJE_RANGO)
        @Digits(integer = 3, fraction = 2, message = PORCENTAJE_DECIMALES)
        BigDecimal porcentaje,

        @Schema(description = "Descripción de la evaluación (máximo 100 caracteres, sin < ni >)", example = "Examen escrito sobre los temas del primer corte")
        @NotBlank(message = DESCRIPCION_OBLIGATORIA)
        @Size(max = DESCRIPCION_MAX, message = DESCRIPCION_LONGITUD)
        @Pattern(regexp = PATRON_TEXTO, message = DESCRIPCION_CARACTERES)
        String descripcion
) {
    public EvaluacionCrearRequestDTO {
        nombre = normalizarTexto(nombre);
        descripcion = normalizarTexto(descripcion);
    }
}
