package co.edu.uco.sigra.estudiantes.dto;

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
        @NotNull(message = "Los datos del estudiante son obligatorios")
        @Valid
        EstudianteRequestDTO estudiante,

        @NotNull(message = "La asignatura es obligatoria")
        UUID asignaturaId,

        @NotNull(message = "El semestre es obligatorio")
        UUID semestreId
) {
}
