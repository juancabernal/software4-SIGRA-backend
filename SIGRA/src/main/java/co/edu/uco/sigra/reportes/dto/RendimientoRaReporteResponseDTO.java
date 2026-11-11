package co.edu.uco.sigra.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "RendimientoRaReporte", description = "Reporte R1: rendimiento académico por RA y asignatura en un semestre")
public record RendimientoRaReporteResponseDTO(
        @Schema(description = "Semestre del reporte. Nulo si no hay semestre activo y no se pidió otro", nullable = true)
        UUID semestreId,
        @Schema(description = "Código del semestre", example = "2026-2", nullable = true)
        String semestreCodigo,
        @Schema(description = "Asignaturas con al menos un RA activo, ordenadas por código. Vacío si no hay datos")
        List<AsignaturaRendimientoReporteDTO> asignaturas,
        @Schema(description = "«Sin datos disponibles» cuando la lista de asignaturas está vacía; nulo en otro caso", nullable = true)
        String mensaje
) {
}
