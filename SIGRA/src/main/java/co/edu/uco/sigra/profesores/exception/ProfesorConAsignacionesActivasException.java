package co.edu.uco.sigra.profesores.exception;

import java.util.UUID;

public class ProfesorConAsignacionesActivasException extends RuntimeException {
    public ProfesorConAsignacionesActivasException(UUID id) {
        super("No se puede inactivar el profesor " + id + " porque tiene asignaturas activas asociadas");
    }
}
