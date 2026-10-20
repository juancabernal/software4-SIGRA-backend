package co.edu.uco.sigra.calificaciones.exception;

import java.util.UUID;

public class CalificacionNoEncontradaException extends RuntimeException {
    public CalificacionNoEncontradaException(UUID id) {
        super("No existe una calificación con id " + id + ".");
    }
}
