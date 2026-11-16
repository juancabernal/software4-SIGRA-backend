package co.edu.uco.sigra.reportes.repository;

import co.edu.uco.sigra.mejoras.entity.AccionMejora;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AccionesMejoraReporteRepository extends Repository<AccionMejora, UUID> {
    @Query("""
            select a from AccionMejora a
            join fetch a.resultadoAprendizaje ra
            join fetch ra.asignatura materia
            join fetch a.conclusionOrigen conclusion
            join fetch conclusion.semestre semestre
            where (:semestreId is null or semestre.id = :semestreId)
              and (:asignaturaId is null or materia.id = :asignaturaId)
              and (:resultadoAprendizajeId is null or ra.id = :resultadoAprendizajeId)
              and (:convertida is null
                   or (:convertida = true and a.evaluacionGeneradaId is not null)
                   or (:convertida = false and a.evaluacionGeneradaId is null))
            order by a.fechaPropuesta desc, a.id
            """)
    List<AccionMejora> consultar(@Param("semestreId") UUID semestreId,
                               @Param("asignaturaId") UUID asignaturaId,
                               @Param("resultadoAprendizajeId") UUID resultadoAprendizajeId,
                               @Param("convertida") Boolean convertida);
}
