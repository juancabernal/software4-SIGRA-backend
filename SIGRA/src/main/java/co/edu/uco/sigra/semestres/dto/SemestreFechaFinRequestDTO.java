package co.edu.uco.sigra.semestres.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Única modificación permitida sobre un semestre: extender su fecha de fin. */
@Schema(name = "SemestreFechaFinRequest", description = "Nueva fecha de fin para extender un semestre")
public record SemestreFechaFinRequestDTO(
        @Schema(description = "Nueva fecha de fin (AAAA-MM-DD); debe ser posterior a la fecha de fin actual",
                example = "2027-06-30")
        @NotNull(message = "La nueva fecha de fin es obligatoria")
        LocalDate fechaFin
) {}
