package co.edu.uco.sigra.asignaturas.exception;

import java.util.UUID;

public class ConfirmacionRequeridaException extends RuntimeException {

    private ConfirmacionRequeridaException(String mensaje) {
        super(mensaje);
    }

    public static ConfirmacionRequeridaException porEvaluacionesAbiertas(UUID asignaturaId, long cantidadEvaluaciones) {
        return new ConfirmacionRequeridaException("La asignatura con id " + asignaturaId
                + " tiene " + cantidadEvaluaciones + " evaluación(es) abierta(s) en el semestre en curso. "
                + "Debe confirmar la inactivación para continuar.");
    }
}
