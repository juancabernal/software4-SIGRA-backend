package co.edu.uco.sigra.semestres.repository;

import co.edu.uco.sigra.semestres.entity.Semestre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SemestreRepository extends JpaRepository<Semestre, UUID> {

    Optional<Semestre> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Semestre> findAllByOrderByFechaInicioDesc();

    /**
     * Dos rangos [a, b] y [c, d] se cruzan (incluyendo los extremos) si a <= d y b >= c.
     * Esta condición cubre cruce parcial, intersección y que un semestre contenga a otro.
     */
    @Query("""
            select case when count(s) > 0 then true else false end
            from Semestre s
            where s.fechaInicio <= :fechaFin and s.fechaFin >= :fechaInicio
            """)
    boolean existeSolapamiento(@Param("fechaInicio") LocalDate fechaInicio,
                               @Param("fechaFin") LocalDate fechaFin);

    /** Igual que {@link #existeSolapamiento} pero ignorando el semestre indicado (para extensiones). */
    @Query("""
            select case when count(s) > 0 then true else false end
            from Semestre s
            where s.id <> :idExcluido
              and s.fechaInicio <= :fechaFin and s.fechaFin >= :fechaInicio
            """)
    boolean existeSolapamientoExcluyendo(@Param("fechaInicio") LocalDate fechaInicio,
                                         @Param("fechaFin") LocalDate fechaFin,
                                         @Param("idExcluido") UUID idExcluido);
}
