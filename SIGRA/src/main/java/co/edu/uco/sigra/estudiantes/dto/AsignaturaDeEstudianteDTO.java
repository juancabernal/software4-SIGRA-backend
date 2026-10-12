package co.edu.uco.sigra.estudiantes.dto;

import java.util.UUID;

public record AsignaturaDeEstudianteDTO(
        UUID matriculaId,
        String asignaturaNombre,
        String semestreCodigo,
        String estado
) {
}
