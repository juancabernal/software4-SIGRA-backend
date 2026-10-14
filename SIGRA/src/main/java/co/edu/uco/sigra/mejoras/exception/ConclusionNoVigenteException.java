package co.edu.uco.sigra.mejoras.exception;

import java.util.UUID;

/** Se intenta registrar una acción de mejora sobre una conclusión que no está VIGENTE (400). */
public class ConclusionNoVigenteException extends RuntimeException {
    public ConclusionNoVigenteException(UUID conclusionId) {
        super("La conclusión " + conclusionId + " no está en estado VIGENTE.");
    }
}
