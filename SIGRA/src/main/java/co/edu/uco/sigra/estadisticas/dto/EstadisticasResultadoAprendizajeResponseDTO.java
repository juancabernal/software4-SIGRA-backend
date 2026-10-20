package co.edu.uco.sigra.estadisticas.dto;

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
public record EstadisticasResultadoAprendizajeResponseDTO(
        UUID resultadoAprendizajeId,
        String codigoResultadoAprendizaje,
        UUID asignaturaId,
        UUID semestreId,
        String codigoSemestre,
        long totalEstudiantes,
        long evaluados,
        long sinDatosSuficientes,
        long sinEvaluar,
        Double promedioNota,
        Double promedioPorcentaje,
        List<DistribucionNivelLogroDTO> distribucion,
        LocalDateTime fechaCalculo
) {
}
