package co.edu.uco.sigra.estudiantes.exception;

import java.util.Objects;
import java.util.UUID;

public class MatriculaYaInactivaException extends RuntimeException {

    private MatriculaYaInactivaException(String mensaje) {
        super(mensaje);
    }

    public static MatriculaYaInactivaException porId(UUID id) {
        Objects.requireNonNull(id, "id");
        return new MatriculaYaInactivaException("La matrícula ya no está activa.");
    }
}
