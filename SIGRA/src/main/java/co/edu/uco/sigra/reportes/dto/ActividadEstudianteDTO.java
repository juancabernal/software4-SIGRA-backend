package co.edu.uco.sigra.reportes.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Actividad de un estudiante matriculado en asignaturas del profesor (sin documento ni detalle de eventos). */
public record ActividadEstudianteDTO(
        UUID estudianteId,
        String nombreCompleto,
        String correoInstitucional,
        List<String> asignaturas,
        long ingresos,
        long totalEventos,
        LocalDateTime ultimaActividad
) {
}
