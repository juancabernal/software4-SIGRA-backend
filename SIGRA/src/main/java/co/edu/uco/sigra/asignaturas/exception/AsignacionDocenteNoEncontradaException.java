package co.edu.uco.sigra.asignaturas.exception;

import java.util.UUID;

public class AsignacionDocenteNoEncontradaException extends RuntimeException {
    public AsignacionDocenteNoEncontradaException(UUID id) {
        super("No existe una asignación docente con id " + id + ".");
    }
}
