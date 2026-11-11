package co.edu.uco.sigra.facultades.dto;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "FacultadResponse", description = "Facultad registrada")
public record FacultadResponseDTO(
        @Schema(description = "Identificador (UUID) de la facultad", example = "7c8d9e0f-1a2b-4c3d-8e4f-5a6b7c8d9e0f")
        UUID id,

        @Schema(description = "Nombre de la facultad", example = "Facultad de Ingeniería")
        String nombre,

        @Schema(description = "Estado del registro", example = "ACTIVO")
        EstadoRegistro estado
) {}
