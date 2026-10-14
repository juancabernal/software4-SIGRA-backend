package co.edu.uco.sigra.resultadosaprendizaje.dto;

import java.util.UUID;


public record ResultadoAprendizajeResponseDTO(
        UUID id,
        UUID asignaturaId,
        String codigo,
        String descripcion,
        String estado
) {
}
