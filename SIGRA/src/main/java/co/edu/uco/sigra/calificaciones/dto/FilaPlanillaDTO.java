package co.edu.uco.sigra.calificaciones.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FilaPlanillaDTO(
        UUID matriculaId,
        UUID estudianteId,
        String numeroDocumento,
        String nombreCompleto,
        boolean matriculaActiva,
        UUID calificacionId,
        BigDecimal nota,
        String observacion,
        Boolean fueraDeFecha,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaModificacion
) {}