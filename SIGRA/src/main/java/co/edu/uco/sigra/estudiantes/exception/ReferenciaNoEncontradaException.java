package co.edu.uco.sigra.estudiantes.exception;

import java.util.UUID;

public class ReferenciaNoEncontradaException extends RuntimeException {

    private ReferenciaNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    public static ReferenciaNoEncontradaException tipoDocumento(UUID id) {
        return new ReferenciaNoEncontradaException("No existe un tipo de documento con id " + id + ".");
    }

    public static ReferenciaNoEncontradaException asignatura(UUID id) {
        return new ReferenciaNoEncontradaException("No existe una asignatura con id " + id + ".");
    }

    public static ReferenciaNoEncontradaException semestre(UUID id) {
        return new ReferenciaNoEncontradaException("No existe un semestre con id " + id + ".");
    }
}
