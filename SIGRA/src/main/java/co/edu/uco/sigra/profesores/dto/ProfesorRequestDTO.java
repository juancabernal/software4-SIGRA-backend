package co.edu.uco.sigra.profesores.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

@Schema(name = "ProfesorRequest", description = "Datos para registrar o modificar un profesor")
public record ProfesorRequestDTO(
        @Schema(description = "Identificador (UUID) del tipo de documento del profesor",
                example = "6b1f9d7e-5c3a-4f21-9e8b-2d4a7c6e1f05")
        @NotNull UUID tipoDocumentoId,

        @Schema(description = "Número de documento: solo dígitos, de 6 a 10. No se puede cambiar una vez registrado",
                example = "1098765432")
        @NotBlank
        @Pattern(regexp = "\\d{6,10}", message = "El número de documento debe tener entre 6 y 10 dígitos numéricos")
        String numeroDocumento,

        @Schema(description = "Nombre completo del profesor", example = "Ana Gómez")
        @NotBlank
        String nombreCompleto,

        // Mismo criterio que LoginRequestDTO: el dominio no distingue mayúsculas; el servicio normaliza el correo.
        @Schema(description = "Correo institucional del dominio @uco.net.co. Se guarda en minúsculas y sin espacios laterales",
                example = "ana.gomez@uco.net.co")
        @NotBlank
        @Pattern(
                regexp = "^\\s*[\\w.-]+@uco\\.net\\.co\\s*$",
                flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "El correo debe tener formato usuario@uco.net.co"
        )
        String correoInstitucional
) {}