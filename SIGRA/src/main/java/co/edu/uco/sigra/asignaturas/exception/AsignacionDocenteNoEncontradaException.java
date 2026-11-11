package co.edu.uco.sigra.asignaturas.exception;

import java.util.Objects;
import java.util.UUID;

public class AsignacionDocenteNoEncontradaException extends RuntimeException {
    public AsignacionDocenteNoEncontradaException(UUID id) {
        super(mensajeSinId(id));
    }

    private static String mensajeSinId(UUID id) {
        Objects.requireNonNull(id, "id");
        return "No existe la asignación docente indicada.";
    }
}
