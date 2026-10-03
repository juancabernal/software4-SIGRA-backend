package co.edu.uco.sigra.resultadosaprendizaje.dto;

import java.util.UUID;

/**
 * Datos de salida de un resultado de aprendizaje (RF-06b: código, descripción y estado).
 * Se expone el id de la asignatura, no la entidad completa.
 */
public record ResultadoAprendizajeResponseDTO(
        UUID id,
        UUID asignaturaId,
        String codigo,
        String descripcion,
        String estado
) {
}
