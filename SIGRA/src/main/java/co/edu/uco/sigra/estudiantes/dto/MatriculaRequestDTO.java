package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MatriculaRequestDTO(
        @Schema(description = "Identificador (UUID) del estudiante a matricular")
        @NotNull(message = "El estudiante es obligatorio")
        UUID estudianteId,

        @Schema(description = "Identificador (UUID) de la asignatura")
        @NotNull(message = "La asignatura es obligatoria")
        UUID asignaturaId,

        @Schema(description = "Identificador (UUID) del semestre")
        @NotNull(message = "El semestre es obligatorio")
        UUID semestreId
) {
}
