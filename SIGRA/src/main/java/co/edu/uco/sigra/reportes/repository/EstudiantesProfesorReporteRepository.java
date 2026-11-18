package co.edu.uco.sigra.reportes.repository;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * R7 (vista del profesor): estudiantes con matrícula activa en el semestre en alguna asignatura que el
 * profesor tiene asignada de forma activa. Una fila por estudiante×asignatura.
 */
public interface EstudiantesProfesorReporteRepository extends Repository<Matricula, UUID> {

    @Query("""
            select e.id as estudianteId, e.primerNombre as primerNombre, e.segundoNombre as segundoNombre,
                   e.primerApellido as primerApellido, e.segundoApellido as segundoApellido,
                   e.correoInstitucional as correoInstitucional, a.codigo as asignaturaCodigo
            from Matricula m
            join m.estudiante e
            join m.asignatura a
            where m.semestre.id = :semestreId
              and m.estado = :matriculaActiva
              and exists (select ad.id from AsignacionDocente ad
                          where ad.asignatura = a and ad.profesor.id = :profesorId and ad.estado = :asignacionActiva)
            order by e.primerApellido asc, e.primerNombre asc, a.codigo asc
            """)
    List<EstudianteAsignatura> listarEstudiantesDelProfesor(@Param("profesorId") UUID profesorId,
                                                            @Param("semestreId") UUID semestreId,
                                                            @Param("matriculaActiva") EstadoMatricula matriculaActiva,
                                                            @Param("asignacionActiva") EstadoRegistro asignacionActiva);

    interface EstudianteAsignatura {
        UUID getEstudianteId();
        String getPrimerNombre();
        String getSegundoNombre();
        String getPrimerApellido();
        String getSegundoApellido();
        String getCorreoInstitucional();
        String getAsignaturaCodigo();
    }
}
