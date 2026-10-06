package co.edu.uco.sigra.programas.service;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;

import java.util.List;

public interface ProgramaService {

    /** Programas ordenados por nombre; si {@code estado} es null devuelve todos. */
    List<ProgramaResponseDTO> consultar(EstadoRegistro estado);
}
