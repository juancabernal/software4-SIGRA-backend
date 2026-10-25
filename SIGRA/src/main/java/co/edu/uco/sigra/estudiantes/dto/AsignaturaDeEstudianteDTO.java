package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

public record AsignaturaDeEstudianteDTO(
        @Schema(description = "Identificador (UUID) de la matrícula")
        UUID matriculaId,

        @Schema(description = "Nombre de la asignatura", example = "Software 4")
        String asignaturaNombre,

        @Schema(description = "Código del semestre", example = "2026-2")
        String semestreCodigo,

        @Schema(description = "Fecha en que se registró la matrícula", example = "2026-08-10")
        LocalDate fechaMatricula,

        @Schema(description = "Estado de la matrícula: ACTIVA o DESVINCULADA", example = "ACTIVA")
        String estado
) {
}
