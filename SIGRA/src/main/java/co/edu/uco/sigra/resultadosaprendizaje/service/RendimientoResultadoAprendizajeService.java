package co.edu.uco.sigra.resultadosaprendizaje.service;

import co.edu.uco.sigra.resultadosaprendizaje.dto.RendimientoAsignaturaResponseDTO;

import java.util.UUID;

public interface RendimientoResultadoAprendizajeService {

    /**
     * Calcula el rendimiento de los RA activos de la asignatura en el semestre indicado, o en el
     * semestre activo cuando {@code semestreId} es nulo.
     *
     * @throws co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException si la asignatura no existe
     * @throws co.edu.uco.sigra.semestres.exception.SemestreNoEncontradoException si se indica un semestre inexistente
     */
    RendimientoAsignaturaResponseDTO consultarPorAsignatura(UUID asignaturaId, UUID semestreId);
}
