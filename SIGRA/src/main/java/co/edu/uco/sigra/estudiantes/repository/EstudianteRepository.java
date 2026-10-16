package co.edu.uco.sigra.estudiantes.repository;

import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EstudianteRepository extends JpaRepository<Estudiante, UUID> {

    boolean existsByTipoDocumento_IdAndNumeroDocumento(UUID tipoDocumentoId, String numeroDocumento);

    // Solo revisa estudiantes. La unicidad del correo entre todos los roles se valida con
    // UsuarioRepository.existsByCorreoInstitucionalIgnoreCase.
    boolean existsByCorreoInstitucional(String correoInstitucional);

    boolean existsByCorreoInstitucionalAndIdNot(String correoInstitucional, UUID id);

    /**
     * Busca estudiantes cuyo nombre completo o numero de documento contenga el criterio,
     * sin distinguir mayusculas de minusculas. El filtro se resuelve en la base de datos
     * con LOWER() y LIKE, no trayendo la tabla a memoria.
     *
     * <p>Este metodo nunca debe invocarse con criterio nulo: Hibernate no logra inferir
     * el tipo del parametro cuando el valor real es null y PostgreSQL termina rechazando
     * el bind (lower(bytea) no existe). El listado sin filtro lo resuelve el servicio
     * con {@link org.springframework.data.jpa.repository.JpaRepository#findAll()}.</p>
     */
    @Query("""
            select e from Estudiante e
            where lower(e.nombreCompleto) like concat('%', lower(:criterio), '%')
               or lower(e.numeroDocumento) like concat('%', lower(:criterio), '%')
            """)
    List<Estudiante> buscarPorNombreODocumento(@Param("criterio") String criterio);

    @Query("""
            select distinct e from Estudiante e, Matricula m, AsignacionDocente a
            where e.id = m.estudiante.id
              and a.asignatura.id = m.asignatura.id
              and a.profesor.id = :profesorId
              and m.estado = :matriculaEstado
              and a.estado = :asignacionEstado
              and e.estado = :estudianteEstado
            and (:criterio = ''
                   or lower(e.nombreCompleto) like concat('%', lower(:criterio), '%')
                   or lower(e.numeroDocumento) like concat('%', lower(:criterio), '%'))
            order by e.nombreCompleto
            """)
    List<Estudiante> buscarVisiblesParaProfesor(@Param("profesorId") UUID profesorId,
                                                @Param("criterio") String criterio,
                                                @Param("matriculaEstado") EstadoMatricula matriculaEstado,
                                                @Param("asignacionEstado") EstadoRegistro asignacionEstado,
                                                @Param("estudianteEstado") EstadoRegistro estudianteEstado);

    /** Comprueba alcance puntual sin materializar todos los estudiantes visibles del profesor. */
    @Query("""
            select case when count(e) > 0 then true else false end
            from Estudiante e, Matricula m, AsignacionDocente a
            where e.id = :estudianteId
              and e.id = m.estudiante.id
              and a.asignatura.id = m.asignatura.id
              and a.profesor.id = :profesorId
              and m.estado = :matriculaEstado
              and a.estado = :asignacionEstado
              and e.estado = :estudianteEstado
            """)
    boolean existsVisibleToProfessor(@Param("estudianteId") UUID estudianteId,
                                     @Param("profesorId") UUID profesorId,
                                     @Param("matriculaEstado") EstadoMatricula matriculaEstado,
                                     @Param("asignacionEstado") EstadoRegistro asignacionEstado,
                                     @Param("estudianteEstado") EstadoRegistro estudianteEstado);
}
