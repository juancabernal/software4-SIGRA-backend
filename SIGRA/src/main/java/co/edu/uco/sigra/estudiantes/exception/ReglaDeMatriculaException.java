package co.edu.uco.sigra.estudiantes.exception;

import java.util.Objects;
import java.util.UUID;

public class ReglaDeMatriculaException extends RuntimeException {

    private ReglaDeMatriculaException(String mensaje) {
        super(mensaje);
    }

    public static ReglaDeMatriculaException estudianteInactivo(UUID estudianteId) {
        Objects.requireNonNull(estudianteId, "estudianteId");
        return new ReglaDeMatriculaException("El estudiante no está activo.");
    }

    public static ReglaDeMatriculaException asignaturaNoActiva(UUID asignaturaId) {
        Objects.requireNonNull(asignaturaId, "asignaturaId");
        return new ReglaDeMatriculaException("La asignatura no está activa.");
    }

    public static ReglaDeMatriculaException semestreNoVigente(UUID semestreId) {
        Objects.requireNonNull(semestreId, "semestreId");
        return new ReglaDeMatriculaException("El semestre no está vigente.");
    }
}
