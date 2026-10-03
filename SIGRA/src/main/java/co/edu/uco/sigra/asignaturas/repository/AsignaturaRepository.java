package co.edu.uco.sigra.asignaturas.repository;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.shared.enums.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AsignaturaRepository extends JpaRepository<Asignatura, UUID> {
    boolean existsByCodigo(String codigo);
    boolean existsByPrograma_IdAndEstado(UUID programaId, EstadoAsignatura estado);

    @Query("select r.asignatura.id as asignaturaId, count(r) as cantidad from ResultadoAprendizaje r "
            + "where r.estado = :estado group by r.asignatura.id")
    List<ConteoRaPorAsignatura> contarRaPorAsignatura(@Param("estado") EstadoRegistro estado);
}
