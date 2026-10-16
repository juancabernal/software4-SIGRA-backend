package co.edu.uco.sigra.evaluaciones.repository;

import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, UUID> {

    List<Evaluacion> findByResultadoAprendizaje_IdAndSemestre_IdOrderByFechaTentativaAscNombreAsc(UUID raId,
                                                                                                  UUID semestreId);
}