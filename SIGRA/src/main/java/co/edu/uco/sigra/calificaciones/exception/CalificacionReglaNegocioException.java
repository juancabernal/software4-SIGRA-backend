package co.edu.uco.sigra.calificaciones.exception;

public class CalificacionReglaNegocioException extends RuntimeException {

    private CalificacionReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    public static CalificacionReglaNegocioException evaluacionInactiva() {
        return new CalificacionReglaNegocioException(
                "La evaluación está inactiva; no admite nuevas calificaciones ni cambios en las existentes.");
    }

    public static CalificacionReglaNegocioException matriculaNoActiva() {
        return new CalificacionReglaNegocioException(
                "La matrícula del estudiante no está activa; no se pueden registrar ni modificar sus calificaciones.");
    }

    public static CalificacionReglaNegocioException matriculaDeOtraAsignatura() {
        return new CalificacionReglaNegocioException(
                "La matrícula no corresponde a la asignatura de la evaluación.");
    }

    public static CalificacionReglaNegocioException semestreNoCoincide() {
        return new CalificacionReglaNegocioException(
                "El semestre de la matrícula no coincide con el semestre de la evaluación.");
    }
}