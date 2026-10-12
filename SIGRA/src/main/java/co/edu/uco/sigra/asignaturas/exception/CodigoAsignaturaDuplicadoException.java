package co.edu.uco.sigra.asignaturas.exception;

public class CodigoAsignaturaDuplicadoException extends RuntimeException {
    public CodigoAsignaturaDuplicadoException(String codigo) {
        super("Ya existe una asignatura con el código " + codigo + ".");
    }
}
