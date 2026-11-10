package co.edu.uco.sigra.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "AsignaturaRendimientoReporte", description = "RA de una asignatura dentro del reporte R1")
public record AsignaturaRendimientoReporteDTO(
        @Schema(description = "Identificador de la asignatura", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID asignaturaId,
        @Schema(description = "Código de la asignatura", example = "SIS101")
        String codigo,
        @Schema(description = "Nombre de la asignatura", example = "Programación")
        String nombre,
        @Schema(description = "Identificador del programa académico de la asignatura")
        UUID programaId,
        @Schema(description = "Nombre del programa académico", example = "Ingeniería de Sistemas")
        String programaNombre,
        @Schema(description = "RA activos de la asignatura, ordenados por código")
        List<RaRendimientoReporteDTO> resultados
) {
}
