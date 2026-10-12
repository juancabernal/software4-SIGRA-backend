package co.edu.uco.sigra.profesores.exception;

public class CorreoDuplicadoException extends RuntimeException {
    public CorreoDuplicadoException(String correo) {
        super("Ya existe un profesor registrado con el correo " + correo);
    }
}
