package co.edu.uco.sigra.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "MatriculasProgramaSemestreReporte",
        description = "Reporte R2: matrículas agrupadas por programa académico y semestre")
public record MatriculasProgramaSemestreReporteResponseDTO(
        UUID semestreId,
        UUID programaId,
        UUID facultadId,
        MatriculasProgramaSemestreReporteResumenDTO resumen,
        List<MatriculasProgramaSemestreReporteFilaDTO> filas
) {
}
