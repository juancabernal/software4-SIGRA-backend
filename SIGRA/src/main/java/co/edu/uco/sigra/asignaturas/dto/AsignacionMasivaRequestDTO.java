package co.edu.uco.sigra.asignaturas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Asigna cada profesor de la lista a cada asignatura de la lista, en una sola operación (RF-07). */
@Schema(name = "AsignacionMasivaRequest",
        description = "Profesores y asignaturas a asignar entre sí. El servicio genera el producto "
                + "cartesiano: cada profesor queda asignado a cada asignatura")
public record AsignacionMasivaRequestDTO(
        @Schema(description = "Identificadores (UUID) de los profesores a asignar",
                example = "[\"b3d4e5f6-7a8b-49c0-91d2-3e4f5a6b7c8d\"]")
        @NotEmpty(message = "Debe indicar al menos un profesor")
        List<@NotNull(message = "Los identificadores de profesor no pueden ser nulos") UUID> profesorIds,

        @Schema(description = "Identificadores (UUID) de las asignaturas a asignar",
                example = "[\"c4e5f6a7-8b9c-4d01-a2e3-4f5a6b7c8d9e\"]")
        @NotEmpty(message = "Debe indicar al menos una asignatura")
        List<@NotNull(message = "Los identificadores de asignatura no pueden ser nulos") UUID> asignaturaIds
) {}
