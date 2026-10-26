package co.edu.uco.sigra.resultadosaprendizaje.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "RendimientoResultadoAprendizaje", description = "Rendimiento de un RA activo en un semestre")
public record RendimientoResultadoAprendizajeResponseDTO(
        @Schema(description = "Identificador del RA", example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID resultadoAprendizajeId,
        @Schema(description = "Código del RA", example = "RA-01")
        String codigo,
        @Schema(description = "SIN_EVALUAR (sin calificaciones), DATOS_INSUFICIENTES (hay notas pero ningún estudiante llega a 3) o CON_NIVEL")
        EstadoRendimiento estado,
        @Schema(description = "Nivel de logro del RA (BAJO, MEDIO, ALTO o SOBRESALIENTE). Nulo si el estado no es CON_NIVEL", example = "ALTO", nullable = true)
        NivelLogro nivelLogro,
        @Schema(description = "Promedio de los promedios ponderados de los estudiantes con nivel calculado, con 2 decimales. Nulo si el estado no es CON_NIVEL", example = "4.05", nullable = true)
        Double promedioNota,
        @Schema(description = "Suma de los porcentajes de las evaluaciones activas del RA en el semestre", example = "100")
        BigDecimal porcentajeAsignado,
        @Schema(description = "Suma de los porcentajes de las evaluaciones activas calificadas a todos los matriculados activos", example = "60")
        BigDecimal porcentajeEvaluado,
        @Schema(description = "Cantidad de evaluaciones activas del RA en el semestre", example = "3")
        int evaluacionesActivas,
        @Schema(description = "Mínimo de evaluaciones activas para considerar el rendimiento completo", example = "3")
        int minimoEvaluaciones,
        @Schema(description = "true si hay al menos el mínimo de evaluaciones activas, suman 100% y todas están calificadas a todos los matriculados", example = "false")
        boolean completo
) {
}
