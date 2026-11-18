package co.edu.uco.sigra.asignaturas.repository;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AsignaturaRepository extends JpaRepository<Asignatura, UUID> {
    boolean existsByCodigo(String codigo);
    boolean existsByPrograma_IdAndEstado(UUID programaId, EstadoAsignatura estado);

    /** Asignaciones que ya existían al terminar el semestre; sin subconsultas correlacionadas. */
    @Query("""
            select distinct ad.asignatura.id
            from AsignacionDocente ad, Semestre s
            where s.id = :semestreId and ad.estado = :estado
              and ad.fechaAsignacion <= s.fechaFin
              and (:profesorId is null or ad.profesor.id = :profesorId)
            """)
    List<UUID> findIdsConAsignacionEnSemestre(@Param("semestreId") UUID semestreId,
                                             @Param("profesorId") UUID profesorId,
                                             @Param("estado") EstadoRegistro estado);

    @Query("""
            select distinct e.resultadoAprendizaje.asignatura.id
            from Evaluacion e where e.semestre.id = :semestreId
            """)
    List<UUID> findIdsConEvaluacionesEnSemestre(@Param("semestreId") UUID semestreId);

    @Query("select distinct m.asignatura.id from Matricula m where m.semestre.id = :semestreId")
    List<UUID> findIdsConMatriculasEnSemestre(@Param("semestreId") UUID semestreId);

    @Query("select r.asignatura.id as asignaturaId, count(r) as cantidad from ResultadoAprendizaje r "
            + "where r.estado = :estado group by r.asignatura.id")
    List<ConteoRaPorAsignatura> contarRaPorAsignatura(@Param("estado") EstadoRegistro estado);

    /** Todo el catálogo con su programa y su facultad en una sola consulta (sin N+1 al leer la facultad). */
    @Query("select a from Asignatura a join fetch a.programa p join fetch p.facultad")
    List<Asignatura> findAllConProgramaYFacultad();

    /** Catálogo de una sola facultad (incidencia 0000036), con programa y facultad cargados. */
    @Query("select a from Asignatura a join fetch a.programa p join fetch p.facultad f where f.id = :facultadId")
    List<Asignatura> findAllDeFacultad(@Param("facultadId") UUID facultadId);

    // ── «Mis asignaturas»: una consulta por dato, agrupada, sin N+1 ─────────────────────────────

    @Query("select a from Asignatura a join fetch a.programa p join fetch p.facultad where a.id in "
            + "(select ad.asignatura.id from AsignacionDocente ad "
            + "where ad.profesor.id = :profesorId and ad.estado = :estado)")
    List<Asignatura> findAsignadasAProfesor(@Param("profesorId") UUID profesorId,
                                            @Param("estado") EstadoRegistro estado);

    /** Igual que {@link #findAsignadasAProfesor}, acotado a una facultad. */
    @Query("select a from Asignatura a join fetch a.programa p join fetch p.facultad f where f.id = :facultadId "
            + "and a.id in (select ad.asignatura.id from AsignacionDocente ad "
            + "where ad.profesor.id = :profesorId and ad.estado = :estado)")
    List<Asignatura> findAsignadasAProfesorEnFacultad(@Param("profesorId") UUID profesorId,
                                                      @Param("estado") EstadoRegistro estado,
                                                      @Param("facultadId") UUID facultadId);

    @Query("select a from Asignatura a join fetch a.programa p join fetch p.facultad where a.id in "
            + "(select m.asignatura.id from Matricula m "
            + "where m.estudiante.id = :estudianteId and m.estado = :estado)")
    List<Asignatura> findMatriculadasPorEstudiante(@Param("estudianteId") UUID estudianteId,
                                                   @Param("estado") EstadoMatricula estado);

    /** Igual que {@link #findMatriculadasPorEstudiante}, acotado a una facultad. */
    @Query("select a from Asignatura a join fetch a.programa p join fetch p.facultad f where f.id = :facultadId "
            + "and a.id in (select m.asignatura.id from Matricula m "
            + "where m.estudiante.id = :estudianteId and m.estado = :estado)")
    List<Asignatura> findMatriculadasPorEstudianteEnFacultad(@Param("estudianteId") UUID estudianteId,
                                                             @Param("estado") EstadoMatricula estado,
                                                             @Param("facultadId") UUID facultadId);

    @Query("select r.asignatura.id as asignaturaId, count(r) as cantidad from ResultadoAprendizaje r "
            + "where r.asignatura.id in :ids and r.estado = :estado group by r.asignatura.id")
    List<ConteoPorAsignatura> contarRaDe(@Param("ids") Collection<UUID> ids, @Param("estado") EstadoRegistro estado);

    @Query("select m.asignatura.id as asignaturaId, count(m) as cantidad from Matricula m "
            + "where m.asignatura.id in :ids and m.estado = :estado group by m.asignatura.id")
    List<ConteoPorAsignatura> contarMatriculasDe(@Param("ids") Collection<UUID> ids,
                                                 @Param("estado") EstadoMatricula estado);

    @Query("select ad.asignatura.id as asignaturaId, "
            + "trim(concat(p.primerNombre, ' ', coalesce(p.segundoNombre, ''), ' ', p.primerApellido, ' ', p.segundoApellido)) as nombre "
            + "from AsignacionDocente ad "
            + "join ad.profesor p where ad.asignatura.id in :ids and ad.estado = :estado "
            + "order by p.primerApellido, p.primerNombre")
    List<ProfesorDeAsignatura> profesoresDe(@Param("ids") Collection<UUID> ids, @Param("estado") EstadoRegistro estado);
}
