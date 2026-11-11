package co.edu.uco.sigra.facultades.exception;

import java.util.UUID;

public class FacultadNoEncontradaException extends RuntimeException {

    public FacultadNoEncontradaException(UUID id) {
        super("No existe una facultad con el identificador " + id);
    }
}
