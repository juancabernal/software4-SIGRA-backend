package co.edu.uco.sigra.asignaturas.exception;

import java.util.UUID;

public class AsignaturaNoEncontradaException extends RuntimeException {
    public AsignaturaNoEncontradaException(UUID id) {
        super("No existe una asignatura con id " + id + ".");
    }
}
