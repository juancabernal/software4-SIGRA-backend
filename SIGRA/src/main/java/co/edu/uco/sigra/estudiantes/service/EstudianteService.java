package co.edu.uco.sigra.estudiantes.service;

import co.edu.uco.sigra.estudiantes.dto.EstudianteRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteResponseDTO;

import java.util.List;
import java.util.UUID;

public interface EstudianteService {
    EstudianteResponseDTO registrar(EstudianteRequestDTO dto);
    List<EstudianteResponseDTO> consultar(String filtro);
    EstudianteResponseDTO consultarPorId(UUID id);
    EstudianteResponseDTO modificar(UUID id, EstudianteRequestDTO dto);
    void inactivar(UUID id);
}
