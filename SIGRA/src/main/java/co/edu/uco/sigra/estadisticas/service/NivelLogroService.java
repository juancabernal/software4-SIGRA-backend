package co.edu.uco.sigra.estadisticas.service;

import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.estadisticas.dto.EstadisticasResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RangoNivelLogroResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RecalculoEstadisticasResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Nivel de logro y estadísticas descriptivas por RA (RF-14 / RF-15).
 * <p>
 * Igual que {@link ConclusionService}, las operaciones protegidas reciben la identidad autenticada
 * ({@link UsuarioPrincipal}) y fallan cerrado sin ella: el ADMINISTRADOR opera globalmente y el
 * PROFESOR solo sobre RA de asignaturas con asignación docente ACTIVA.
 */
public interface NivelLogroService {

    /**
     * Recalcula el nivel de logro de cada estudiante del (RA, semestre) a partir de sus calificaciones
     * (promedio ponderado normalizado por el porcentaje de cada evaluación, mínimo 3 calificaciones) y,
     * al terminar, dispara la conclusión automática (RF-16) con la misma identidad.
     * <p>
     * Punto de entrada para el módulo de calificaciones: debe invocarlo cada vez que se registre o se
     * actualice una calificación de ese RA y semestre, pasando el usuario que la registró. Es idempotente.
     */
    RecalculoEstadisticasResponseDTO recalcular(UUID resultadoAprendizajeId, UUID semestreId,
                                                UsuarioPrincipal principal);

    /** Estadísticas del último recálculo, con el mismo alcance por rol. */
    EstadisticasResultadoAprendizajeResponseDTO consultarEstadisticas(UUID resultadoAprendizajeId, UUID semestreId,
                                                                      UsuarioPrincipal principal);

    /** El catálogo de los 4 rangos de nivel de logro. */
    List<RangoNivelLogroResponseDTO> consultarCatalogo();
}
