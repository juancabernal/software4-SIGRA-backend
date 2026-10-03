package co.edu.uco.sigra.asignaturas.dto;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;

import java.util.UUID;

/** Criterios opcionales de consulta de asignaturas; los que vengan informados se combinan con AND. */
public record AsignaturaFiltroDTO(
        String texto,
        UUID programaId,
        EstadoAsignatura estado,
        Integer raMin,
        Integer raMax
) {}
