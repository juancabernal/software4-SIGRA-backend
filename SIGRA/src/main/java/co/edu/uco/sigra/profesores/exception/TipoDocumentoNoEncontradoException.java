package co.edu.uco.sigra.profesores.exception;

public class CorreoDuplicadoException extends RuntimeException {
    public CorreoDuplicadoException(String correoInstitucional) {
        super("Ya existe un profesor registrado con el correo institucional proporcionado: " + correoInstitucional);
    }
}
