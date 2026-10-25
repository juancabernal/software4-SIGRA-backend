package co.edu.uco.sigra.estadisticas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "ConclusionResponse", description = "Conclusión automática generada para un resultado de aprendizaje en un semestre (RF-16)")
public record ConclusionResponseDTO(
        @Schema(description = "Identificador (UUID) de la conclusión", example = "5e6f7a8b-9c0d-4e1f-2a3b-4c5d6e7f8a9b")
        UUID id,

        @Schema(description = "Identificador (UUID) del resultado de aprendizaje", example = "6f7a8b9c-0d1e-4f2a-3b4c-5d6e7f8a9b0c")
        UUID resultadoAprendizajeId,

        @Schema(description = "Código del resultado de aprendizaje", example = "RA-05")
        String resultadoAprendizajeCodigo,

        @Schema(description = "Identificador (UUID) de la asignatura del resultado de aprendizaje", example = "3c4d5e6f-7a8b-4c9d-0e1f-2a3b4c5d6e7f")
        UUID asignaturaId,

        @Schema(description = "Identificador (UUID) del semestre", example = "7a8b9c0d-1e2f-4a3b-4c5d-6e7f8a9b0c1d")
        UUID semestreId,

        @Schema(description = "Código del semestre", example = "2026-1")
        String semestreCodigo,

        @Schema(description = "Mensaje de la conclusión", example = "Nivel de logro insuficiente en RA-05: se recomienda registrar una acción de mejora")
        String mensaje,

        @Schema(description = "Si el resultado de aprendizaje tenía datos suficientes (mínimo 3 calificaciones) al generarse", example = "true")
        boolean tieneDatosSuficientes,

        @Schema(description = "Fecha y hora en que se generó la conclusión")
        LocalDateTime fechaGeneracion,

        @Schema(description = "Porcentaje de estudiantes en nivel Bajo o Medio que originó la conclusión", example = "55.0")
        double porcentajeInsuficiente,

        @Schema(description = "Estado de la conclusión", allowableValues = {"VIGENTE", "ATENDIDA"}, example = "VIGENTE")
        String estado,

        @Schema(description = "Cantidad de estudiantes evaluados al momento de generarse", example = "20")
        Integer totalEvaluados,

        @Schema(description = "Cantidad de estudiantes en nivel Bajo", example = "8")
        Integer enNivelBajo,

        @Schema(description = "Cantidad de estudiantes en nivel Medio", example = "6")
        Integer enNivelMedio,

        @Schema(description = "Umbral de porcentaje insuficiente usado para generar la conclusión", example = "40.0")
        Double umbralPorcentaje,

        @Schema(description = "Porcentaje insuficiente del semestre anterior con conclusión, si existe", example = "48.0")
        Double porcentajeSemestreAnterior,

        @Schema(description = "Código del semestre anterior con conclusión, si existe", example = "2025-2")
        String semestreAnteriorCodigo,

        @Schema(description = "Recomendación calculada a partir de los niveles Bajo y Medio; null en conclusiones sin esos contadores",
                example = "El problema es de fundamentos: se recomienda reforzarlos antes de avanzar.")
        String recomendacion
) {
}
