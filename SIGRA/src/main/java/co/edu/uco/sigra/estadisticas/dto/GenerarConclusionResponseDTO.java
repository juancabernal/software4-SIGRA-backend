package co.edu.uco.sigra.estadisticas.dto;

/**
 * Resultado de intentar generar una conclusión. {@code generada=false} no es un error: puede ser
 * datos insuficientes, desempeño dentro del umbral o una conclusión Vigente que ya existía.
 */
public record GenerarConclusionResponseDTO(
        boolean generada,
        String motivo,
        ConclusionResponseDTO conclusion
) {
}
