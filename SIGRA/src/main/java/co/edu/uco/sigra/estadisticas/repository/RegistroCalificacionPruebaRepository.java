package co.edu.uco.sigra.estadisticas.repository;

import co.edu.uco.sigra.estadisticas.entity.RegistroCalificacionPrueba;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Repositorio de la tabla de prueba de calificaciones. Se retira junto con {@link RegistroCalificacionPrueba}. */
@Repository
public interface RegistroCalificacionPruebaRepository extends JpaRepository<RegistroCalificacionPrueba, UUID> {

    @Query("""
            select r from RegistroCalificacionPrueba r
            join fetch r.estudiante
            where r.resultadoAprendizaje.id = :raId
              and r.semestre.id = :semestreId
            """)
    List<RegistroCalificacionPrueba> buscarPorRaYSemestre(@Param("raId") UUID resultadoAprendizajeId,
                                                          @Param("semestreId") UUID semestreId);
}
