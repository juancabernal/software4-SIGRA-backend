package co.edu.uco.sigra.reportes.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Actividad del propio profesor autenticado. */
public record ActividadPropiaDTO(
        long totalEventos,
        LocalDateTime ultimaActividad,
        List<ConteoAccionDTO> acciones
) {
}
