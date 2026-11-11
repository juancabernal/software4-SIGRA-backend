package co.edu.uco.sigra.estadisticas.exception;

import java.util.Objects;
import java.util.UUID;

public class ConclusionNoEncontradaException extends RuntimeException {
    public ConclusionNoEncontradaException(UUID id) {
        super(mensajeSinId(id));
    }

    private static String mensajeSinId(UUID id) {
        Objects.requireNonNull(id, "id");
        return "No existe la conclusión indicada.";
    }
}
