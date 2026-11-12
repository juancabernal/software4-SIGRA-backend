package co.edu.uco.sigra.programas.exception;

import java.util.Objects;
import java.util.UUID;

public class ProgramaAcademicoNoEncontradoException extends RuntimeException {

    public ProgramaAcademicoNoEncontradoException(UUID id) {
        super(mensajeSinId(id));
    }

    private static String mensajeSinId(UUID id) {
        Objects.requireNonNull(id, "id");
        return "No existe el programa académico indicado.";
    }
}
