package co.edu.uco.sigra.mejoras.exception;

import java.util.UUID;

public class AccionMejoraNoEncontradaException extends RuntimeException {
    public AccionMejoraNoEncontradaException(UUID id) {
        super("No existe una acción de mejora con id " + id + ".");
    }
}
