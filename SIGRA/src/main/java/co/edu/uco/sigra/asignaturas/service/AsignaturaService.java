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
    AsignaturaResponseDTO inactivar(UUID id);
}
