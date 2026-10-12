package co.edu.uco.sigra.estadisticas.repository;

import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConclusionRepository extends JpaRepository<Conclusion, UUID> {

    /** La conclusión VIGENTE de un (RA, semestre), si existe: RF-16 no permite duplicarla. */
    Optional<Conclusion> findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
            UUID resultadoAprendizajeId, UUID semestreId, EstadoConclusion estado);
}
