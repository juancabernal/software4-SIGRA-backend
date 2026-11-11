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
    RendimientoRaReporteResponseDTO consultar(UUID semestreId, UUID programaId, UUID asignaturaId,
                                              UsuarioPrincipal principal);
}
