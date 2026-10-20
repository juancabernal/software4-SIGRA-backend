package co.edu.uco.sigra.resultadosaprendizaje.dto;

import java.util.List;
import java.util.UUID;

/**
 * Rendimiento de los resultados de aprendizaje ACTIVOS de una asignatura en el semestre activo.
 * Si no hay semestre activo, {@code semestreId} y {@code semestreCodigo} son null y la lista va vacía.
 */
public record RendimientoAsignaturaResponseDTO(
        UUID asignaturaId,
        UUID semestreId,
        String semestreCodigo,
        List<RendimientoResultadoAprendizajeResponseDTO> resultados
) {
}
