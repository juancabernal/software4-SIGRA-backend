package co.edu.uco.sigra.mejoras.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Datos para convertir una acción de mejora en evaluación (RF-18). */
public record ConvertirEvaluacionRequestDTO(

        @NotNull(message = "El porcentaje es obligatorio")
        @Positive(message = "El porcentaje debe ser mayor que 0")
        @DecimalMax(value = "100.0", message = "El porcentaje no puede superar 100")
        Double porcentaje
) {
}
