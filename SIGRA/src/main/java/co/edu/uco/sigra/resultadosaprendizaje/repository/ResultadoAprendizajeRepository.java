package co.edu.uco.sigra.resultadosaprendizaje.repository;

import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.shared.enums.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ResultadoAprendizajeRepository extends JpaRepository<ResultadoAprendizaje, UUID> {
    long countByAsignatura_IdAndEstado(UUID asignaturaId, EstadoRegistro estado);

    @Modifying(flushAutomatically = true)
    @Query("update ResultadoAprendizaje r set r.estado = :nuevo where r.asignatura.id = :asignaturaId and r.estado = :actual")
    int cambiarEstadoPorAsignatura(@Param("asignaturaId") UUID asignaturaId,
                                   @Param("actual") EstadoRegistro actual,
                                   @Param("nuevo") EstadoRegistro nuevo);
}
