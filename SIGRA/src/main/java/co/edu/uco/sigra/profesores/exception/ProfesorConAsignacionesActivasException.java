package co.edu.uco.sigra.profesores.exception;

import java.util.Objects;
import java.util.UUID;

public class ProfesorConAsignacionesActivasException extends RuntimeException {
    public ProfesorConAsignacionesActivasException(UUID id) {
        super(mensajeSinId(id));
    }

    private static String mensajeSinId(UUID id) {
        Objects.requireNonNull(id, "id");
        return "No se puede inactivar el profesor porque tiene asignaturas activas asociadas";
    }
}
