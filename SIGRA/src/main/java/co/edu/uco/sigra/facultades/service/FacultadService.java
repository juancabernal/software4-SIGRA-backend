package co.edu.uco.sigra.facultades.service;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.facultades.dto.FacultadRequestDTO;
import co.edu.uco.sigra.facultades.dto.FacultadResponseDTO;

import java.util.List;
import java.util.UUID;

public interface FacultadService {

    /** Facultades ordenadas por nombre. Los filtros son opcionales; el nombre se compara por contenido, sin tildes. */
    List<FacultadResponseDTO> consultar(String nombre, EstadoRegistro estado);

    FacultadResponseDTO consultarPorId(UUID id);

    FacultadResponseDTO registrar(FacultadRequestDTO dto);

    FacultadResponseDTO actualizarNombre(UUID id, FacultadRequestDTO dto);

    FacultadResponseDTO inactivar(UUID id);
}
