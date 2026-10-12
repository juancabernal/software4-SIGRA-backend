package co.edu.uco.sigra.estudiantes.exception;

import java.util.UUID;

public class ConfirmacionRequeridaException extends RuntimeException {

    private ConfirmacionRequeridaException(String mensaje) {
        super(mensaje);
    }

    public static ConfirmacionRequeridaException porCalificacionesRegistradas(UUID matriculaId, long cantidadCalificaciones) {
        return new ConfirmacionRequeridaException("La matrícula con id " + matriculaId
                + " tiene " + cantidadCalificaciones + " calificación(es) registrada(s). "
                + "Debe confirmar la desvinculación para continuar.");
    }
}
