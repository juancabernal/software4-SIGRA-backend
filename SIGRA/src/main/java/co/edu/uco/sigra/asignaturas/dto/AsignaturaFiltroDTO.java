package co.edu.uco.sigra.asignaturas.dto;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;

import java.util.UUID;

/**
 * Criterios opcionales de consulta de asignaturas; los que vengan informados se combinan con AND.
 * El texto llega recortado (vacío equivale a no filtrar).
 */
public record AsignaturaFiltroDTO(
        String texto,
        UUID programaId,
        EstadoAsignatura estado,
        Integer raMin,
        Integer raMax
) {
    public AsignaturaFiltroDTO {
        texto = ReglasEntrada.normalizarTexto(texto);
    }
}
