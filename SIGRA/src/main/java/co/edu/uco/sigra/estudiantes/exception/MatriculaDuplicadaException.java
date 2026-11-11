package co.edu.uco.sigra.estudiantes.exception;

import java.util.Objects;
import java.util.UUID;

public class MatriculaDuplicadaException extends RuntimeException {

    private MatriculaDuplicadaException(String mensaje) {
        super(mensaje);
    }

    public static MatriculaDuplicadaException porTerna(UUID estudianteId, UUID asignaturaId, UUID semestreId) {
        Objects.requireNonNull(estudianteId, "estudianteId");
        Objects.requireNonNull(asignaturaId, "asignaturaId");
        Objects.requireNonNull(semestreId, "semestreId");
        return new MatriculaDuplicadaException(
                "El estudiante ya está matriculado en esa asignatura y semestre.");
    }
}
