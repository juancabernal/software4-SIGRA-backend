package co.edu.uco.sigra.estadisticas.dto;

/**
 * Resultado de recalcular un (RA, semestre): las estadísticas actualizadas y lo que respondió la
 * generación de la conclusión automática (RF-16), que se dispara al final de cada recálculo.
 */
public record RecalculoEstadisticasResponseDTO(
        EstadisticasResultadoAprendizajeResponseDTO estadisticas,
        GenerarConclusionResponseDTO conclusion
) {
}
