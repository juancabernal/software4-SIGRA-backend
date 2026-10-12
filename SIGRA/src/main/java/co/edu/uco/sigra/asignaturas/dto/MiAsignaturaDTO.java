package co.edu.uco.sigra.asignaturas.dto;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;

import java.util.List;
import java.util.UUID;

/**
 * Una asignatura vista desde «mis asignaturas» del Profesor o del Estudiante.
 *
 * @param cantidadRa          resultados de aprendizaje ACTIVOS
 * @param cantidadEstudiantes matrículas ACTIVAS
 * @param profesores          nombres de los profesores con asignación docente ACTIVA, ordenados
 */
public record MiAsignaturaDTO(
        UUID id,
        String codigo,
        String nombre,
        UUID programaId,
        String programaNombre,
        EstadoAsignatura estado,
        long cantidadRa,
        long cantidadEstudiantes,
        List<String> profesores
) {}
