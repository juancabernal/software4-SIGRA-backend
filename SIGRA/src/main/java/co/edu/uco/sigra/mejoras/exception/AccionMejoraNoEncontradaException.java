package co.edu.uco.sigra.mejoras.exception;

import java.util.Objects;
import java.util.UUID;

public class AccionMejoraNoEncontradaException extends RuntimeException {
    public AccionMejoraNoEncontradaException(UUID id) {
        super(mensajeSinId(id));
    }

    private static String mensajeSinId(UUID id) {
        Objects.requireNonNull(id, "id");
        return "No existe la acción de mejora indicada.";
    }
}
