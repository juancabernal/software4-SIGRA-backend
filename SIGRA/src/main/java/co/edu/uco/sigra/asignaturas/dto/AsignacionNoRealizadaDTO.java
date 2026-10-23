package co.edu.uco.sigra.asignaturas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Un par profesor-asignatura que no se asignó, con el motivo (RF-07): ya estaba asignado o alguno de los dos está inactivo. */
@Schema(name = "AsignacionNoRealizada", description = "Par profesor-asignatura que se omitió o se rechazó, con el motivo")
public record AsignacionNoRealizadaDTO(
        @Schema(description = "Identificador (UUID) del profesor del par",
                example = "b3d4e5f6-7a8b-49c0-91d2-3e4f5a6b7c8d")
        UUID profesorId,

        @Schema(description = "Identificador (UUID) de la asignatura del par",
                example = "c4e5f6a7-8b9c-4d01-a2e3-4f5a6b7c8d9e")
        UUID asignaturaId,

        @Schema(description = "Motivo por el que el par no se asignó",
                example = "El profesor ya está asignado a esta asignatura")
        String motivo
) {}
