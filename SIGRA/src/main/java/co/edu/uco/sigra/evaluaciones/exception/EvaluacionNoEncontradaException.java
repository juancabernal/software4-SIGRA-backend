package co.edu.uco.sigra.evaluaciones.exception;

import java.util.UUID;

/** La evaluación solicitada no existe (404). */
public class EvaluacionNoEncontradaException extends RuntimeException {
    public EvaluacionNoEncontradaException(UUID id) {
        super("No existe una evaluación con id " + id + ".");
    }
}
