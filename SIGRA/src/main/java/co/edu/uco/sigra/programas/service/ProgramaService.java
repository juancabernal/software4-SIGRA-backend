package co.edu.uco.sigra.programas.service;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.programas.dto.ProgramaRequestDTO;
import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.dto.ProgramaUpdateDTO;

import java.util.List;
import java.util.UUID;

public interface ProgramaService {

    /** Programas ordenados por nombre; si {@code estado} es null devuelve todos. */
    List<ProgramaResponseDTO> consultar(EstadoRegistro estado);

    /** Filtros opcionales: nombre y código se comparan por contenido, sin distinguir mayúsculas. */
    List<ProgramaResponseDTO> consultar(String nombre, String codigo, EstadoRegistro estado);

    ProgramaResponseDTO consultarPorId(UUID id);

    ProgramaResponseDTO registrar(ProgramaRequestDTO dto);

    ProgramaResponseDTO actualizarNombre(UUID id, ProgramaUpdateDTO dto);

    ProgramaResponseDTO inactivar(UUID id);
}
