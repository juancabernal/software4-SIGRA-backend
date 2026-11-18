package co.edu.uco.sigra.reportes.service;

import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.reportes.dto.TrazabilidadUsoProfesorResponseDTO;
import co.edu.uco.sigra.reportes.dto.TrazabilidadUsoReporteResponseDTO;

import java.time.LocalDate;
import java.util.UUID;

public interface TrazabilidadUsoReporteService {

    TrazabilidadUsoReporteResponseDTO consultarGeneral(LocalDate desde, LocalDate hasta, UUID semestreId,
                                                       RolUsuario rol);

    TrazabilidadUsoProfesorResponseDTO consultarProfesor(LocalDate desde, LocalDate hasta, UUID semestreId,
                                                         UsuarioPrincipal principal);
}
