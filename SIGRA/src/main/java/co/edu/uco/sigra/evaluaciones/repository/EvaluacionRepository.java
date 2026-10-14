package co.edu.uco.sigra.evaluaciones.repository;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, UUID> {

    /** Suma de porcentajes de las evaluaciones en el estado indicado de un (RA, semestre); 0 si no hay. */
    @Query("select coalesce(sum(e.porcentaje), 0) from Evaluacion e "
            + "where e.resultadoAprendizaje.id = :raId and e.semestre.id = :semestreId and e.estado = :estado")
    BigDecimal sumarPorcentajes(@Param("raId") UUID raId,
                                @Param("semestreId") UUID semestreId,
                                @Param("estado") EstadoEvaluacion estado);

    /** Igual que {@link #sumarPorcentajes} pero sin contar la evaluación indicada (para modificar). */
    @Query("select coalesce(sum(e.porcentaje), 0) from Evaluacion e "
            + "where e.resultadoAprendizaje.id = :raId and e.semestre.id = :semestreId and e.estado = :estado "
            + "and e.id <> :idExcluido")
    BigDecimal sumarPorcentajesExcluyendo(@Param("raId") UUID raId,
                                          @Param("semestreId") UUID semestreId,
                                          @Param("estado") EstadoEvaluacion estado,
                                          @Param("idExcluido") UUID idExcluido);

    boolean existsByResultadoAprendizaje_IdAndSemestre_IdAndNombreIgnoreCase(UUID raId, UUID semestreId,
                                                                            String nombre);

    boolean existsByResultadoAprendizaje_IdAndSemestre_IdAndNombreIgnoreCaseAndIdNot(UUID raId, UUID semestreId,
                                                                                    String nombre, UUID id);

    List<Evaluacion> findByResultadoAprendizaje_IdAndSemestre_IdOrderByFechaTentativaAscNombreAsc(UUID raId,
                                                                                                 UUID semestreId);

    /** Cascada al inactivar un RA: sus evaluaciones activas pasan a INACTIVA marcadas como inactivadas con el RA. */
    @Modifying(flushAutomatically = true)
    @Query("update Evaluacion e set e.estado = :inactiva, e.inactivadaConRa = true "
            + "where e.resultadoAprendizaje.id = :raId and e.estado = :activa")
    int inactivarActivasPorResultado(@Param("raId") UUID raId,
                                     @Param("inactiva") EstadoEvaluacion inactiva,
                                     @Param("activa") EstadoEvaluacion activa);

    /** Al reactivar un RA: vuelven a ACTIVA solo las evaluaciones que se inactivaron con él. */
    @Modifying(flushAutomatically = true)
    @Query("update Evaluacion e set e.estado = :activa, e.inactivadaConRa = false "
            + "where e.resultadoAprendizaje.id = :raId and e.inactivadaConRa = true")
    int restaurarInactivadasConResultado(@Param("raId") UUID raId,
                                         @Param("activa") EstadoEvaluacion activa);

    /** Cascada al inactivar una asignatura: las evaluaciones activas de todos sus RA. */
    @Modifying(flushAutomatically = true)
    @Query("update Evaluacion e set e.estado = :inactiva, e.inactivadaConRa = true "
            + "where e.estado = :activa and e.resultadoAprendizaje.id in "
            + "(select r.id from ResultadoAprendizaje r where r.asignatura.id = :asignaturaId)")
    int inactivarActivasPorAsignatura(@Param("asignaturaId") UUID asignaturaId,
                                      @Param("inactiva") EstadoEvaluacion inactiva,
                                      @Param("activa") EstadoEvaluacion activa);

    /**
     * Al reactivar una asignatura: se restauran las evaluaciones inactivadas con su RA, pero solo de los
     * RA que quedaron ACTIVOS (un RA que el administrador inactivó a mano sigue inactivo, y sus
     * evaluaciones también). Debe llamarse después de restaurar los RA.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Evaluacion e set e.estado = :activa, e.inactivadaConRa = false "
            + "where e.inactivadaConRa = true and e.resultadoAprendizaje.id in "
            + "(select r.id from ResultadoAprendizaje r where r.asignatura.id = :asignaturaId and r.estado = :raActivo)")
    int restaurarInactivadasPorAsignatura(@Param("asignaturaId") UUID asignaturaId,
                                          @Param("activa") EstadoEvaluacion activa,
                                          @Param("raActivo") EstadoRegistro raActivo);
}
