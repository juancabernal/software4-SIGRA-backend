package co.edu.uco.sigra.asignaturas.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Asigna cada profesor de la lista a cada asignatura de la lista, en una sola operación (RF-07). */
public record AsignacionMasivaRequestDTO(
        @NotEmpty(message = "Debe indicar al menos un profesor")
        List<@NotNull(message = "Los identificadores de profesor no pueden ser nulos") UUID> profesorIds,

        @NotEmpty(message = "Debe indicar al menos una asignatura")
        List<@NotNull(message = "Los identificadores de asignatura no pueden ser nulos") UUID> asignaturaIds
) {}
