package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

public record MiMateriaDTO(
        @Schema(description = "Identificador de la matrícula para consultar su detalle")
        UUID matriculaId,
        @Schema(example = "MAT-301")
        String asignaturaCodigo,
        @Schema(example = "Cálculo I")
        String asignaturaNombre,
        @Schema(description = "Nombre del programa académico")
        String programaNombre,
        @Schema(example = "2026-2")
        String semestreCodigo,
        LocalDate fechaMatricula,
        SituacionMateria situacion
) {
}
