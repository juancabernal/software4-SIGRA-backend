package co.edu.uco.sigra.estadisticas.repository;

import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConclusionRepository extends JpaRepository<Conclusion, UUID> {

    /** La conclusión VIGENTE de un (RA, semestre), si existe: RF-16 no permite duplicarla. */
    Optional<Conclusion> findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
            UUID resultadoAprendizajeId, UUID semestreId, EstadoConclusion estado);

    /**
     * La conclusión más reciente del mismo RA en un semestre anterior al dado (por fechaInicio),
     * para comparar el porcentaje insuficiente con el semestre actual (RF-16, diagnóstico).
     */
    @Query("SELECT c FROM Conclusion c "
            + "WHERE c.resultadoAprendizaje.id = :resultadoAprendizajeId "
            + "AND c.semestre.fechaInicio < :fechaInicioActual "
            + "ORDER BY c.semestre.fechaInicio DESC, c.fechaGeneracion DESC")
    List<Conclusion> buscarAnteriores(@Param("resultadoAprendizajeId") UUID resultadoAprendizajeId,
                                       @Param("fechaInicioActual") LocalDate fechaInicioActual);
}
