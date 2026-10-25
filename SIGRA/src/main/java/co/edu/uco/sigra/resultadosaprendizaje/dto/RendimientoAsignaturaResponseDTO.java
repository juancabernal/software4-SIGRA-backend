package co.edu.uco.sigra.resultadosaprendizaje.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Rendimiento de los resultados de aprendizaje ACTIVOS de una asignatura en el semestre activo.
 * Si no hay semestre activo, {@code semestreId} y {@code semestreCodigo} son null y la lista va vacía.
 */
@Schema(name = "RendimientoAsignatura", description = "Rendimiento de los RA activos de una asignatura en un semestre")
public record RendimientoAsignaturaResponseDTO(
        @Schema(description = "Identificador de la asignatura", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID asignaturaId,
        @Schema(description = "Semestre sobre el que se calculó. Nulo si no hay semestre activo y no se pidió otro", example = "9d8c7b6a-5f4e-4d3c-b2a1-0f9e8d7c6b5a", nullable = true)
        UUID semestreId,
        @Schema(description = "Código del semestre. Nulo si no hay semestre", example = "2026-2", nullable = true)
        String semestreCodigo,
        @Schema(description = "Rendimiento de cada RA activo, ordenado por código. Vacío si no hay semestre")
        List<RendimientoResultadoAprendizajeResponseDTO> resultados
) {
}
