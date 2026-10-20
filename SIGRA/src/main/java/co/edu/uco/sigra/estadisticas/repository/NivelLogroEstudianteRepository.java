package co.edu.uco.sigra.estadisticas.repository;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
