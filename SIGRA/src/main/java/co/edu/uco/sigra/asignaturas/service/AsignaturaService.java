package co.edu.uco.sigra.asignaturas.service;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaUpdateDTO;

import java.util.UUID;

public interface AsignaturaService {
    AsignaturaResponseDTO registrarAsignatura(AsignaturaRequestDTO dto);
    AsignaturaResponseDTO obtenerAsignatura(UUID id);
    AsignaturaResponseDTO modificarAsignatura(UUID id, AsignaturaUpdateDTO dto);
    AsignaturaResponseDTO activar(UUID id);

    /**
     * Inactiva una asignatura ACTIVA e inactiva en cascada sus resultados de aprendizaje activos.
     * No afecta matrículas, evaluaciones ni calificaciones.
     */
    AsignaturaResponseDTO inactivar(UUID id);
}
