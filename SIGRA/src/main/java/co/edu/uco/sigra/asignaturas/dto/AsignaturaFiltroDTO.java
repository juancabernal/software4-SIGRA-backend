package co.edu.uco.sigra.asignaturas.dto;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;

import java.util.UUID;

/**
 * Criterios opcionales de consulta de asignaturas; los que vengan informados se combinan con AND.
 * El texto llega recortado (vacío equivale a no filtrar). {@code facultadId} filtra por la facultad
 * del programa de la asignatura (incidencia 0000036).
 */
public record AsignaturaFiltroDTO(
        String texto,
        UUID programaId,
        UUID facultadId,
        EstadoAsignatura estado,
        Integer raMin,
        Integer raMax
) {
    public AsignaturaFiltroDTO {
        texto = ReglasEntrada.normalizarTexto(texto);
    }

    /** Sin filtro de facultad: conserva la forma anterior del filtro. */
    public AsignaturaFiltroDTO(String texto, UUID programaId, EstadoAsignatura estado, Integer raMin, Integer raMax) {
        this(texto, programaId, null, estado, raMin, raMax);
    }
}
