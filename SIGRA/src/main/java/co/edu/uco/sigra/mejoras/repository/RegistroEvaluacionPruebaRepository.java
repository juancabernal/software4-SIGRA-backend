package co.edu.uco.sigra.mejoras.repository;

import co.edu.uco.sigra.mejoras.entity.RegistroEvaluacionPrueba;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RegistroEvaluacionPruebaRepository extends JpaRepository<RegistroEvaluacionPrueba, UUID> {

    @Query("select coalesce(sum(r.porcentaje), 0) from RegistroEvaluacionPrueba r "
            + "where r.resultadoAprendizaje.id = :resultadoAprendizajeId and r.semestre.id = :semestreId")
    double sumarPorcentajes(@Param("resultadoAprendizajeId") UUID resultadoAprendizajeId,
                             @Param("semestreId") UUID semestreId);
}
