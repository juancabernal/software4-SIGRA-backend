package co.edu.uco.sigra.reportes.service;

import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.reportes.dto.RendimientoRaReporteResponseDTO;

import java.util.UUID;

/** Reporte R1: rendimiento académico por RA y asignatura. Solo lectura; reutiliza las estadísticas existentes. */
public interface RendimientoRaReporteService {

    /**
     * @param semestreId   semestre a consultar; nulo usa el semestre activo
     * @param programaId   filtro opcional por programa académico
     * @param asignaturaId filtro opcional por asignatura
     * @param principal    ADMINISTRADOR ve todo; PROFESOR solo sus asignaturas con asignación ACTIVA; otro rol, 403
     */
    default RendimientoRaReporteResponseDTO consultar(UUID semestreId, UUID programaId, UUID asignaturaId,
                                                      UsuarioPrincipal principal) {
        return consultar(semestreId, programaId, null, asignaturaId, principal);
    }

    /**
     * Igual que {@link #consultar(UUID, UUID, UUID, UsuarioPrincipal)} con un filtro opcional por la facultad
     * del programa (incidencia 0000036). Se combina con AND con los demás; una facultad sin datos, inexistente
     * o distinta de la del programa pedido da la lista vacía (no 404).
     */
    RendimientoRaReporteResponseDTO consultar(UUID semestreId, UUID programaId, UUID facultadId, UUID asignaturaId,
                                              UsuarioPrincipal principal);
}
