package co.edu.uco.sigra.calificaciones.service;

import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.calificaciones.dto.CalificacionModificarRequestDTO;
import co.edu.uco.sigra.calificaciones.dto.CalificacionRequestDTO;
import co.edu.uco.sigra.calificaciones.dto.CalificacionResponseDTO;
import co.edu.uco.sigra.calificaciones.dto.EvaluacionCalificableDTO;
import co.edu.uco.sigra.calificaciones.dto.HistorialCalificacionDTO;
import co.edu.uco.sigra.calificaciones.dto.PlanillaCalificacionesDTO;

import java.util.List;
import java.util.UUID;

public interface CalificacionService {

    List<EvaluacionCalificableDTO> listarEvaluaciones(UUID resultadoAprendizajeId, UUID semestreId);

    PlanillaCalificacionesDTO consultarPlanilla(UUID evaluacionId, UsuarioPrincipal usuario);

    CalificacionResponseDTO registrar(UUID evaluacionId, CalificacionRequestDTO dto, UsuarioPrincipal usuario);

    CalificacionResponseDTO modificar(UUID calificacionId, CalificacionModificarRequestDTO dto,
                                      UsuarioPrincipal usuario);

    List<HistorialCalificacionDTO> consultarHistorial(UUID calificacionId, UsuarioPrincipal usuario);
}