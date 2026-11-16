package co.edu.uco.sigra.reportes.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** R4. El semestre corresponde a la conclusión que originó la acción. */
public record AccionesMejoraReporteResponseDTO(Resumen resumen, List<Fila> filas) {
    public record Resumen(long total, long convertidas, long sinConvertir, double porcentajeConversion) {}

    public record Fila(UUID id, String nombre, String descripcion, LocalDateTime fechaPropuesta,
                       UUID asignaturaId, String asignaturaCodigo, String asignaturaNombre,
                       UUID resultadoAprendizajeId, String resultadoAprendizajeCodigo,
                       UUID semestreId, String semestreCodigo, UUID evaluacionGeneradaId,
                       boolean convertida) {}
}
