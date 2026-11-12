package co.edu.uco.sigra.estudiantes.repository;

import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.calificaciones.entity.Calificacion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface MiProcesoConsultaRepository extends Repository<Matricula, UUID> {

    @Query("""
            select distinct m from Matricula m
            join fetch m.asignatura a
            left join fetch a.programa
            join fetch m.semestre
            where m.estudiante.id = :estudianteId
            """)
    List<Matricula> buscarMatriculasDelEstudiante(@Param("estudianteId") UUID estudianteId);

    @Query("""
            select distinct m from Matricula m
            join fetch m.asignatura a
            left join fetch a.programa
            join fetch m.semestre
            where m.id = :matriculaId and m.estudiante.id = :estudianteId
            """)
    Optional<Matricula> buscarMatriculaPropia(@Param("matriculaId") UUID matriculaId,
                                               @Param("estudianteId") UUID estudianteId);

    @Query("""
            select r from ResultadoAprendizaje r
            where r.asignatura.id in :asignaturaIds
              and r.estado = :estado
            order by r.codigo
            """)
    List<ResultadoAprendizaje> buscarResultadosPorAsignaturas(
            @Param("asignaturaIds") List<UUID> asignaturaIds,
            @Param("estado") EstadoRegistro estado);

    @Query("""
            select e from Evaluacion e
            where e.resultadoAprendizaje.id in :resultadoIds
              and e.semestre.id in :semestreIds
              and e.estado = :estado
              and e.porcentaje > 0
            order by e.fechaTentativa, e.nombre
            """)
    List<Evaluacion> buscarEvaluaciones(
            @Param("resultadoIds") List<UUID> resultadoIds,
            @Param("semestreIds") List<UUID> semestreIds,
            @Param("estado") EstadoEvaluacion estado);

    @Query("""
            select c from Calificacion c
            where c.matricula.id in :matriculaIds
              and c.evaluacion.estado = :estadoEvaluacion
              and c.evaluacion.porcentaje > 0
            """)
    List<Calificacion> buscarCalificaciones(
            @Param("matriculaIds") List<UUID> matriculaIds,
            @Param("estadoEvaluacion") EstadoEvaluacion estadoEvaluacion);

    @Query("""
            select n from NivelLogroEstudiante n
            where n.estudiante.id = :estudianteId
              and n.semestre.id in :semestreIds
              and n.resultadoAprendizaje.id in :resultadoIds
            """)
    List<NivelLogroEstudiante> buscarNiveles(
            @Param("estudianteId") UUID estudianteId,
            @Param("semestreIds") List<UUID> semestreIds,
            @Param("resultadoIds") List<UUID> resultadoIds);
}
