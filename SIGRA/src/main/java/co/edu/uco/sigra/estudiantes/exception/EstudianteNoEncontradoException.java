package co.edu.uco.sigra.estudiantes.exception;

import java.util.UUID;

public class EstudianteNoEncontradoException extends RuntimeException {

    private EstudianteNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static EstudianteNoEncontradoException porId(UUID id) {
        return new EstudianteNoEncontradoException("No existe un estudiante con id " + id + ".");
    }
}
