package co.edu.uco.sigra.calificaciones.repository;

import co.edu.uco.sigra.calificaciones.entity.Calificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CalificacionRepository extends JpaRepository<Calificacion, UUID> {

    boolean existsByEvaluacion_Id(UUID evaluacionId);

    boolean existsByEvaluacion_IdAndMatricula_Id(UUID evaluacionId, UUID matriculaId);

    long countByMatricula_Id(UUID matriculaId);

    @Query("""
            select c from Calificacion c
            join fetch c.matricula m
            join fetch m.estudiante
            where c.evaluacion.id = :evaluacionId
            """)
    List<Calificacion> buscarPorEvaluacion(@Param("evaluacionId") UUID evaluacionId);
}