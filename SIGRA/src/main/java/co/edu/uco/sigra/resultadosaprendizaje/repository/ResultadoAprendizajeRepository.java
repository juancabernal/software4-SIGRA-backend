package co.edu.uco.sigra.resultadosaprendizaje.repository;

import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResultadoAprendizajeRepository extends JpaRepository<ResultadoAprendizaje, UUID> {


    /**
     * Bloquea (SELECT ... FOR UPDATE) la fila del RA hasta el fin de la transacción. Lo usa evaluaciones para
     * validar la suma de porcentajes sin que dos peticiones simultáneas la excedan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ResultadoAprendizaje r where r.id = :id")
    Optional<ResultadoAprendizaje> bloquearPorId(@Param("id") UUID id);

    /**
     * Bloquea la fila de la asignatura mientras se valida el tope (7) y el mínimo (5) de RA activos. Se escribe
     * aquí, con el nombre de la entidad, para no modificar el repositorio del módulo de asignaturas.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Asignatura a where a.id = :id")
    Optional<Asignatura> bloquearAsignaturaPorId(@Param("id") UUID id);

    long countByAsignatura_IdAndEstado(UUID asignaturaId, EstadoRegistro estado);

    @Modifying(flushAutomatically = true)
    @Query("update ResultadoAprendizaje r set r.estado = :inactivo, r.inactivadoConAsignatura = true "
            + "where r.asignatura.id = :asignaturaId and r.estado = :activo")
    int inactivarActivosPorAsignatura(@Param("asignaturaId") UUID asignaturaId,
                                      @Param("inactivo") EstadoRegistro inactivo,
                                      @Param("activo") EstadoRegistro activo);

    @Modifying(flushAutomatically = true)
    @Query("update ResultadoAprendizaje r set r.estado = :activo, r.inactivadoConAsignatura = false "
            + "where r.asignatura.id = :asignaturaId and r.inactivadoConAsignatura = true")
    int restaurarInactivadosConAsignatura(@Param("asignaturaId") UUID asignaturaId,
                                          @Param("activo") EstadoRegistro activo);


    boolean existsByAsignatura_IdAndCodigo(UUID asignaturaId, String codigo);


    List<ResultadoAprendizaje> findByAsignatura_IdOrderByCodigoAsc(UUID asignaturaId);


    List<ResultadoAprendizaje> findByAsignatura_IdAndEstadoOrderByCodigoAsc(UUID asignaturaId, EstadoRegistro estado);


    List<ResultadoAprendizaje> findByEstadoOrderByCodigoAsc(EstadoRegistro estado);
}
