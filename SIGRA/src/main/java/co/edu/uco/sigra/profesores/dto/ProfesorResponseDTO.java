package co.edu.uco.sigra.profesores.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "ProfesorResponse", description = "Profesor registrado")
public record ProfesorResponseDTO(
        @Schema(description = "Identificador (UUID) del profesor", example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID id,

        @Schema(description = "Nombre del tipo de documento", example = "Cédula de Ciudadanía")
        String tipoDocumentoNombre,

        @Schema(description = "Número de documento", example = "1098765432")
        String numeroDocumento,

        @Schema(description = "Nombre completo", example = "Ana Gómez")
        String nombreCompleto,

        @Schema(description = "Correo institucional, en minúsculas", example = "ana.gomez@uco.net.co")
        String correoInstitucional,

        @Schema(description = "Estado del registro", allowableValues = {"ACTIVO", "INACTIVO"}, example = "ACTIVO")
        String estado,

        @Schema(description = "Verdadero si el profesor tiene al menos una asignación docente activa; en ese caso no se puede inactivar",
                example = "false")
        boolean tieneAsignacionesActivas
) {
}