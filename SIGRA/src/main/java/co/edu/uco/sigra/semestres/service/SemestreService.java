package co.edu.uco.sigra.semestres.service;

import co.edu.uco.sigra.semestres.dto.SemestreFechaFinRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreResponseDTO;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;

import java.util.List;
import java.util.UUID;

public interface SemestreService {
    SemestreResponseDTO crear(SemestreRequestDTO dto);
    List<SemestreResponseDTO> listar();

    /** Lista con búsqueda parcial por código (sin distinguir mayúsculas) y filtro por estado; ambos opcionales. */
    List<SemestreResponseDTO> listar(String codigo, EstadoSemestre estado);
    /** Semestres en los que el profesor tiene asignaturas asignadas, del más reciente al más antiguo. */
    List<SemestreResponseDTO> listarDelProfesor(UUID profesorId);
    SemestreResponseDTO consultarPorCodigo(String codigo);
    SemestreResponseDTO extenderFechaFin(String codigo, SemestreFechaFinRequestDTO dto);
}
