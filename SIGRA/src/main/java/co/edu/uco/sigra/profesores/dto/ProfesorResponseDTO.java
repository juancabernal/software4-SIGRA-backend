package co.edu.uco.sigra.profesores.dto;

import java.util.UUID;

public record ProfesorResponseDTO(
        UUID id,
        String tipoDocumentoNombre,
        String numeroDocumento,
        String nombreCompleto,
        String correoInstitucional,
        String estado,
        boolean tieneAsignacionesActivas
) {
}
