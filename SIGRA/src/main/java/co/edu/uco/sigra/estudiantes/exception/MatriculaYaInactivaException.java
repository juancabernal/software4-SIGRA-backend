package co.edu.uco.sigra.estudiantes.exception;

import java.util.UUID;

public class MatriculaYaInactivaException extends RuntimeException {

    private MatriculaYaInactivaException(String mensaje) {
        super(mensaje);
    }

    public static MatriculaYaInactivaException porId(UUID id) {
        return new MatriculaYaInactivaException("La matrícula con id " + id + " ya no está activa.");
    }
}
