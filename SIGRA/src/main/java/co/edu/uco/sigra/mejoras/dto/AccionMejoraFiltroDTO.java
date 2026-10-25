package co.edu.uco.sigra.mejoras.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Filtro de consulta de acciones de mejora: todos los campos son opcionales. */
@Schema(name = "AccionMejoraFiltro", description = "Filtros de la consulta de acciones de mejora; todos opcionales")
public record AccionMejoraFiltroDTO(
        @Schema(description = "Filtra por el identificador (UUID) del resultado de aprendizaje", example = "6f7a8b9c-0d1e-4f2a-3b4c-5d6e7f8a9b0c")
        UUID resultadoAprendizajeId,

        @Schema(description = "Filtra por el identificador (UUID) de la asignatura", example = "3c4d5e6f-7a8b-4c9d-0e1f-2a3b4c5d6e7f")
        UUID asignaturaId,

        @Schema(description = "Filtra por el identificador (UUID) del semestre de la conclusión origen", example = "7a8b9c0d-1e2f-4a3b-4c5d-6e7f8a9b0c1d")
        UUID semestreId,

        @Schema(description = "Filtra por si la acción ya fue convertida en evaluación", example = "false")
        Boolean convertida
) {
}
