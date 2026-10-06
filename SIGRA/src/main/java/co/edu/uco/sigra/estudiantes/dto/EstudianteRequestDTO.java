package co.edu.uco.sigra.estudiantes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record EstudianteRequestDTO(
        @NotNull(message = "El tipo de documento es obligatorio")
        UUID tipoDocumentoId,

        @NotBlank(message = "El número de documento es obligatorio")
        @Pattern(regexp = "\\d{6,10}", message = "El número de documento debe tener entre 6 y 10 dígitos numéricos")
        String numeroDocumento,

        @NotBlank(message = "El nombre completo es obligatorio")
        String nombreCompleto,

        @NotBlank(message = "El correo institucional es obligatorio")
        @Pattern(
                regexp = "^\\s*[\\w.-]+@uco\\.net\\.co\\s*$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "El correo debe tener formato usuario@uco.net.co"
        )
        String correoInstitucional
) {
}
