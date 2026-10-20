package co.edu.uco.sigra.resultadosaprendizaje.service;

import co.edu.uco.sigra.resultadosaprendizaje.dto.RendimientoAsignaturaResponseDTO;

import java.util.UUID;

public interface RendimientoResultadoAprendizajeService {

    /**
     * Calcula el rendimiento de los RA activos de la asignatura en el semestre activo.
     *
     * @throws co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException si la asignatura no existe
     */
    RendimientoAsignaturaResponseDTO consultarPorAsignatura(UUID asignaturaId);
}
