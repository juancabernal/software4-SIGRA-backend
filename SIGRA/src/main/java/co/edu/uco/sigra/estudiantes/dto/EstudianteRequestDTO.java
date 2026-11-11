package co.edu.uco.sigra.estudiantes.dto;

import co.edu.uco.sigra.common.util.NombresPersona;
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

        @Schema(description = "Primer nombre (una sola palabra, sin espacios)", example = "Mariana")
        @NotBlank(message = "El primer nombre es obligatorio")
        @Size(min = 1, max = 50, message = "El primer nombre debe tener entre 1 y 50 caracteres")
        @Pattern(regexp = NombresPersona.PATRON_PARTE, message = NombresPersona.MENSAJE_PARTE)
        String primerNombre,

        @Schema(description = "Segundo nombre opcional (una sola palabra, sin espacios)", example = "Lucía")
        @Size(max = 50, message = "El segundo nombre debe tener máximo 50 caracteres")
        @Pattern(regexp = NombresPersona.PATRON_PARTE, message = NombresPersona.MENSAJE_PARTE)
        String segundoNombre,

        @Schema(description = "Primer apellido (una sola palabra, sin espacios)", example = "Gómez")
        @NotBlank(message = "El primer apellido es obligatorio")
        @Size(min = 1, max = 50, message = "El primer apellido debe tener entre 1 y 50 caracteres")
        @Pattern(regexp = NombresPersona.PATRON_PARTE, message = NombresPersona.MENSAJE_PARTE)
        String primerApellido,

        @Schema(description = "Segundo apellido (una sola palabra, sin espacios)", example = "Restrepo")
        @NotBlank(message = "El segundo apellido es obligatorio")
        @Size(min = 1, max = 50, message = "El segundo apellido debe tener entre 1 y 50 caracteres")
        @Pattern(regexp = NombresPersona.PATRON_PARTE, message = NombresPersona.MENSAJE_PARTE)
        String segundoApellido,

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
