package co.edu.uco.sigra.asignaturas.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Resumen de una asignación masiva (RF-07): qué se creó, qué se omitió por duplicado y qué se rechazó por inactivo. */
@Schema(name = "ResultadoAsignacionMasiva", description = "Resumen de una asignación masiva de profesores a asignaturas")
public record ResultadoAsignacionMasivaDTO(
        @Schema(description = "Pares creados, o reactivados si existía una fila INACTIVA previa")
        List<AsignacionDocenteResponseDTO> creadas,

        @Schema(description = "Pares que ya estaban ACTIVOS: se informan, no se crea una fila nueva")
        List<AsignacionNoRealizadaDTO> omitidas,

        @Schema(description = "Pares con el profesor o la asignatura inactivos: se informan, no se crea una fila")
        List<AsignacionNoRealizadaDTO> rechazadas
) {}
