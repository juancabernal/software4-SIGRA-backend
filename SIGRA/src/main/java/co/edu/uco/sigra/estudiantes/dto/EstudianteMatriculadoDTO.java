package co.edu.uco.sigra.estudiantes.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EstudianteMatriculadoDTO(
        UUID matriculaId,
        String numeroDocumento,
        String nombreCompleto,
        LocalDate fechaMatricula,
        String estado
) {
}
