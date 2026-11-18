package co.edu.uco.sigra.reportes.service;

import co.edu.uco.sigra.reportes.dto.MatriculasProgramaSemestreReporteResponseDTO;

import java.util.UUID;

public interface MatriculasProgramaSemestreReporteService {

    MatriculasProgramaSemestreReporteResponseDTO consultar(UUID semestreId, UUID programaId, UUID facultadId);
}
