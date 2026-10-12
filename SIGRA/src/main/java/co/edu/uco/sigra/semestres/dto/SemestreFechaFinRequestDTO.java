package co.edu.uco.sigra.semestres.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Única modificación permitida sobre un semestre: extender su fecha de fin. */
public record SemestreFechaFinRequestDTO(
        @NotNull(message = "La nueva fecha de fin es obligatoria")
        LocalDate fechaFin
) {}
