package co.edu.uco.sigra.semestres.service;

import co.edu.uco.sigra.semestres.dto.SemestreFechaFinRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreResponseDTO;

import java.util.List;

public interface SemestreService {
    SemestreResponseDTO crear(SemestreRequestDTO dto);
    List<SemestreResponseDTO> listar();
    SemestreResponseDTO consultarPorCodigo(String codigo);
    SemestreResponseDTO extenderFechaFin(String codigo, SemestreFechaFinRequestDTO dto);
}
