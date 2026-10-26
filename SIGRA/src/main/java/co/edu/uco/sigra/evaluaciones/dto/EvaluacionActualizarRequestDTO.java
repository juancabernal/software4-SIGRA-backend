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
 * Datos de entrada para modificar una evaluación (RF-10b). Se envían todos los campos; si la evaluación ya
 * tiene calificaciones, la fecha tentativa y el porcentaje deben llegar iguales (el servicio lo valida).
 * El nombre y la descripción se normalizan aquí, antes de validarse (ver {@link ReglasEntradaEvaluacion}).
 */
@Schema(name = "EvaluacionActualizarRequest", description = "Datos para modificar una evaluación. Se envían todos los campos")
public record EvaluacionActualizarRequestDTO(

        @Schema(description = "Nombre de la evaluación (máximo 30 caracteres). Único por RA y semestre, sin distinguir mayúsculas", example = "Parcial 1")
        @NotBlank(message = NOMBRE_OBLIGATORIO)
        @Size(max = NOMBRE_MAX, message = NOMBRE_LONGITUD)
        @Pattern(regexp = PATRON_TEXTO, message = NOMBRE_CARACTERES)
        String nombre,

        @Schema(description = "Fecha tentativa (AAAA-MM-DD). Si la evaluación ya tiene calificaciones debe llegar igual; si cambia, debe ser posterior a hoy y dentro de su semestre", example = "2026-11-20")
        @NotNull(message = FECHA_OBLIGATORIA)
        LocalDate fechaTentativa,

        @Schema(description = "Porcentaje de la evaluación (si ya tiene calificaciones debe llegar igual): mayor que 0 y máximo 100, hasta 2 decimales. La suma de las activas del RA no puede superar 100", example = "30")
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
    public EvaluacionActualizarRequestDTO {
        nombre = normalizarTexto(nombre);
        descripcion = normalizarTexto(descripcion);
    }
}
