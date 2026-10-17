package co.edu.uco.sigra.estadisticas.repository;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Soporte de lectura para el diagnóstico de la conclusión (RF-16): cuenta estudiantes por nivel
 * de logro en un (RA, semestre). Es un repositorio nuestro, separado de
 * NivelLogroEstudianteRepository (de Santiago), sobre la misma entidad: Spring Data admite
 * varios repositorios sobre una entidad, y así no se toca el suyo ni se crea una dependencia
 * hacia su NivelLogroService (eso sí crearía un ciclo entre beans, porque su recálculo ya llama
 * a generarConclusion).
 */
@Repository
public interface ConclusionDiagnosticoRepository extends JpaRepository<NivelLogroEstudiante, UUID> {

    long countByResultadoAprendizaje_IdAndSemestre_IdAndNivelLogro(
            UUID resultadoAprendizajeId, UUID semestreId, NivelLogro nivelLogro);
}
