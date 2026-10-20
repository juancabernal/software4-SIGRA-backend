package co.edu.uco.sigra.estadisticas.exception;

import java.util.UUID;

public class ConclusionNoEncontradaException extends RuntimeException {
    public ConclusionNoEncontradaException(UUID id) {
        super("No existe una conclusión con id " + id + ".");
    }
}
