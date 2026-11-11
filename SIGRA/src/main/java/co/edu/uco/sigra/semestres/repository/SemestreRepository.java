package co.edu.uco.sigra.semestres.repository;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
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

    /**
     * Semestres en los que el profesor tiene asignaturas (Mantis 0000045). La asignación docente no
     * guarda el semestre, así que un semestre cuenta si, para alguna asignación en el estado indicado:
     * la asignación ya existía antes de que el semestre terminara, o la asignatura tiene evaluaciones o
     * matrículas registradas en ese semestre (cubre semestres anteriores a la fecha de asignación).
     */
    @Query("""
            select s from Semestre s
            where exists (
                select ad.id from AsignacionDocente ad
                where ad.profesor.id = :profesorId
                  and ad.estado = :estado
                  and (ad.fechaAsignacion <= s.fechaFin
                       or exists (select e.id from Evaluacion e
                                  where e.semestre = s and e.resultadoAprendizaje.asignatura = ad.asignatura)
                       or exists (select m.id from Matricula m
                                  where m.semestre = s and m.asignatura = ad.asignatura))
            )
            order by s.fechaInicio desc
            """)
    List<Semestre> findConAsignaturasDelProfesor(@Param("profesorId") UUID profesorId,
                                                 @Param("estado") EstadoRegistro estado);
}
