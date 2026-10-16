package co.edu.uco.sigra.estadisticas.exception;

/** Falta un token válido de un usuario activo (401). Mismo control temporal que el de asignaturas. */
public class AutenticacionRequeridaException extends RuntimeException {
    public AutenticacionRequeridaException() {
        super("Debe iniciar sesión para acceder a este recurso.");
    }
}
