package co.edu.uco.sigra.profesores.exception;

import java.util.Objects;
import java.util.UUID;

public class ProfesorNoEncontradoException extends RuntimeException {
    public ProfesorNoEncontradoException(UUID id) {
        super(mensajeSinId(id));
    }

    private static String mensajeSinId(UUID id) {
        Objects.requireNonNull(id, "id");
        return "No se encontró el profesor indicado.";
    }
}
