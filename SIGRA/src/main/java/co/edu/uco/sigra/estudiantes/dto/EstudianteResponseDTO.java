package co.edu.uco.sigra.estudiantes.dto;

import java.util.UUID;

public record EstudianteResponseDTO(
        UUID id,
        String tipoDocumentoNombre,
        String numeroDocumento,
        String nombreCompleto,
        String correoInstitucional,
        String estado
) {
}
