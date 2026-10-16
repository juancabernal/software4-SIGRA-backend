package co.edu.uco.sigra.asignaturas.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** Asigna cada profesor de la lista a cada asignatura de la lista, en una sola operación (RF-07). */
public record AsignacionMasivaRequestDTO(
        @NotEmpty(message = "Debe indicar al menos un profesor")
        List<UUID> profesorIds,

        @NotEmpty(message = "Debe indicar al menos una asignatura")
        List<UUID> asignaturaIds
) {}
