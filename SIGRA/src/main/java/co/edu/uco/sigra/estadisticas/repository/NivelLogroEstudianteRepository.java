package co.edu.uco.sigra.estadisticas.repository;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface NivelLogroEstudianteRepository extends JpaRepository<NivelLogroEstudiante, UUID> {

    List<NivelLogroEstudiante> findByResultadoAprendizaje_IdAndSemestre_Id(UUID resultadoAprendizajeId,
                                                                          UUID semestreId);

    /** Estudiantes con nivel de logro calculado (3 o más calificaciones) en el (RA, semestre). */
    long countByResultadoAprendizaje_IdAndSemestre_IdAndNivelLogroIsNotNull(UUID resultadoAprendizajeId,
                                                                            UUID semestreId);

    long countByResultadoAprendizaje_IdAndSemestre_IdAndNivelLogroIn(UUID resultadoAprendizajeId,
                                                                     UUID semestreId,
                                                                     Collection<NivelLogro> niveles);

    /**
     * Para cada (RA, semestre) con algún nivel de logro calculado, el instante del cálculo más reciente.
     *
     * @return filas {@code [resultadoAprendizajeId (UUID), semestreId (UUID), instante (LocalDateTime)]}
     */
    @Query("select n.resultadoAprendizaje.id, n.semestre.id, max(n.fechaCalculo) from NivelLogroEstudiante n "
            + "group by n.resultadoAprendizaje.id, n.semestre.id")
    List<Object[]> buscarUltimoCalculoPorPar();

    /**
     * Filas de riesgo académico (R6): nivel BAJO/MEDIO en RAs activos de las asignaturas indicadas.
     * Trae estudiante, RA, asignatura y programa para armar el reporte sin N+1.
     */
    @Query("""
            select distinct n from NivelLogroEstudiante n
            join fetch n.estudiante
            join fetch n.resultadoAprendizaje ra
            join fetch ra.asignatura a
            join fetch a.programa p
            join fetch p.facultad
            where n.semestre.id = :semestreId
              and n.nivelLogro in :niveles
              and ra.estado = :estadoRa
              and a.id in :asignaturaIds
            """)
    List<NivelLogroEstudiante> buscarInsuficientesPorSemestreYAsignaturas(
            @Param("semestreId") UUID semestreId,
            @Param("niveles") Collection<NivelLogro> niveles,
            @Param("estadoRa") EstadoRegistro estadoRa,
            @Param("asignaturaIds") Collection<UUID> asignaturaIds);
}
