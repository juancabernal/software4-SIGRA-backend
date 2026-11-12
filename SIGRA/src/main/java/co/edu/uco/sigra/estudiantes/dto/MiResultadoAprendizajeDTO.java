package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MiResultadoAprendizajeDTO(
        @Schema(description = "Código del resultado de aprendizaje")
        String codigo,
        @Schema(description = "Descripción del resultado de aprendizaje")
        String descripcion,
        @Schema(description = "Nivel del último cálculo del sistema")
        MiNivelLogro nivelLogro,
        @Schema(description = "Promedio ponderado; null si no hay nivel calculado")
        Double promedio,
        @Schema(description = "Cantidad de evaluaciones con calificación")
        int evaluacionesCalificadas,
        @Schema(description = "Cantidad total de evaluaciones visibles")
        int evaluacionesTotales,
        @Schema(description = "Evaluaciones ordenadas por fecha y nombre")
        List<MiEvaluacionDTO> evaluaciones
) {
}
