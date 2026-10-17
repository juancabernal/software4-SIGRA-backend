package co.edu.uco.sigra.estadisticas.service;

import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.estadisticas.dto.ConclusionFiltroDTO;
import co.edu.uco.sigra.estadisticas.dto.ConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.GenerarConclusionResponseDTO;

import java.util.List;
import java.util.UUID;

/**
 * Conclusiones automáticas (RF-16): alertas de desempeño insuficiente por (RA, semestre).
 * Métodos del diagrama de clases: generarConclusion(idRA, idSemestre) y consultarConclusiones(filtro).
 */
public interface ConclusionService {

    /**
     * Evalúa el (RA, semestre) y genera una conclusión Vigente si corresponde (RF-16). La dispara
     * normalmente el recálculo de estadísticas del RA; mientras ese módulo no exista, se puede
     * invocar directamente. {@code generada=false} en la respuesta no es un error.
     */
    GenerarConclusionResponseDTO generarConclusion(UUID resultadoAprendizajeId, UUID semestreId,
                                                   UsuarioPrincipal principal);

    /** El PROFESOR solo ve las conclusiones de las asignaturas donde tiene asignación docente activa. */
    List<ConclusionResponseDTO> consultarConclusiones(ConclusionFiltroDTO filtro, UsuarioPrincipal principal);

    ConclusionResponseDTO consultarPorId(UUID id, UsuarioPrincipal principal);
}
