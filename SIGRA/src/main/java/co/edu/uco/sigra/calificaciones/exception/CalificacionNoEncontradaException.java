package co.edu.uco.sigra.calificaciones.exception;

import java.util.Objects;
import java.util.UUID;

public class CalificacionNoEncontradaException extends RuntimeException {
    public CalificacionNoEncontradaException(UUID id) {
        super(mensajeSinId(id));
    }

    private static String mensajeSinId(UUID id) {
        Objects.requireNonNull(id, "id");
        return "No existe la calificación indicada.";
    }
}
