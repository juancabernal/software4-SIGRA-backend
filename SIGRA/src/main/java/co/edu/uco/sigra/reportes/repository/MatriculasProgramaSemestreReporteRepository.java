package co.edu.uco.sigra.reportes.repository;

import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MatriculasProgramaSemestreReporteRepository extends Repository<Matricula, UUID> {

    @Query("""
            select m.semestre.id as semestreId, m.semestre.codigo as semestreCodigo,
                   m.semestre.fechaInicio as semestreFechaInicio, m.semestre.estado as semestreEstado,
                   p.id as programaId, p.codigo as programaCodigo, p.nombre as programaNombre,
                   f.nombre as facultadNombre, count(m) as totalMatriculas,
                   sum(case when m.estado = :activa then 1 else 0 end) as activas,
                   sum(case when m.estado = :desvinculada then 1 else 0 end) as desvinculadas,
                   count(distinct m.estudiante.id) as estudiantes,
                   count(distinct m.asignatura.id) as asignaturas
            from Matricula m
            join m.asignatura a
            join a.programa p
            join p.facultad f
            where (:semestreId is null or m.semestre.id = :semestreId)
              and (:programaId is null or p.id = :programaId)
              and (:facultadId is null or f.id = :facultadId)
            group by m.semestre.id, m.semestre.codigo, m.semestre.fechaInicio, m.semestre.estado,
                     p.id, p.codigo, p.nombre, f.nombre
            order by m.semestre.fechaInicio desc, p.nombre asc
            """)
    List<Fila> agrupar(@Param("semestreId") UUID semestreId,
                       @Param("programaId") UUID programaId,
                       @Param("facultadId") UUID facultadId,
                       @Param("activa") EstadoMatricula activa,
                       @Param("desvinculada") EstadoMatricula desvinculada);

    @Query("""
            select count(distinct m.estudiante.id)
            from Matricula m
            join m.asignatura a
            join a.programa p
            join p.facultad f
            where (:semestreId is null or m.semestre.id = :semestreId)
              and (:programaId is null or p.id = :programaId)
              and (:facultadId is null or f.id = :facultadId)
            """)
    long contarEstudiantesDistintos(@Param("semestreId") UUID semestreId,
                                    @Param("programaId") UUID programaId,
                                    @Param("facultadId") UUID facultadId);

    interface Fila {
        UUID getSemestreId();
        String getSemestreCodigo();
        LocalDate getSemestreFechaInicio();
        EstadoSemestre getSemestreEstado();
        UUID getProgramaId();
        String getProgramaCodigo();
        String getProgramaNombre();
        String getFacultadNombre();
        Long getTotalMatriculas();
        Long getActivas();
        Long getDesvinculadas();
        Long getEstudiantes();
        Long getAsignaturas();
    }
}
