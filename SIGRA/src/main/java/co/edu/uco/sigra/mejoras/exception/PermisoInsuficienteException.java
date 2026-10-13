package co.edu.uco.sigra.mejoras.exception;

/** El rol no permite la operación (403). */
public class PermisoInsuficienteException extends RuntimeException {
    public PermisoInsuficienteException() {
        super("No tiene permisos suficientes para realizar esta acción.");
    }
}
