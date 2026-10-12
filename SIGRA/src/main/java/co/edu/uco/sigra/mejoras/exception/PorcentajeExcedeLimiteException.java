package co.edu.uco.sigra.mejoras.exception;

/** La suma de porcentajes del RA en el semestre superaría el 100% con el nuevo porcentaje (400). */
public class PorcentajeExcedeLimiteException extends RuntimeException {
    public PorcentajeExcedeLimiteException(double sumaActual, double porcentajeNuevo) {
        super("El porcentaje supera el límite permitido para este resultado de aprendizaje en el semestre.");
    }
}
