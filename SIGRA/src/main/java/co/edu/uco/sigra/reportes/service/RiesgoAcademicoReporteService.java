package co.edu.uco.sigra.reportes.service;

import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.reportes.dto.RiesgoAcademicoReporteResponseDTO;

import java.util.UUID;

/** Reporte R6: estudiantes con RA no alcanzados (riesgo académico). Solo lectura. */
public interface RiesgoAcademicoReporteService {

    default RiesgoAcademicoReporteResponseDTO consultar(UUID semestreId, UUID programaId, UUID asignaturaId,
                                                        Integer minRaNoAlcanzados, UsuarioPrincipal principal) {
        return consultar(semestreId, programaId, null, asignaturaId, minRaNoAlcanzados, principal);
    }

    /**
     * Igual que la consulta sin facultad, con un filtro opcional por la facultad del programa (incidencia
     * 0000036). Se combina con AND; una facultad sin datos, inexistente o distinta de la del programa pedido
     * da la lista vacía (no 404).
     */
    RiesgoAcademicoReporteResponseDTO consultar(UUID semestreId, UUID programaId, UUID facultadId, UUID asignaturaId,
                                                Integer minRaNoAlcanzados, UsuarioPrincipal principal);
}
