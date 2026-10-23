package co.edu.uco.sigra.asignaturas.dto;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "AsignaturaResponse", description = "Asignatura con su estado y la cantidad de resultados de aprendizaje activos")
public record AsignaturaResponseDTO(
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

        @Schema(description = "Cantidad de resultados de aprendizaje ACTIVOS de la asignatura", example = "6")
        long cantidadRa
) {}
