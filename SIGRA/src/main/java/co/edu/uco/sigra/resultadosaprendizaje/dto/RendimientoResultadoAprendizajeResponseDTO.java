package co.edu.uco.sigra.resultadosaprendizaje.dto;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Rendimiento de un resultado de aprendizaje en el semestre activo.
 *
 * @param nivelLogro          solo con estado CON_NIVEL; null en otro caso
 * @param promedioNota        promedio (0.0 - 5.0) de los promedios ponderados de los estudiantes; null si no hay nivel
 * @param porcentajeAsignado  suma de los porcentajes de las evaluaciones ACTIVAS del RA
 * @param porcentajeEvaluado  suma de los porcentajes de las evaluaciones ACTIVAS ya calificadas a todos los matriculados
 * @param evaluacionesActivas cantidad de evaluaciones ACTIVAS del RA en el semestre
 * @param minimoEvaluaciones  evaluaciones activas que exige RF-15 (3 calificaciones por estudiante) para que haya nivel
 * @param completo            true si asignado y evaluado suman 100 y hay al menos {@code minimoEvaluaciones}
 */
public record RendimientoResultadoAprendizajeResponseDTO(
        UUID resultadoAprendizajeId,
        String codigo,
        EstadoRendimiento estado,
        NivelLogro nivelLogro,
        Double promedioNota,
        BigDecimal porcentajeAsignado,
        BigDecimal porcentajeEvaluado,
        int evaluacionesActivas,
        int minimoEvaluaciones,
        boolean completo
) {
}
