package co.edu.uco.sigra.estudiantes.exception;

import java.util.UUID;

public class MatriculaDuplicadaException extends RuntimeException {

    private MatriculaDuplicadaException(String mensaje) {
        super(mensaje);
    }

    public static MatriculaDuplicadaException porTerna(UUID estudianteId, UUID asignaturaId, UUID semestreId) {
        return new MatriculaDuplicadaException("El estudiante " + estudianteId
                + " ya está matriculado en la asignatura " + asignaturaId
                + " para el semestre " + semestreId + ".");
    }
}
