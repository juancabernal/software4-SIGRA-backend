package co.edu.uco.sigra.mejoras.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AccionMejoraResponseDTO(
        UUID id,
        String nombre,
        String descripcion,
        LocalDateTime fechaPropuesta,
        UUID resultadoAprendizajeId,
        String resultadoAprendizajeCodigo,
        UUID asignaturaId,
        UUID conclusionOrigenId,
        UUID evaluacionGeneradaId,
        boolean convertida
) {
}
