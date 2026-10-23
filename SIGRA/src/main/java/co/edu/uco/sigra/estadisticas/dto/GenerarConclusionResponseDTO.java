package co.edu.uco.sigra.estadisticas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de intentar generar una conclusión. {@code generada=false} no es un error: puede ser
 * datos insuficientes, desempeño dentro del umbral o una conclusión Vigente que ya existía.
 */
@Schema(name = "GenerarConclusionResponse", description = "Resultado de intentar generar una conclusión; generada=false no es un error")
public record GenerarConclusionResponseDTO(
        @Schema(description = "Si se generó una conclusión nueva", example = "true")
        boolean generada,

        @Schema(description = "Motivo del resultado: datos insuficientes, dentro del umbral, ya vigente o generada",
                example = "Se generó una nueva conclusión.")
        String motivo,

        @Schema(description = "Conclusión generada; null si generada=false")
        ConclusionResponseDTO conclusion
) {
}
