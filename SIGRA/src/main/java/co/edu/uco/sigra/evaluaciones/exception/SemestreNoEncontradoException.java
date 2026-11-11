package co.edu.uco.sigra.evaluaciones.exception;

import java.util.Objects;
import java.util.UUID;

/**
 * El semestre pedido en la consulta no existe (404). La del módulo semestres busca por código; aquí
 * se consulta por id, como hace el módulo de estadísticas con la suya.
 */
public class SemestreNoEncontradoException extends RuntimeException {
    public SemestreNoEncontradoException(UUID id) {
        super(mensajeSinId(id));
    }

    private static String mensajeSinId(UUID id) {
        Objects.requireNonNull(id, "id");
        return "No existe el semestre indicado.";
    }
}
