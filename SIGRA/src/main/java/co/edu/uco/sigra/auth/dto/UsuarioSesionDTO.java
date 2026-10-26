package co.edu.uco.sigra.auth.dto;

import co.edu.uco.sigra.common.enums.RolUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "UsuarioSesion", description = "Datos públicos del usuario autenticado")
public record UsuarioSesionDTO(
        @Schema(description = "Identificador único del usuario.", example = "4b64b64b-4b64-4b64-8b64-4b64b64b64b6",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id,
        @Schema(description = "Nombre completo del usuario.", example = "Administradora de Prueba",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String nombreCompleto,
        @Schema(description = "Correo institucional del usuario.", example = "administrador.prueba@uco.net.co",
                format = "email", requiredMode = Schema.RequiredMode.REQUIRED)
        String correoInstitucional,
        @Schema(description = "Rol con el que se autenticó el usuario.", example = "ADMINISTRADOR",
                allowableValues = {"ADMINISTRADOR", "PROFESOR", "ESTUDIANTE"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        RolUsuario rol
) {
}
