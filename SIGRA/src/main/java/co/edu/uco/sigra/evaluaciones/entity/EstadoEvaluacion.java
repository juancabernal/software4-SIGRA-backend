package co.edu.uco.sigra.evaluaciones.entity;

/**
 * Estado de una {@link Evaluacion} (RF-10c). Eliminación lógica: una evaluación INACTIVA no admite
 * nuevas calificaciones, pero conserva las ya registradas.
 */
public enum EstadoEvaluacion {
    ACTIVA,
    INACTIVA
}
