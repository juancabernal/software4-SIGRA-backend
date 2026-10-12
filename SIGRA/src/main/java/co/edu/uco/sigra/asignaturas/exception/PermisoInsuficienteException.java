package co.edu.uco.sigra.asignaturas.exception;

/** El rol del usuario no permite la operación o la asignatura no está en su alcance (403). */
public class PermisoInsuficienteException extends RuntimeException {
    public PermisoInsuficienteException() {
        super("No tiene permisos suficientes para realizar esta acción.");
    }
}
