package co.edu.uco.sigra.profesores.exception;

import java.util.Objects;
import java.util.UUID;

public class TipoDocumentoNoEncontradoException extends RuntimeException {
    public TipoDocumentoNoEncontradoException(UUID id) {
        super(mensajeSinId(id));
    }

    private static String mensajeSinId(UUID id) {
        Objects.requireNonNull(id, "id");
        return "No se encontró el tipo de documento indicado.";
    }
}
