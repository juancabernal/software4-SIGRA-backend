package co.edu.uco.sigra.asignaturas.dto;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Una asignatura vista desde «mis asignaturas» del Profesor o del Estudiante.
 *
 * @param cantidadRa          resultados de aprendizaje ACTIVOS
 * @param cantidadEstudiantes matrículas ACTIVAS
 * @param profesores          nombres de los profesores con asignación docente ACTIVA, ordenados
 */
@Schema(name = "MiAsignatura", description = "Asignatura del profesor o estudiante autenticado, con sus indicadores")
public record MiAsignaturaDTO(
        @Schema(description = "Identificador (UUID) de la asignatura", example = "3c4d5e6f-7a8b-4c9d-0e1f-2a3b4c5d6e7f")
        UUID id,

        @Schema(description = "Código único de la asignatura", example = "MAT-401")
        String codigo,

        @Schema(description = "Nombre de la asignatura", example = "Cálculo Diferencial")
        String nombre,

        @Schema(description = "Identificador (UUID) del programa académico", example = "4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a")
        UUID programaId,

        @Schema(description = "Nombre del programa académico", example = "Ingeniería de Software")
        String programaNombre,

        @Schema(description = "Estado de la asignatura", allowableValues = {"BORRADOR", "ACTIVA", "INACTIVA"}, example = "ACTIVA")
        EstadoAsignatura estado,

        @Schema(description = "Cantidad de resultados de aprendizaje ACTIVOS", example = "6")
        long cantidadRa,

        @Schema(description = "Cantidad de matrículas ACTIVAS en la asignatura", example = "32")
        long cantidadEstudiantes,

        @Schema(description = "Nombres de los profesores con asignación docente ACTIVA, en orden alfabético")
        List<String> profesores
) {}
