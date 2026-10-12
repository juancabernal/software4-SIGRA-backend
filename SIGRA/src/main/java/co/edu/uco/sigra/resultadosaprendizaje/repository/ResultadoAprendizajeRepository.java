package co.edu.uco.sigra.resultadosaprendizaje.repository;

import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResultadoAprendizajeRepository extends JpaRepository<ResultadoAprendizaje, UUID> {


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
