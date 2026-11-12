package co.edu.uco.sigra.reportes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Severidad del riesgo académico según cuántos RA no alcanzó el estudiante en la asignatura. */
@Schema(description = "ATENCION = 1 RA; RIESGO = 2; ALTO_RIESGO = 3 o más")
public enum SeveridadRiesgo {
    ATENCION,
    RIESGO,
    ALTO_RIESGO
}
