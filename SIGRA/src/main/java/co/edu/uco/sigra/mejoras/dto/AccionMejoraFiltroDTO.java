package co.edu.uco.sigra.mejoras.dto;

import java.util.UUID;

/** Filtro de consulta de acciones de mejora: los tres campos son opcionales. */
public record AccionMejoraFiltroDTO(
        UUID resultadoAprendizajeId,
        UUID asignaturaId,
        Boolean convertida
) {
}
