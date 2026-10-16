package co.edu.uco.sigra.calificaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record HistorialCalificacionDTO(
        UUID id,
        String tipoEvento,
        BigDecimal notaAnterior,
        BigDecimal notaNueva,
        String observacionAnterior,
        String observacionNueva,
        boolean fueraDeFecha,
        UUID usuarioId,
        String rolUsuario,
        LocalDateTime fechaEvento
) {}