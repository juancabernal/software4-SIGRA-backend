package co.edu.uco.sigra.profesores.exception;

import java.util.UUID;

public class TipoDocumentoNoEncontradoException extends RuntimeException {
    public TipoDocumentoNoEncontradoException(UUID id) {
        super("No se encontró el tipo de documento con id " + id);
    }
}
