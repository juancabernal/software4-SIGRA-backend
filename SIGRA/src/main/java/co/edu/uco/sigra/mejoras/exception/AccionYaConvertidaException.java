package co.edu.uco.sigra.mejoras.exception;

import java.util.UUID;

/** La acción de mejora ya fue convertida en evaluación antes (409). */
public class AccionYaConvertidaException extends RuntimeException {
    public AccionYaConvertidaException(UUID accionId) {
        super("La acción de mejora " + accionId + " ya fue convertida en evaluación.");
    }
}
