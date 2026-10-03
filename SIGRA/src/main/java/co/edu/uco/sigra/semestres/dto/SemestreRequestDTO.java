package co.edu.uco.sigra.semestres.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record SemestreRequestDTO(
        @NotBlank
        @Pattern(regexp = "^\\d{4}-[12]$", message = "El código debe tener el formato AAAA-1 o AAAA-2")
        String codigo,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin
) {}
