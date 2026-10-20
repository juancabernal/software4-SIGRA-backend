package co.edu.uco.sigra.asignaturas.dto;

import java.util.UUID;

/** Un par profesor-asignatura que no se asignó, con el motivo (RF-07): ya estaba asignado o alguno de los dos está inactivo. */
public record AsignacionNoRealizadaDTO(
        UUID profesorId,
        UUID asignaturaId,
        String motivo
) {}
