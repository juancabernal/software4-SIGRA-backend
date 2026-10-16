package co.edu.uco.sigra.calificaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CalificacionResponseDTO(
        UUID id,
        UUID evaluacionId,
        UUID matriculaId,
        UUID estudianteId,
        String estudianteNombre,
        BigDecimal nota,
        String observacion,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaModificacion,
        boolean fueraDeFecha
) {}