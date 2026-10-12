package co.edu.uco.sigra.semestres.dto;

import java.time.LocalDate;
import java.util.UUID;

public record SemestreResponseDTO(
        UUID id,
        String codigo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        String estado
) {}
