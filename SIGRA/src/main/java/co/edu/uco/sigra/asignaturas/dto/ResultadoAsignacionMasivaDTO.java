package co.edu.uco.sigra.asignaturas.dto;

import java.util.List;

/** Resumen de una asignación masiva (RF-07): qué se creó, qué se omitió por duplicado y qué se rechazó por inactivo. */
public record ResultadoAsignacionMasivaDTO(
        List<AsignacionDocenteResponseDTO> creadas,
        List<AsignacionNoRealizadaDTO> omitidas,
        List<AsignacionNoRealizadaDTO> rechazadas
) {}
