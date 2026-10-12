package co.edu.uco.sigra.mejoras.dto;

import java.util.UUID;

/** Filtro de consulta de acciones de mejora: todos los campos son opcionales. */
public record AccionMejoraFiltroDTO(
        UUID resultadoAprendizajeId,
        UUID asignaturaId,
        UUID semestreId,
        Boolean convertida
) {
}
