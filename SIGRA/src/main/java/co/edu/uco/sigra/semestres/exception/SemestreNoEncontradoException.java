package co.edu.uco.sigra.semestres.exception;

public class SemestreNoEncontradoException extends RuntimeException {
    public SemestreNoEncontradoException(String codigo) {
        super("No se encontró el semestre con código " + codigo);
    }
}
