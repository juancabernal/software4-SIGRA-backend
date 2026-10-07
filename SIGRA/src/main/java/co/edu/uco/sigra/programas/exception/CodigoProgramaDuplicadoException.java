package co.edu.uco.sigra.programas.exception;

public class CodigoProgramaDuplicadoException extends RuntimeException {

    public CodigoProgramaDuplicadoException(String codigo) {
        super("Ya existe un programa académico con el código '" + codigo + "'");
    }
}
