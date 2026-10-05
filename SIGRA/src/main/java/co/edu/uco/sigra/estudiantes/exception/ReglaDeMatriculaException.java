package co.edu.uco.sigra.estudiantes.exception;

import java.util.UUID;

public class ReglaDeMatriculaException extends RuntimeException {

    private ReglaDeMatriculaException(String mensaje) {
        super(mensaje);
    }

    public static ReglaDeMatriculaException estudianteInactivo(UUID estudianteId) {
        return new ReglaDeMatriculaException("El estudiante con id " + estudianteId + " no está activo.");
    }

    public static ReglaDeMatriculaException asignaturaNoActiva(UUID asignaturaId) {
        return new ReglaDeMatriculaException("La asignatura con id " + asignaturaId + " no está activa.");
    }

    public static ReglaDeMatriculaException semestreNoActivo(UUID semestreId) {
        return new ReglaDeMatriculaException("El semestre con id " + semestreId + " no está activo.");
    }
}
