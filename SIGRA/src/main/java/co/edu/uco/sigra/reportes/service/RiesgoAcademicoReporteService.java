package co.edu.uco.sigra.reportes.service;

import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.reportes.dto.RiesgoAcademicoReporteResponseDTO;

import java.util.UUID;

/** Reporte R6: estudiantes con RA no alcanzados (riesgo académico). Solo lectura. */
public interface RiesgoAcademicoReporteService {

    RiesgoAcademicoReporteResponseDTO consultar(UUID semestreId, UUID programaId, UUID asignaturaId,
                                                Integer minRaNoAlcanzados, UsuarioPrincipal principal);
}
