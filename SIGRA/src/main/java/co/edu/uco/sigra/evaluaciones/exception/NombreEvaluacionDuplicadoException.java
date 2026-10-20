package co.edu.uco.sigra.evaluaciones.exception;

/** Ya existe una evaluación con ese nombre para el mismo RA y semestre, sin distinguir mayúsculas (409). */
public class NombreEvaluacionDuplicadoException extends RuntimeException {

    public static final String MENSAJE =
            "Ya existe una evaluación con ese nombre para este resultado de aprendizaje en el semestre.";

    public NombreEvaluacionDuplicadoException() {
        super(MENSAJE);
    }
}
