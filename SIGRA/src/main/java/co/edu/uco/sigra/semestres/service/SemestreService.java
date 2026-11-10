package co.edu.uco.sigra.semestres.service;

import co.edu.uco.sigra.semestres.dto.SemestreFechaFinRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreResponseDTO;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;

import java.util.List;

public interface SemestreService {
    SemestreResponseDTO crear(SemestreRequestDTO dto);
    List<SemestreResponseDTO> listar();

    /** Lista con búsqueda parcial por código (sin distinguir mayúsculas) y filtro por estado; ambos opcionales. */
    List<SemestreResponseDTO> listar(String codigo, EstadoSemestre estado);
    SemestreResponseDTO consultarPorCodigo(String codigo);
    SemestreResponseDTO extenderFechaFin(String codigo, SemestreFechaFinRequestDTO dto);
}
