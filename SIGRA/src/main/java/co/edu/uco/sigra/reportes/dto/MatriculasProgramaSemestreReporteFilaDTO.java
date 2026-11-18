package co.edu.uco.sigra.reportes.dto;

import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

public record MatriculasProgramaSemestreReporteFilaDTO(
        UUID semestreId,
        String semestreCodigo,
        LocalDate semestreFechaInicio,
        EstadoSemestre semestreEstado,
        UUID programaId,
        String programaCodigo,
        String programaNombre,
        String facultadNombre,
        long totalMatriculas,
        long activas,
        long desvinculadas,
        long estudiantes,
        long asignaturas
) {
}
