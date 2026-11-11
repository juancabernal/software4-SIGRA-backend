package co.edu.uco.sigra.mejoras.exception;

import java.util.Objects;
import java.util.UUID;

/** La acción de mejora ya fue convertida en evaluación antes (409). */
public class AccionYaConvertidaException extends RuntimeException {
    public AccionYaConvertidaException(UUID accionId) {
        super(mensajeSinId(accionId));
    }

    private static String mensajeSinId(UUID accionId) {
        Objects.requireNonNull(accionId, "accionId");
        return "La acción de mejora ya fue convertida en evaluación.";
    }
}
