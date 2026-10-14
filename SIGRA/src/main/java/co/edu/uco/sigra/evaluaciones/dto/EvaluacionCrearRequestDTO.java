package co.edu.uco.sigra.evaluaciones.dto;

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
public record EvaluacionCrearRequestDTO(

        @NotBlank(message = NOMBRE_OBLIGATORIO)
        @Size(max = NOMBRE_MAX, message = NOMBRE_LONGITUD)
        @Pattern(regexp = PATRON_TEXTO, message = NOMBRE_CARACTERES)
        String nombre,

        @NotNull(message = FECHA_OBLIGATORIA)
        LocalDate fechaTentativa,

        @NotNull(message = PORCENTAJE_OBLIGATORIO)
        @DecimalMin(value = PORCENTAJE_MIN, inclusive = false, message = PORCENTAJE_RANGO)
        @DecimalMax(value = PORCENTAJE_MAX, message = PORCENTAJE_RANGO)
        @Digits(integer = 3, fraction = 2, message = PORCENTAJE_DECIMALES)
        BigDecimal porcentaje,

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
