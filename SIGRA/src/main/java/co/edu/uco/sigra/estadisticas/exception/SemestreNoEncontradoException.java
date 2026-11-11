package co.edu.uco.sigra.estadisticas.exception;

import java.util.Objects;
import java.util.UUID;

/**
 * 404 de semestre por id, propio de este módulo: {@code semestres.exception.SemestreNoEncontradoException}
 * del equipo de Semestres solo admite buscar por código, y aquí se necesita por id (viene en la URL).
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
