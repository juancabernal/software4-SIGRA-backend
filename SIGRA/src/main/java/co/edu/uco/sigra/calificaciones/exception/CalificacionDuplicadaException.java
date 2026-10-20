package co.edu.uco.sigra.calificaciones.exception;

public class CalificacionDuplicadaException extends RuntimeException {

    public static final String MENSAJE =
            "El estudiante ya tiene una calificación en esta evaluación; modifíquela en lugar de registrar otra.";

    public CalificacionDuplicadaException() {
        super(MENSAJE);
    }
}