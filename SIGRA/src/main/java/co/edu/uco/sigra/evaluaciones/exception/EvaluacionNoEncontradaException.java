package co.edu.uco.sigra.evaluaciones.exception;

import java.util.Objects;
import java.util.UUID;

/** La evaluación solicitada no existe (404). */
public class EvaluacionNoEncontradaException extends RuntimeException {
    public EvaluacionNoEncontradaException(UUID id) {
        super(mensajeSinId(id));
    }

    private static String mensajeSinId(UUID id) {
        Objects.requireNonNull(id, "id");
        return "No existe la evaluación indicada.";
    }
}
