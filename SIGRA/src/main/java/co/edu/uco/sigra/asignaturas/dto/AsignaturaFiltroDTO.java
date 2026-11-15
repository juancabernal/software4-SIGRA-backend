package co.edu.uco.sigra.asignaturas.dto;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;

import java.util.UUID;

/**
 * Criterios opcionales de consulta de asignaturas; los que vengan informados se combinan con AND.
 * El texto llega recortado (vacío equivale a no filtrar). {@code facultadId} filtra por la facultad
 * del programa de la asignatura (incidencia 0000036); {@code semestreId}, por la actividad en un
 * semestre (incidencia 0000044).
 */
public record AsignaturaFiltroDTO(
        String texto,
        UUID programaId,
        UUID facultadId,
        EstadoAsignatura estado,
        Integer raMin,
        Integer raMax,
        UUID semestreId
) {
    public AsignaturaFiltroDTO {
        texto = ReglasEntrada.normalizarTexto(texto);
    }

    /** Sin filtro de facultad (forma de la incidencia 0000044). */
    public AsignaturaFiltroDTO(String texto, UUID programaId, EstadoAsignatura estado,
                               Integer raMin, Integer raMax, UUID semestreId) {
        this(texto, programaId, null, estado, raMin, raMax, semestreId);
    }

    /** Sin filtros de facultad ni de semestre: conserva la forma original del filtro. */
    public AsignaturaFiltroDTO(String texto, UUID programaId, EstadoAsignatura estado,
                               Integer raMin, Integer raMax) {
        this(texto, programaId, null, estado, raMin, raMax, null);
    }
}
