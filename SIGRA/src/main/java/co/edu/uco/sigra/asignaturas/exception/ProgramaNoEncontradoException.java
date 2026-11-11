package co.edu.uco.sigra.asignaturas.exception;

import java.util.Objects;
import java.util.UUID;

public class ProgramaNoEncontradoException extends RuntimeException {
    public ProgramaNoEncontradoException(UUID id) {
        super(mensajeSinId(id));
    }

    private static String mensajeSinId(UUID id) {
        Objects.requireNonNull(id, "id");
        return "No existe el programa académico indicado.";
    }
}
