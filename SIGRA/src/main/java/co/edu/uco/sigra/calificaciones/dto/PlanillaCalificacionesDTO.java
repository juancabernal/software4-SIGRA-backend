package co.edu.uco.sigra.calificaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PlanillaCalificacionesDTO(
        UUID evaluacionId,
        String evaluacionNombre,
        LocalDate fechaTentativa,
        BigDecimal porcentaje,
        String estadoEvaluacion,
        UUID semestreId,
        String semestreCodigo,
        UUID asignaturaId,
        String asignaturaCodigo,
        String asignaturaNombre,
        String resultadoAprendizajeCodigo,
        int totalEstudiantes,
        int totalCalificados,
        List<FilaPlanillaDTO> filas
) {}