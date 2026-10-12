package co.edu.uco.sigra.auth.exception;

public class UsuarioBloqueadoException extends RuntimeException {
    public UsuarioBloqueadoException() {
        super("El usuario está temporalmente bloqueado. Intente nuevamente más tarde.");
    }
}
