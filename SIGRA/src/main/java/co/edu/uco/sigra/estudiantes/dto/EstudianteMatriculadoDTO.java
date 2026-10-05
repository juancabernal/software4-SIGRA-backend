package co.edu.uco.sigra.estudiantes.dto;

import java.util.UUID;

public record EstudianteMatriculadoDTO(
        UUID matriculaId,
        String numeroDocumento,
        String nombreCompleto,
        String estado
) {
}
