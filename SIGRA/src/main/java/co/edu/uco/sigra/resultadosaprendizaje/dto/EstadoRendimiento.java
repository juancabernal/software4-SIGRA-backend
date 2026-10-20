package co.edu.uco.sigra.resultadosaprendizaje.dto;

/**
 * Estado del rendimiento de un resultado de aprendizaje (RF-14 / RF-15).
 */
public enum EstadoRendimiento {
    /** El RA todavía no tiene calificaciones. */
    SIN_EVALUAR,
    /** Hay calificaciones, pero ningún estudiante alcanza el mínimo de 3 (RF-15). */
    DATOS_INSUFICIENTES,
    /** Hay al menos un estudiante con nivel calculado; se informa el nivel del RA. */
    CON_NIVEL
}
