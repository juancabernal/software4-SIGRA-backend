package co.edu.uco.sigra.estadisticas.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConclusionResponseDTO(
        UUID id,
        UUID resultadoAprendizajeId,
        String resultadoAprendizajeCodigo,
        UUID asignaturaId,
        UUID semestreId,
        String semestreCodigo,
        String mensaje,
        boolean tieneDatosSuficientes,
        LocalDateTime fechaGeneracion,
        double porcentajeInsuficiente,
        String estado
) {
}
