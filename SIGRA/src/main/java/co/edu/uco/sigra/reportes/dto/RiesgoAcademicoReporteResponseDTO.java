package co.edu.uco.sigra.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "RiesgoAcademicoReporte",
        description = "Reporte R6: estudiantes con RA no alcanzados (riesgo académico)")
public record RiesgoAcademicoReporteResponseDTO(
        @Schema(description = "Semestre del reporte. Nulo si no hay semestre activo y no se pidió otro",
                nullable = true)
        UUID semestreId,
        @Schema(description = "Código del semestre", example = "2026-2", nullable = true)
        String semestreCodigo,
        @Schema(description = "Cantidad de filas estudiante×asignatura en riesgo tras filtros")
        int totalEstudiantesEnRiesgo,
        @Schema(description = "Filas ordenadas por cantidad de RA no alcanzados (desc) y nombre")
        List<EstudianteRiesgoDTO> estudiantes,
        @Schema(description = "«Sin datos disponibles» cuando la lista está vacía; nulo en otro caso",
                nullable = true)
        String mensaje
) {
}
