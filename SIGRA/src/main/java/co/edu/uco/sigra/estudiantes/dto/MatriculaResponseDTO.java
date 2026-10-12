package co.edu.uco.sigra.estudiantes.dto;

import java.util.UUID;

public record MatriculaResponseDTO(
        UUID id,
        UUID estudianteId,
        UUID asignaturaId,
        UUID semestreId,
        String estado
) {
}
