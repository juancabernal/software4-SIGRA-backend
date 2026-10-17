package co.edu.uco.sigra.profesores.dto;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "AsignaturaProfesorResponse", description = "Asignación docente de un profesor con los datos de la asignatura")
public record AsignaturaProfesorResponseDTO (
        @Schema(description = "Identificador (UUID) de la asignación docente", example = "2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e")
        UUID asignacionId,

        @Schema(description = "Identificador (UUID) de la asignatura", example = "3c4d5e6f-7a8b-4c9d-0e1f-2a3b4c5d6e7f")
        UUID asignaturaId,

        @Schema(description = "Código de la asignatura", example = "MAT-101")
        String codigoAsignatura,

        @Schema(description = "Nombre de la asignatura", example = "Cálculo Diferencial")
        String nombreAsignatura,

        @Schema(description = "Estado de la asignación docente (ACTIVO o INACTIVO)", example = "ACTIVO")
        EstadoRegistro estadoAsignatura
){}