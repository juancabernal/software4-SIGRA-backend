package co.edu.uco.sigra.asignaturas.dto;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;

import java.util.UUID;

public record AsignaturaResponseDTO(
        UUID id,
        String codigo,
        String nombre,
        UUID programaId,
        String programaNombre,
        EstadoAsignatura estado,
        long cantidadRa
) {}
