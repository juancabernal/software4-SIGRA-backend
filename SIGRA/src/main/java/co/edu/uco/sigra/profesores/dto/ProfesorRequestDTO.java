package co.edu.uco.sigra.profesores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ProfesorRequestDTO(
        @NotNull UUID tipoDocumentoId,

        @NotBlank
        @Pattern(regexp = "\\d{6,10}", message = "El número de documento debe tener entre 6 y 10 dígitos numéricos")
        String numeroDocumento,

        @NotBlank
        String nombreCompleto,

        // Mismo criterio que LoginRequestDTO: el dominio no distingue mayúsculas; el servicio normaliza el correo.
        @NotBlank
        @Pattern(
                regexp = "^\\s*[\\w.-]+@uco\\.net\\.co\\s*$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "El correo debe tener formato usuario@uco.net.co"
        )
        String correoInstitucional
) {}
