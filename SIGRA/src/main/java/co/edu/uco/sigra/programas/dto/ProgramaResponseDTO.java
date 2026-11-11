package co.edu.uco.sigra.programas.dto;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "ProgramaResponse", description = "Programa académico registrado")
public record ProgramaResponseDTO(
        @Schema(description = "Identificador (UUID) del programa", example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID id,

        @Schema(description = "Código del programa, inmutable una vez creado", example = "ING-SIS")
        String codigo,

        @Schema(description = "Nombre del programa", example = "Ingeniería de Sistemas")
        String nombre,

        @Schema(description = "Identificador (UUID) de la facultad", example = "7c8d9e0f-1a2b-4c3d-8e4f-5a6b7c8d9e0f")
        UUID facultadId,

        @Schema(description = "Nombre de la facultad", example = "Facultad de Ingeniería")
        String facultadNombre,

        @Schema(description = "Estado del registro", example = "ACTIVO")
        EstadoRegistro estado
) {}