package co.edu.uco.sigra.asignaturas.exception;

/** Falta un token válido de un usuario activo (401). */
public class AutenticacionRequeridaException extends RuntimeException {
    public AutenticacionRequeridaException() {
        super("Debe iniciar sesión para acceder a este recurso.");
    }
}
