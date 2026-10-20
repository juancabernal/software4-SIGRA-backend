package co.edu.uco.sigra.calificaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EvaluacionCalificableDTO(
        UUID id,
        String nombre,
        LocalDate fechaTentativa,
        BigDecimal porcentaje,
        String estado
) {}