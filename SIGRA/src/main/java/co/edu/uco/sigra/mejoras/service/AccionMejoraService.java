package co.edu.uco.sigra.mejoras.service;

import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraFiltroDTO;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraResponseDTO;
import co.edu.uco.sigra.mejoras.dto.ConvertirEvaluacionRequestDTO;
import co.edu.uco.sigra.mejoras.dto.RegistrarAccionMejoraRequestDTO;

import java.util.List;
import java.util.UUID;

/** Acciones de mejora (RF-17): se registran a partir de una conclusión VIGENTE, que pasa a ATENDIDA. */
public interface AccionMejoraService {

    AccionMejoraResponseDTO registrarAccionMejora(UUID conclusionId, RegistrarAccionMejoraRequestDTO datos,
                                                   IdentidadActual identidad);

    List<AccionMejoraResponseDTO> consultarAcciones(AccionMejoraFiltroDTO filtro);

    /** El PROFESOR solo ve las acciones de mejora de las asignaturas donde tiene asignación docente activa. */
    List<AccionMejoraResponseDTO> consultarAcciones(AccionMejoraFiltroDTO filtro, IdentidadActual identidad);

    AccionMejoraResponseDTO consultarPorId(UUID id);

    /**
     * RF-18: convierte la acción en una evaluación nueva del módulo evaluaciones (semestre activo), con el
     * nombre y la descripción de la acción y la fecha tentativa y el porcentaje indicados.
     */
    AccionMejoraResponseDTO convertirEnEvaluacion(UUID accionId, ConvertirEvaluacionRequestDTO datos,
                                                   IdentidadActual identidad);
}
