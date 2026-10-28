package co.edu.uco.sigra.semestres.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

@Schema(name = "SemestreRequest", description = "Datos para registrar un semestre")
public record SemestreRequestDTO(
        @Schema(description = "Código único con formato AAAA-1 o AAAA-2; el año debe coincidir con el de fechaInicio",
                example = "2027-1")
        @NotBlank
        @Pattern(regexp = "^\\d{4}-[12]$", message = "El código debe tener el formato AAAA-1 o AAAA-2")
        String codigo,

        @Schema(description = "Fecha de inicio (AAAA-MM-DD); debe ser hoy o una fecha futura", example = "2027-01-25")
        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio,

        @Schema(description = "Fecha de fin (AAAA-MM-DD); debe ser posterior a la fecha de inicio", example = "2027-06-05")
        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate fechaFin
) {}
