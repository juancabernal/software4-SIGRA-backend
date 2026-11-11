package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record EstudianteResponseDTO(
        @Schema(description = "Identificador (UUID) del estudiante")
        UUID id,

        @Schema(description = "Nombre del tipo de documento", example = "Cédula de ciudadanía")
        String tipoDocumentoNombre,

        @Schema(description = "Número de documento", example = "1098765432")
        String numeroDocumento,

        @Schema(description = "Primer nombre", example = "Mariana")
        String primerNombre,

        @Schema(description = "Segundo nombre opcional", example = "Lucía")
        String segundoNombre,

        @Schema(description = "Primer apellido", example = "Gómez")
        String primerApellido,

        @Schema(description = "Segundo apellido", example = "Restrepo")
        String segundoApellido,

        @Schema(description = "Nombre completo derivado para listados", example = "Mariana Lucía Gómez Restrepo")
        String nombreCompleto,

        @Schema(description = "Correo institucional", example = "mariana.gomez@uco.net.co")
        String correoInstitucional,

        @Schema(description = "Estado del registro", example = "ACTIVO")
        String estado
) {
}
