package co.edu.uco.sigra.estudiantes.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MatriculaRequestDTO(
        @NotNull(message = "El estudiante es obligatorio")
        UUID estudianteId,

        @NotNull(message = "La asignatura es obligatoria")
        UUID asignaturaId,

        @NotNull(message = "El semestre es obligatorio")
        UUID semestreId
) {
}
