package co.edu.uco.sigra.asignaturas.dto;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "AsignacionDocenteResponse", description = "Asignación docente creada o reactivada")
public record AsignacionDocenteResponseDTO(
        @Schema(description = "Identificador (UUID) de la asignación docente",
                example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID id,

        @Schema(description = "Identificador (UUID) del profesor asignado",
                example = "b3d4e5f6-7a8b-49c0-91d2-3e4f5a6b7c8d")
        UUID profesorId,

        @Schema(description = "Nombre completo del profesor", example = "Ana Gómez")
        String nombreProfesor,

        @Schema(description = "Identificador (UUID) de la asignatura asignada",
                example = "c4e5f6a7-8b9c-4d01-a2e3-4f5a6b7c8d9e")
        UUID asignaturaId,

        @Schema(description = "Código de la asignatura", example = "MAT-101")
        String codigoAsignatura,

        @Schema(description = "Nombre de la asignatura", example = "Cálculo")
        String nombreAsignatura,

        @Schema(description = "Estado del registro", allowableValues = {"ACTIVO", "INACTIVO"}, example = "ACTIVO")
        EstadoRegistro estado,

        @Schema(description = "Fecha en que se creó o reactivó la asignación", example = "2026-10-14")
        LocalDate fechaAsignacion
) {}
