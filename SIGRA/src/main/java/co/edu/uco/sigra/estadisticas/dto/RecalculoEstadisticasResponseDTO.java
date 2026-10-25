package co.edu.uco.sigra.estadisticas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de recalcular un (RA, semestre): las estadísticas actualizadas y lo que respondió la
 * generación de la conclusión automática (RF-16), que se dispara al final de cada recálculo.
 */
@Schema(name = "RecalculoEstadisticasResponse",
        description = "Estadísticas recalculadas y resultado de la conclusión automática (RF-16) que dispara el recálculo")
public record RecalculoEstadisticasResponseDTO(
        @Schema(description = "Estadísticas del RA en el semestre después del recálculo")
        EstadisticasResultadoAprendizajeResponseDTO estadisticas,

        @Schema(description = "Resultado de intentar generar la conclusión automática")
        GenerarConclusionResponseDTO conclusion
) {
}
