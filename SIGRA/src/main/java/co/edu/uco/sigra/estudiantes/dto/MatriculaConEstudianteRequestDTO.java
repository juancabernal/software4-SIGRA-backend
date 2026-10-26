package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Datos de entrada del alta de un estudiante y su matrícula en un solo paso (RF-09a).
 * <p>
 * Anida {@link EstudianteRequestDTO} en lugar de repetir sus campos: al marcarlo
 * {@code @Valid}, sus propias anotaciones {@code @NotBlank}/{@code @Pattern} se aplican en
 * cascada sin duplicarlas aquí.
 */
public record MatriculaConEstudianteRequestDTO(
        @Schema(description = "Datos del estudiante a registrar")
        @NotNull(message = "Los datos del estudiante son obligatorios")
        @Valid
        EstudianteRequestDTO estudiante,

        @Schema(description = "Identificador (UUID) de la asignatura")
        @NotNull(message = "La asignatura es obligatoria")
        UUID asignaturaId,

        @Schema(description = "Identificador (UUID) del semestre")
        @NotNull(message = "El semestre es obligatorio")
        UUID semestreId
) {
}
