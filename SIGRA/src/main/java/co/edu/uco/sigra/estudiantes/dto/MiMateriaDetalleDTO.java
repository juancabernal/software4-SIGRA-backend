package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MiMateriaDetalleDTO(
        @Schema(description = "Identificador de la matrícula")
        UUID matriculaId,
        @Schema(description = "Código de la asignatura")
        String asignaturaCodigo,
        @Schema(description = "Nombre de la asignatura")
        String asignaturaNombre,
        @Schema(description = "Nombre del programa académico")
        String programaNombre,
        @Schema(description = "Código del semestre")
        String semestreCodigo,
        @Schema(description = "Fecha de matrícula")
        LocalDate fechaMatricula,
        @Schema(description = "Situación de la matrícula")
        SituacionMateria situacion,
        @Schema(description = "Resultados de aprendizaje ordenados por código")
        List<MiResultadoAprendizajeDTO> resultadosAprendizaje
) {
}
