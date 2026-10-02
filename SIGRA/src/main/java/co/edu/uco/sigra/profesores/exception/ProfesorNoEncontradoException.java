package co.edu.uco.sigra.profesores.exception;

import java.util.UUID;

public class ProfesorNoEncontradoException extends RuntimeException {
    public ProfesorNoEncontradoException(UUID id) {
        super("No se encontró el profesor con id " + id);
    }
}

