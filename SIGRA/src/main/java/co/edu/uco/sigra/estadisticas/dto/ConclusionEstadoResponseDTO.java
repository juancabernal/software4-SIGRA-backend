package co.edu.uco.sigra.estadisticas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de solo lectura de {@code consultarEstado} (RF-16): el estado actual del (RA, semestre)
 * y, cuando aplica, la conclusión asociada. {@code totalEvaluados} y {@code porcentajeInsuficiente}
 * permiten que el frontend explique el estado sin recalcular nada.
 */
@Schema(name = "ConclusionEstadoResponse", description = "Estado observable de un resultado de aprendizaje en un semestre, sin generar ni mutar nada")
public record ConclusionEstadoResponseDTO(
        @Schema(description = "Estado observado", allowableValues = {"VIGENTE", "ATENDIDA", "SIN_DATOS_SUFICIENTES", "SATISFACTORIO"},
                example = "VIGENTE")
        EstadoConsultaConclusion estado,

        @Schema(description = "Conclusión asociada; presente solo si el estado es VIGENTE o ATENDIDA")
        ConclusionResponseDTO conclusion,

        @Schema(description = "Cantidad de estudiantes evaluados; null cuando el estado es VIGENTE o ATENDIDA", example = "20")
        Integer totalEvaluados,

        @Schema(description = "Porcentaje en nivel Bajo o Medio; null cuando el estado es VIGENTE, ATENDIDA o SIN_DATOS_SUFICIENTES",
                example = "25.0")
        Double porcentajeInsuficiente
) {
}
