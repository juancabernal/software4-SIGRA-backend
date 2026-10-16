package co.edu.uco.sigra.estudiantes.exception;

public class PermisoInsuficienteException extends RuntimeException {
    public PermisoInsuficienteException() {
        super("No tiene permisos suficientes para realizar esta acción.");
    }
}
