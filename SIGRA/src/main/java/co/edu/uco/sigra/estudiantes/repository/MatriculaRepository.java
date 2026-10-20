package co.edu.uco.sigra.estudiantes.repository;

import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatriculaRepository extends JpaRepository<Matricula, UUID> {

    /**
     * Busca la matrícula de una terna (estudiante, asignatura, semestre) sin filtrar por
     * estado: el servicio decide, con el resultado, si crea una matrícula nueva, reactiva
     * una retirada o rechaza por duplicada (design.md, D3).
     */
    @Query("""
            select m from Matricula m
            join fetch m.estudiante
            join fetch m.asignatura
            join fetch m.semestre
            where m.estudiante.id = :estudianteId
              and m.asignatura.id = :asignaturaId
              and m.semestre.id = :semestreId
            """)
    Optional<Matricula> buscarPorTerna(@Param("estudianteId") UUID estudianteId,
                                        @Param("asignaturaId") UUID asignaturaId,
                                        @Param("semestreId") UUID semestreId);

    /**
     * Lista las matrículas de una asignatura en un semestre. Cuando {@code estado} es nulo
     * devuelve activas y desvinculadas; cuando se indica un estado, filtra por él. Una sola
     * consulta cubre los dos listados (con y sin filtro de estado) que pide el servicio.
     */
    @Query("""
            select m from Matricula m
            join fetch m.estudiante
            join fetch m.asignatura
            join fetch m.semestre
            where m.asignatura.id = :asignaturaId
              and m.semestre.id = :semestreId
              and (:estado is null or m.estado = :estado)
            """)
    List<Matricula> buscarPorAsignaturaYSemestre(@Param("asignaturaId") UUID asignaturaId,
                                                  @Param("semestreId") UUID semestreId,
                                                  @Param("estado") EstadoMatricula estado);

    /**
     * Historial completo de matrículas de un estudiante, sin importar el estado.
     */
    @Query("""
            select m from Matricula m
            join fetch m.estudiante
            join fetch m.asignatura
            join fetch m.semestre
            where m.estudiante.id = :estudianteId
            """)
    List<Matricula> buscarPorEstudiante(@Param("estudianteId") UUID estudianteId);

    @Query("""
            select count(m) from Matricula m, AsignacionDocente a
            where m.estudiante.id = :estudianteId
              and a.profesor.id = :profesorId
              and a.asignatura.id = m.asignatura.id
              and m.estado = :matriculaEstado
              and a.estado = :asignacionEstado
              and m.estudiante.estado = :estudianteEstado
            """)
    long countActiveByStudentAndProfessor(@Param("estudianteId") UUID estudianteId,
                                          @Param("profesorId") UUID profesorId,
                                          @Param("matriculaEstado") EstadoMatricula matriculaEstado,
                                          @Param("asignacionEstado") EstadoRegistro asignacionEstado,
                                          @Param("estudianteEstado") EstadoRegistro estudianteEstado);

    @Query("""
            select m from Matricula m
            join fetch m.estudiante
            join fetch m.asignatura
            join fetch m.semestre
            where m.estudiante.id = :estudianteId
              and m.estado = :matriculaEstado
              and m.estudiante.estado = :estudianteEstado
              and exists (
                  select a.id from AsignacionDocente a
                  where a.profesor.id = :profesorId
                    and a.asignatura.id = m.asignatura.id
                    and a.estado = :asignacionEstado
              )
            """)
    List<Matricula> buscarPorEstudianteYProfesor(@Param("estudianteId") UUID estudianteId,
                                                  @Param("profesorId") UUID profesorId,
                                                  @Param("matriculaEstado") EstadoMatricula matriculaEstado,
                                                  @Param("asignacionEstado") EstadoRegistro asignacionEstado,
                                                  @Param("estudianteEstado") EstadoRegistro estudianteEstado);
}
