package co.edu.uco.sigra.asignaturas.repository;

import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Soporte de lectura para la confirmación adicional al inactivar una asignatura: cuenta las
 * evaluaciones en un (asignatura, semestre, estado) dados. Es un repositorio nuestro, separado
 * de EvaluacionRepository (de otro integrante del equipo), sobre la misma entidad: Spring Data
 * admite varios repositorios sobre una entidad, y así no se toca el suyo ni se crea una
 * dependencia hacia EvaluacionService desde AsignaturaServiceImpl más allá de la ya existente
 * para consultarSemestreActivo (evitar un ciclo de beans), igual que ya se hizo con
 * ConclusionDiagnosticoRepository.
 */
@Repository
public interface EvaluacionAbiertaRepository extends JpaRepository<Evaluacion, UUID> {

    long countByResultadoAprendizaje_Asignatura_IdAndSemestre_IdAndEstado(
            UUID asignaturaId, UUID semestreId, EstadoEvaluacion estado);
}
