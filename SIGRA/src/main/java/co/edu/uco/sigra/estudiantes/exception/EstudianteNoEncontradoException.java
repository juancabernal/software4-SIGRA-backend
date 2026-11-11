package co.edu.uco.sigra.estudiantes.exception;

import java.util.Objects;
import java.util.UUID;

public class EstudianteNoEncontradoException extends RuntimeException {

    private EstudianteNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static EstudianteNoEncontradoException porId(UUID id) {
        Objects.requireNonNull(id, "id");
        return new EstudianteNoEncontradoException("No existe el estudiante solicitado.");
    }
}
