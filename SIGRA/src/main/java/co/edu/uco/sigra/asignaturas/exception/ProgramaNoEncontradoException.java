package co.edu.uco.sigra.asignaturas.exception;

import java.util.UUID;

public class ProgramaNoEncontradoException extends RuntimeException {
    public ProgramaNoEncontradoException(UUID id) {
        super("No existe un programa académico con id " + id + ".");
    }
}
