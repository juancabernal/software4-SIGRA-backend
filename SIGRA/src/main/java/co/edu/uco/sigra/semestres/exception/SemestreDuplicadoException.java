package co.edu.uco.sigra.semestres.exception;

public class SemestreDuplicadoException extends RuntimeException {
    public SemestreDuplicadoException(String codigo) {
        super("Ya existe un semestre con el código " + codigo);
    }
}
