package co.edu.uco.sigra.resultadosaprendizaje.service;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeActualizarRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeCrearRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeResponseDTO;

import java.util.List;
import java.util.UUID;


public interface ResultadoAprendizajeService {

    ResultadoAprendizajeResponseDTO registrar(UUID asignaturaId, ResultadoAprendizajeCrearRequestDTO dto);

    List<ResultadoAprendizajeResponseDTO> consultarPorAsignatura(UUID asignaturaId, EstadoRegistro estado);

    List<ResultadoAprendizajeResponseDTO> consultarTodos(EstadoRegistro estado);

    ResultadoAprendizajeResponseDTO consultarPorId(UUID id);

    ResultadoAprendizajeResponseDTO actualizarDescripcion(UUID id, ResultadoAprendizajeActualizarRequestDTO dto);

    ResultadoAprendizajeResponseDTO inactivar(UUID id);

    ResultadoAprendizajeResponseDTO reactivar(UUID id);
}
