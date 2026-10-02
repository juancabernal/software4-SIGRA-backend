package co.edu.uco.sigra.profesores.exception;

public class DocumentoDuplicadoException extends RuntimeException {
    public DocumentoDuplicadoException(String numeroDocumento) {
        super("Ya existe un profesor registrado con el documento" + numeroDocumento);
    }
}
