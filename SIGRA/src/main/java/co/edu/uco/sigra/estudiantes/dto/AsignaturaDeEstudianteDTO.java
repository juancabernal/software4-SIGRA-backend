package co.edu.uco.sigra.estudiantes.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AsignaturaDeEstudianteDTO(
        UUID matriculaId,
        String asignaturaNombre,
        String semestreCodigo,
        LocalDate fechaMatricula,
        String estado
) {
}
