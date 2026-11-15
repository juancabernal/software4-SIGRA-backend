package co.edu.uco.sigra.reportes.dto;

public record MatriculasProgramaSemestreReporteResumenDTO(
        long totalMatriculas,
        long activas,
        long desvinculadas,
        long estudiantesDistintos,
        long programas,
        long semestres
) {
}
