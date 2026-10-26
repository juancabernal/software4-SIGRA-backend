package co.edu.uco.sigra.estadisticas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Estadísticas descriptivas de un RA en un semestre (RF-14 / RF-15).
 *
 * @param totalEstudiantes    matriculados activos en la asignatura del RA más quienes conservan una fila de
 *                            nivel con calificaciones (las de matrículas desvinculadas ya no cuentan)
 * @param evaluados           con nivel de logro calculado (3 o más calificaciones)
 * @param sinDatosSuficientes con 1 o 2 calificaciones: «sin datos suficientes para calcular»
 * @param sinEvaluar          sin ninguna calificación en el RA: «Sin evaluar»
 * @param promedioNota        promedio de los promedios ponderados de los evaluados (0.0 – 5.0, dos
 *                            decimales); null si no hay evaluados
 * @param promedioPorcentaje  el mismo promedio en porcentaje (nota / 5 × 100); null si no hay evaluados
 * @param distribucion        siempre los 4 niveles en orden (Bajo, Medio, Alto, Sobresaliente); el
 *                            porcentaje es sobre los evaluados
 * @param fechaCalculo        fecha del último recálculo; null si nunca se ha recalculado
 */
@Schema(name = "EstadisticasResultadoAprendizajeResponse",
        description = "Estadísticas descriptivas de un RA en un semestre (RF-14, RF-15)")
public record EstadisticasResultadoAprendizajeResponseDTO(
        @Schema(description = "Identificador (UUID) del resultado de aprendizaje", example = "1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d")
        UUID resultadoAprendizajeId,

        @Schema(description = "Código del resultado de aprendizaje", example = "RA-01")
        String codigoResultadoAprendizaje,

        @Schema(description = "Identificador (UUID) de la asignatura del RA", example = "3c4d5e6f-7a8b-4c9d-8e0f-1a2b3c4d5e6f")
        UUID asignaturaId,

        @Schema(description = "Identificador (UUID) del semestre", example = "6b1f9d7e-5c3a-4f21-9e8b-2d4a7c6e1f05")
        UUID semestreId,

        @Schema(description = "Código del semestre", example = "2027-1")
        String codigoSemestre,

        @Schema(description = "Matriculados activos en la asignatura más quienes conservan un nivel con calificaciones",
                example = "25")
        long totalEstudiantes,

        @Schema(description = "Estudiantes con nivel de logro calculado (3 o más calificaciones)", example = "20")
        long evaluados,

        @Schema(description = "Estudiantes con 1 o 2 calificaciones: sin datos suficientes para calcular", example = "3")
        long sinDatosSuficientes,

        @Schema(description = "Estudiantes sin ninguna calificación en el RA", example = "2")
        long sinEvaluar,

        @Schema(description = "Promedio de los promedios ponderados de los evaluados (0.0 – 5.0, dos decimales); null si no hay evaluados",
                example = "3.85", nullable = true)
        Double promedioNota,

        @Schema(description = "El mismo promedio en porcentaje (nota / 5 × 100); null si no hay evaluados",
                example = "77.0", nullable = true)
        Double promedioPorcentaje,

        @Schema(description = "Siempre los 4 niveles en orden (BAJO, MEDIO, ALTO, SOBRESALIENTE); el porcentaje es sobre los evaluados")
        List<DistribucionNivelLogroDTO> distribucion,

        @Schema(description = "Fecha y hora del último recálculo; null si nunca se ha recalculado",
                example = "2026-10-14T12:00:00", nullable = true)
        LocalDateTime fechaCalculo
) {
}
