package co.edu.uco.sigra.evaluaciones.service;

import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionActualizarRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionCrearRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionResponseDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionesRaResumenDTO;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Gestión de evaluaciones por RA (RF-10 / RF-11).
 * <p>
 * La identidad puede ser {@code null} (control por rol apagado): en ese caso no se filtra por dueño.
 * Con identidad de PROFESOR, solo opera sobre las asignaturas con asignación docente ACTIVA.
 */
public interface EvaluacionService {

    /** RF-10a. También lo usa RF-18 (conversión de una acción de mejora en evaluación). */
    EvaluacionResponseDTO crear(UUID resultadoAprendizajeId, EvaluacionCrearRequestDTO dto, IdentidadActual identidad);

    /** RF-10b. */
    EvaluacionResponseDTO modificar(UUID id, EvaluacionActualizarRequestDTO dto, IdentidadActual identidad);

    /** RF-10c. */
    EvaluacionResponseDTO inactivar(UUID id, IdentidadActual identidad);

    /** RF-10c. */
    EvaluacionResponseDTO reactivar(UUID id, IdentidadActual identidad);

    /** Vista del profesor: evaluaciones del RA en el semestre (el activo si {@code semestreId} es null). */
    EvaluacionesRaResumenDTO consultarPorResultadoAprendizaje(UUID resultadoAprendizajeId, UUID semestreId,
                                                             IdentidadActual identidad);

    EvaluacionResponseDTO consultarPorId(UUID id, IdentidadActual identidad);

    /** Suma de porcentajes de las evaluaciones ACTIVAS de un (RA, semestre). Lo usa RF-18. */
    BigDecimal consultarPorcentajeAsignado(UUID resultadoAprendizajeId, UUID semestreId);

    /** Cascada al inactivar un RA. */
    void inactivarPorResultadoAprendizaje(UUID resultadoAprendizajeId);

    /** Al reactivar un RA: restaura solo las evaluaciones inactivadas con él. */
    void restaurarPorResultadoAprendizaje(UUID resultadoAprendizajeId);

    /** Cascada al inactivar una asignatura. */
    void inactivarPorAsignatura(UUID asignaturaId);

    /** Al reactivar una asignatura (después de restaurar sus RA). */
    void restaurarPorAsignatura(UUID asignaturaId);
}
