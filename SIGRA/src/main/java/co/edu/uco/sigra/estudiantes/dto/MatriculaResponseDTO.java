package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

public record MatriculaResponseDTO(
        @Schema(description = "Identificador (UUID) de la matrícula")
        UUID id,

        @Schema(description = "Identificador (UUID) del estudiante")
        UUID estudianteId,

        @Schema(description = "Identificador (UUID) de la asignatura")
        UUID asignaturaId,

        @Schema(description = "Identificador (UUID) del semestre")
        UUID semestreId,

        @Schema(description = "Fecha en que se registró la matrícula", example = "2026-08-10")
        LocalDate fechaMatricula,

        @Schema(description = "Estado de la matrícula: ACTIVA o DESVINCULADA", example = "ACTIVA")
        String estado
) {
}
