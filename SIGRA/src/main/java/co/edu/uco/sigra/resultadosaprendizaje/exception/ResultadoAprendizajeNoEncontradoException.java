package co.edu.uco.sigra.resultadosaprendizaje.exception;

import java.util.Objects;
import java.util.UUID;

public class ResultadoAprendizajeNoEncontradoException extends RuntimeException {
    public ResultadoAprendizajeNoEncontradoException(UUID id) {
        super(mensajeSinId(id));
    }

    private static String mensajeSinId(UUID id) {
        Objects.requireNonNull(id, "id");
        return "No existe el resultado de aprendizaje indicado.";
    }
}
