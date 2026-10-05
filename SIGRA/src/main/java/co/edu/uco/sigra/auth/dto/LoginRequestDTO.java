package co.edu.uco.sigra.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// El dominio se valida sin distinguir mayúsculas y se admiten espacios laterales;
// la normalización definitiva (trim + minúsculas) la hace AuthServiceImpl.
public record LoginRequestDTO(
        @NotBlank(message = "El correo institucional es obligatorio")
        @Pattern(
                regexp = "^\\s*[\\w.-]+@uco\\.net\\.co\\s*$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "El correo debe tener formato usuario@uco.net.co"
        )
        String correoInstitucional,

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena
) {
}
