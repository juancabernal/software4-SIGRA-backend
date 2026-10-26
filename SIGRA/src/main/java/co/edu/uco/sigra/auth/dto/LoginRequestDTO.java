package co.edu.uco.sigra.auth.dto;

import co.edu.uco.sigra.common.validation.Utf8ByteSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

// El dominio se valida sin distinguir mayúsculas y se admiten espacios laterales;
// la normalización definitiva (trim + minúsculas) la hace AuthServiceImpl.
@Schema(name = "LoginRequest", description = "Credenciales institucionales para iniciar sesión")
public record LoginRequestDTO(
        @NotBlank(message = "El correo institucional es obligatorio")
        @Pattern(
                regexp = "^\\s*[\\w.-]+@uco\\.net\\.co\\s*$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "El correo debe tener formato usuario@uco.net.co"
        )
        @Size(max = 254, message = "El correo institucional no puede superar 254 caracteres")
        @Schema(description = "Correo institucional terminado en @uco.net.co. Se aceptan mayúsculas y espacios laterales; el backend lo normaliza.",
                example = "administrador.prueba@uco.net.co", format = "email", maxLength = 254,
                requiredMode = Schema.RequiredMode.REQUIRED)
        String correoInstitucional,

        @NotBlank(message = "La contraseña es obligatoria")
        @Utf8ByteSize(max = 72, message = "La contraseña no puede superar 72 bytes UTF-8")
        @Schema(description = "Contraseña del usuario. Obligatoria y de máximo 72 bytes UTF-8; el valor nunca se devuelve ni se registra.",
                example = "ClaveDePrueba2026!", format = "password",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String contrasena
) {
}
