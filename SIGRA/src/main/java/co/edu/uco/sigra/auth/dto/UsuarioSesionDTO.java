package co.edu.uco.sigra.auth.dto;

import co.edu.uco.sigra.common.enums.RolUsuario;

import java.util.UUID;

public record UsuarioSesionDTO(
        UUID id,
        String nombreCompleto,
        String correoInstitucional,
        RolUsuario rol
) {
}
