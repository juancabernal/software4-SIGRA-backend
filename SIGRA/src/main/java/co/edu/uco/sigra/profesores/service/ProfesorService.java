package co.edu.uco.sigra.profesores.service;

import co.edu.uco.sigra.profesores.dto.ProfesorRequestDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorResponseDTO;

import java.util.List;
import java.util.UUID;

public interface ProfesorService {
    ProfesorResponseDTO registrarProfesor(ProfesorRequestDTO dto);
    List<ProfesorResponseDTO> consultar(String filtro);
    ProfesorResponseDTO modificarProfesor(UUID id, ProfesorRequestDTO dto);
    void inactivar(UUID id);
}
