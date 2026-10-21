package co.edu.uco.sigra.evaluaciones.exception;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Reglas de negocio de las evaluaciones (RF-10 / RF-11) que se incumplen (400). */
public class EvaluacionReglaNegocioException extends RuntimeException {

    private EvaluacionReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    /** RF-11: la suma de las evaluaciones activas del RA en el semestre superaría 100%. */
    public static EvaluacionReglaNegocioException porcentajeExcedido(BigDecimal disponible) {
        return new EvaluacionReglaNegocioException(
                "La suma de porcentajes de las evaluaciones activas de este resultado de aprendizaje en el semestre "
                        + "superaría el 100%. Porcentaje disponible: " + disponible.stripTrailingZeros().toPlainString()
                        + "%.");
    }

    public static EvaluacionReglaNegocioException resultadoAprendizajeInactivo() {
        return new EvaluacionReglaNegocioException(
                "El resultado de aprendizaje está inactivo; no se pueden crear ni reactivar evaluaciones.");
    }

    public static EvaluacionReglaNegocioException asignaturaNoActiva() {
        return new EvaluacionReglaNegocioException(
                "La asignatura no está activa; no se pueden crear ni reactivar evaluaciones.");
    }

    public static EvaluacionReglaNegocioException sinSemestreActivo() {
        return new EvaluacionReglaNegocioException(
                "No hay un semestre activo; no se pueden crear evaluaciones.");
    }

    public static EvaluacionReglaNegocioException fechaFueraDelSemestre(String codigoSemestre, LocalDate inicio,
                                                                        LocalDate fin) {
        return new EvaluacionReglaNegocioException(
                "La fecha tentativa debe estar dentro del semestre " + codigoSemestre + " (entre " + inicio
                        + " y " + fin + ").");
    }

    /** La fecha tentativa de una evaluación debe ser posterior a hoy. */
    public static EvaluacionReglaNegocioException fechaNoPosteriorAHoy() {
        return new EvaluacionReglaNegocioException("La fecha tentativa debe ser posterior a la fecha de hoy.");
    }

    /** RF-10b: con calificaciones registradas solo se editan el nombre y la descripción. */
    public static EvaluacionReglaNegocioException camposBloqueadosPorCalificaciones() {
        return new EvaluacionReglaNegocioException(
                "La evaluación ya tiene calificaciones registradas: no se pueden modificar la fecha tentativa "
                        + "ni el porcentaje, solo el nombre y la descripción.");
    }

    public static EvaluacionReglaNegocioException modificarInactiva() {
        return new EvaluacionReglaNegocioException("No se puede modificar una evaluación inactiva.");
    }

    public static EvaluacionReglaNegocioException semestreNoActivo() {
        return new EvaluacionReglaNegocioException(
                "La evaluación pertenece a un semestre que no está activo; solo se puede consultar.");
    }

    public static EvaluacionReglaNegocioException yaActiva() {
        return new EvaluacionReglaNegocioException("La evaluación ya se encuentra activa.");
    }

    public static EvaluacionReglaNegocioException yaInactiva() {
        return new EvaluacionReglaNegocioException("La evaluación ya se encuentra inactiva.");
    }
}
