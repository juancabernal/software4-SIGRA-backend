package co.edu.uco.sigra.programas.exception;

import java.util.UUID;

public class ProgramaAcademicoNoEncontradoException extends RuntimeException {

    public ProgramaAcademicoNoEncontradoException(UUID id) {
        super("No existe un programa académico con el identificador " + id);
    }
}
