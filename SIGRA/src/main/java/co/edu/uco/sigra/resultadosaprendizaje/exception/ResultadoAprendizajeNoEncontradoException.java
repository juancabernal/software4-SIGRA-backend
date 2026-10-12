package co.edu.uco.sigra.resultadosaprendizaje.exception;

import java.util.UUID;

public class ResultadoAprendizajeNoEncontradoException extends RuntimeException {
    public ResultadoAprendizajeNoEncontradoException(UUID id) {
        super("No existe un resultado de aprendizaje con id " + id + ".");
    }
}
