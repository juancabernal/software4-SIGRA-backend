package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

public record EstudianteMatriculadoDTO(
        @Schema(description = "Identificador (UUID) de la matrícula")
        UUID matriculaId,

        @Schema(description = "Número de documento del estudiante", example = "1098765432")
        String numeroDocumento,

        @Schema(description = "Nombre completo del estudiante", example = "Mariana Gómez Restrepo")
        String nombreCompleto,

        @Schema(description = "Fecha en que se registró la matrícula", example = "2026-08-10")
        LocalDate fechaMatricula,

        @Schema(description = "Estado de la matrícula: ACTIVA o DESVINCULADA", example = "ACTIVA")
        String estado
) {
}
