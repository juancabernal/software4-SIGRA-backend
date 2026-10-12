package co.edu.uco.sigra.estudiantes.exception;

import java.util.UUID;

public class MatriculaNoEncontradaException extends RuntimeException {

    private MatriculaNoEncontradaException(String mensaje) {
        super(mensaje);
    }

    public static MatriculaNoEncontradaException porId(UUID id) {
        return new MatriculaNoEncontradaException("No existe una matrícula con id " + id + ".");
    }
}
