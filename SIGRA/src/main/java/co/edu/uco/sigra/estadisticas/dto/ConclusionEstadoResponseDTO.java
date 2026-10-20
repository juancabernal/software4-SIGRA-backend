package co.edu.uco.sigra.estadisticas.dto;

/**
 * Respuesta de solo lectura de {@code consultarEstado} (RF-16): el estado actual del (RA, semestre)
 * y, cuando aplica, la conclusión asociada. {@code totalEvaluados} y {@code porcentajeInsuficiente}
 * permiten que el frontend explique el estado sin recalcular nada.
 */
public record ConclusionEstadoResponseDTO(
        EstadoConsultaConclusion estado,
        ConclusionResponseDTO conclusion,
        Integer totalEvaluados,
        Double porcentajeInsuficiente
) {
}
