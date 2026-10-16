package co.edu.uco.sigra.calificaciones.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

import static co.edu.uco.sigra.calificaciones.dto.ReglasEntradaCalificacion.*;

public record CalificacionRequestDTO(
        @NotNull(message = MATRICULA_OBLIGATORIA)
        UUID matriculaId,

        @NotNull(message = NOTA_OBLIGATORIA)
        @DecimalMin(value = NOTA_MINIMA, message = NOTA_RANGO)
        @DecimalMax(value = NOTA_MAXIMA, message = NOTA_RANGO)
        @Digits(integer = 1, fraction = 1, message = NOTA_DECIMALES)
        BigDecimal nota,

        @NotBlank(message = OBSERVACION_OBLIGATORIA)
        @Size(min = OBSERVACION_MIN, max = OBSERVACION_MAX, message = OBSERVACION_LONGITUD)
        @Pattern(regexp = PATRON_OBSERVACION, message = OBSERVACION_CARACTERES)
        String observacion
) {
    public CalificacionRequestDTO {
        observacion = normalizarObservacion(observacion);
    }
}