package co.edu.uco.sigra.mejoras.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "AccionMejoraResponse", description = "Acción de mejora registrada sobre una conclusión (RF-17), con el estado de su conversión en evaluación (RF-18)")
public record AccionMejoraResponseDTO(
        @Schema(description = "Identificador (UUID) de la acción de mejora", example = "8b9c0d1e-2f3a-4b4c-5d6e-7f8a9b0c1d2e")
        UUID id,

        @Schema(description = "Nombre de la acción de mejora", example = "Taller de refuerzo de derivadas")
        String nombre,

        @Schema(description = "Descripción de la acción de mejora", example = "Sesiones de práctica dirigida sobre reglas de derivación")
        String descripcion,

        @Schema(description = "Fecha y hora en que se registró la acción de mejora")
        LocalDateTime fechaPropuesta,

        @Schema(description = "Identificador (UUID) del resultado de aprendizaje", example = "6f7a8b9c-0d1e-4f2a-3b4c-5d6e7f8a9b0c")
        UUID resultadoAprendizajeId,

        @Schema(description = "Código del resultado de aprendizaje", example = "RA-05")
        String resultadoAprendizajeCodigo,

        @Schema(description = "Identificador (UUID) de la asignatura del resultado de aprendizaje", example = "3c4d5e6f-7a8b-4c9d-0e1f-2a3b4c5d6e7f")
        UUID asignaturaId,

        @Schema(description = "Identificador (UUID) de la conclusión que originó la acción de mejora", example = "5e6f7a8b-9c0d-4e1f-2a3b-4c5d6e7f8a9b")
        UUID conclusionOrigenId,

        @Schema(description = "Identificador (UUID) del semestre de la conclusión origen", example = "7a8b9c0d-1e2f-4a3b-4c5d-6e7f8a9b0c1d")
        UUID semestreId,

        @Schema(description = "Código del semestre de la conclusión origen", example = "2026-1")
        String semestreCodigo,

        @Schema(description = "Identificador (UUID) de la evaluación generada al convertir; null si aún no se ha convertido",
                example = "9c0d1e2f-3a4b-4c5d-6e7f-8a9b0c1d2e3f")
        UUID evaluacionGeneradaId,

        @Schema(description = "Si la acción de mejora ya fue convertida en evaluación", example = "false")
        boolean convertida
) {
}
