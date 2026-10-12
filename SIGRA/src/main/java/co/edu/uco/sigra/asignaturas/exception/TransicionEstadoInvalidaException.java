package co.edu.uco.sigra.asignaturas.exception;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;

public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(EstadoAsignatura actual, String accion, String requisito) {
        super("No se puede " + accion + " una asignatura en estado " + actual + ". " + requisito);
    }
}
