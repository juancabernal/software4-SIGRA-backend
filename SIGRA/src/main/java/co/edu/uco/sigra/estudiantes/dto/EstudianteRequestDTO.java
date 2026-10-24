package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EstudianteRequestDTO(
        @Schema(description = "Identificador (UUID) del tipo de documento", example = "6b1f9d7e-5c3a-4f21-9e8b-2d4a7c6e1f05")
        @NotNull(message = "El tipo de documento es obligatorio")
        UUID tipoDocumentoId,

        @Schema(description = "Número de documento, entre 6 y 10 dígitos numéricos", example = "1098765432")
        @NotBlank(message = "El número de documento es obligatorio")
        @Pattern(regexp = "\\d{6,10}", message = "El número de documento debe tener entre 6 y 10 dígitos numéricos")
        String numeroDocumento,

        @Schema(description = "Nombre completo del estudiante", example = "Mariana Gómez Restrepo")
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(min = 3, max = 150, message = "El nombre completo debe tener entre 3 y 150 caracteres")
        @Pattern(
                regexp = "^\\p{L}+(?:\\s\\p{L}+)*$",
                message = "El nombre completo solo puede contener letras y un único espacio entre cada palabra"
        )
        String nombreCompleto,

        @Schema(description = "Correo institucional, dominio @uco.net.co", example = "mariana.gomez@uco.net.co")
        @NotBlank(message = "El correo institucional es obligatorio")
        @Pattern(
                regexp = "^\\s*[\\w.-]+@uco\\.net\\.co\\s*$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "El correo debe tener formato usuario@uco.net.co"
        )
        String correoInstitucional
) {
}
