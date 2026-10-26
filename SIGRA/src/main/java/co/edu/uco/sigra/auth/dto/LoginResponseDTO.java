package co.edu.uco.sigra.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "Resultado de una autenticación exitosa")
public record LoginResponseDTO(
        @Schema(description = "JWT firmado emitido por el backend para acceder a endpoints protegidos.",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0YjY0YjY0Yi00YjY0LTRiNjQtOGI2NC00YjY0YjY0YjY0YiJ9.firma-ficticia",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String token,
        @Schema(description = "Esquema de autenticación que debe acompañar al JWT.", example = "Bearer",
                allowableValues = {"Bearer"}, requiredMode = Schema.RequiredMode.REQUIRED)
        String tipo,
        @Schema(description = "Tiempo de vigencia del JWT en segundos.", example = "3600", minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long expiraEn,
        @Schema(description = "Información pública del usuario autenticado, sin contraseña ni hash de contraseña.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UsuarioSesionDTO usuario
) {
}
