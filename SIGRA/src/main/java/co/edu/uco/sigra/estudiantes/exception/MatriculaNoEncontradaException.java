package co.edu.uco.sigra.estudiantes.exception;

import java.util.Objects;
import java.util.UUID;

public class MatriculaNoEncontradaException extends RuntimeException {

    private MatriculaNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    public static MatriculaNoEncontradaException porId(UUID id) {
        Objects.requireNonNull(id, "id");
        return new MatriculaNoEncontradaException("No existe la matrícula solicitada.");
    }
}
