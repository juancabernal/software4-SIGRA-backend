package co.edu.uco.sigra.auditoria.exception;

public class RangoFechasInvalidoException extends RuntimeException {
    public RangoFechasInvalidoException() {
        super("La fecha inicial del filtro no puede ser posterior a la fecha final.");
    }
}