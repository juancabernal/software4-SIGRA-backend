package co.edu.uco.sigra.estudiantes.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nivel del último cálculo oficial; SIN_EVALUAR es un valor de salida")
public enum MiNivelLogro {
    BAJO,
    MEDIO,
    ALTO,
    SOBRESALIENTE,
    SIN_EVALUAR
}
