package co.edu.uco.sigra.estudiantes.exception;

public class AutenticacionRequeridaException extends RuntimeException {
    public AutenticacionRequeridaException() {
        super("Debe iniciar sesión para acceder a este recurso.");
    }
}
