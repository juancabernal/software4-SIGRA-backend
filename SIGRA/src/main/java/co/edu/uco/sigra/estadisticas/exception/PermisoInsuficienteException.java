package co.edu.uco.sigra.estadisticas.exception;

/** El rol no permite la operación, o la conclusión no está en el alcance del profesor (403). */
public class PermisoInsuficienteException extends RuntimeException {
    public PermisoInsuficienteException() {
        super("No tiene permisos suficientes para realizar esta acción.");
    }
}
