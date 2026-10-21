package co.edu.uco.sigra.evaluaciones.service;

import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionActualizarRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionCrearRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionResponseDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionesRaResumenDTO;
import co.edu.uco.sigra.semestres.entity.Semestre;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestión de evaluaciones por RA (RF-10 / RF-11).
 * <p>
 * La identidad es obligatoria y el servicio aplica el alcance del profesor sobre sus asignaturas.
 * Con identidad de PROFESOR, solo opera sobre las asignaturas con asignación docente ACTIVA.
 */
public interface EvaluacionService {

    /** RF-10a. También lo usa RF-18 (conversión de una acción de mejora en evaluación). */
    EvaluacionResponseDTO crear(UUID resultadoAprendizajeId, EvaluacionCrearRequestDTO dto, UsuarioPrincipal principal);

    /** RF-10b. */
    EvaluacionResponseDTO modificar(UUID id, EvaluacionActualizarRequestDTO dto, UsuarioPrincipal principal);

    /** RF-10c. */
    EvaluacionResponseDTO inactivar(UUID id, UsuarioPrincipal principal);

    /** RF-10c. */
    EvaluacionResponseDTO reactivar(UUID id, UsuarioPrincipal principal);

    /** Vista del profesor: evaluaciones del RA en el semestre (el activo si {@code semestreId} es null). */
    EvaluacionesRaResumenDTO consultarPorResultadoAprendizaje(UUID resultadoAprendizajeId, UUID semestreId,
                                                             UsuarioPrincipal principal);

    EvaluacionResponseDTO consultarPorId(UUID id, UsuarioPrincipal principal);

    /**
     * El semestre activo: el que contiene la fecha de hoy en la zona de Colombia. Vacío si hoy no cae en
     * ningún semestre. Lo usan los demás módulos que necesitan el mismo criterio (p. ej. el rendimiento del RA).
     */
    Optional<Semestre> consultarSemestreActivo();

    /** Suma de porcentajes de las evaluaciones ACTIVAS de un (RA, semestre). Lo usa RF-18. */
    BigDecimal consultarPorcentajeAsignado(UUID resultadoAprendizajeId, UUID semestreId);

    /** Cascada al inactivar un RA: solo sus evaluaciones del semestre activo; las de otros semestres no se tocan. */
    void inactivarPorResultadoAprendizaje(UUID resultadoAprendizajeId);

    /** Al reactivar un RA: restaura solo las evaluaciones del semestre activo inactivadas con él. */
    void restaurarPorResultadoAprendizaje(UUID resultadoAprendizajeId);

    /** Cascada al inactivar una asignatura: solo las evaluaciones del semestre activo. */
    void inactivarPorAsignatura(UUID asignaturaId);

    /** Al reactivar una asignatura (después de restaurar sus RA). */
    void restaurarPorAsignatura(UUID asignaturaId);
}
