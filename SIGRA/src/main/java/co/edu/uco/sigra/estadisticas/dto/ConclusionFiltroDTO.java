package co.edu.uco.sigra.estadisticas.dto;

import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;

import java.util.UUID;

/** Filtro de consulta de conclusiones (consultarConclusiones del diagrama de clases). */
public record ConclusionFiltroDTO(
        UUID resultadoAprendizajeId,
        UUID semestreId,
        UUID asignaturaId,
        EstadoConclusion estado
) {
}
