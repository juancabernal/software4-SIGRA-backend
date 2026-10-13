package co.edu.uco.sigra.estadisticas.exception;

import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;

public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(EstadoConclusion actual, String accion, String requisito) {
        super("No se puede " + accion + " una conclusión en estado " + actual + ". " + requisito);
    }
}
