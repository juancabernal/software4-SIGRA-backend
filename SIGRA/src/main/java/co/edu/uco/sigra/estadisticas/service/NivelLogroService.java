package co.edu.uco.sigra.estadisticas.service;

import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.estadisticas.dto.EstadisticasResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RangoNivelLogroResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RecalculoEstadisticasResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Nivel de logro y estadísticas descriptivas por RA (RF-14 / RF-15).
 */
public interface NivelLogroService {

    /**
     * Recalcula el nivel de logro de cada estudiante del (RA, semestre) a partir de sus calificaciones
     * (promedio ponderado normalizado por el porcentaje de cada evaluación, mínimo 3 calificaciones) y,
     * al terminar, dispara la conclusión automática (RF-16).
     * <p>
     * Punto de entrada para el módulo de calificaciones: debe invocarlo cada vez que se registre o se
     * actualice una calificación de ese RA y semestre (recálculo automático). Es idempotente.
     */
    RecalculoEstadisticasResponseDTO recalcular(UUID resultadoAprendizajeId, UUID semestreId);

    /** Igual que {@link #recalcular(UUID, UUID)}, aplicando el alcance del PROFESOR (sus asignaturas). */
    RecalculoEstadisticasResponseDTO recalcular(UUID resultadoAprendizajeId, UUID semestreId,
                                                IdentidadActual identidad);

    /** Estadísticas del último recálculo; con un PROFESOR, solo de sus asignaturas con asignación activa. */
    EstadisticasResultadoAprendizajeResponseDTO consultarEstadisticas(UUID resultadoAprendizajeId, UUID semestreId,
                                                                      IdentidadActual identidad);

    /** El catálogo de los 4 rangos de nivel de logro. */
    List<RangoNivelLogroResponseDTO> consultarCatalogo();
}
