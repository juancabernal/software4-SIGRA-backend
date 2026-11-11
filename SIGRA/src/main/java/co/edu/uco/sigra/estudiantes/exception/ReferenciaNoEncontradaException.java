package co.edu.uco.sigra.estudiantes.exception;

import java.util.Objects;
import java.util.UUID;

public class ReferenciaNoEncontradaException extends RuntimeException {

    private ReferenciaNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    public static ReferenciaNoEncontradaException tipoDocumento(UUID id) {
        Objects.requireNonNull(id, "id");
        return new ReferenciaNoEncontradaException("No existe el tipo de documento indicado.");
    }

    public static ReferenciaNoEncontradaException asignatura(UUID id) {
        Objects.requireNonNull(id, "id");
        return new ReferenciaNoEncontradaException("No existe la asignatura indicada.");
    }

    public static ReferenciaNoEncontradaException semestre(UUID id) {
        Objects.requireNonNull(id, "id");
        return new ReferenciaNoEncontradaException("No existe el semestre indicado.");
    }
}
