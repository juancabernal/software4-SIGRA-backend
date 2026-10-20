package co.edu.uco.sigra.estadisticas.service;

import java.util.UUID;

/**
 * Lo mínimo que el cálculo del nivel de logro necesita de una calificación: de qué estudiante es,
 * la nota (0.0 – 5.0) y el porcentaje de la evaluación a la que pertenece (mayor que 0).
 * Las reglas de captura (nota en rango, porcentajes por RA) las valida el módulo dueño de los datos;
 * aquí se rechazan valores imposibles para que nunca se calcule un nivel con datos corruptos.
 */
public record CalificacionPonderada(UUID estudianteId, double nota, double porcentajeEvaluacion) {

    public CalificacionPonderada {
        if (estudianteId == null) {
            throw new IllegalArgumentException("La calificación debe indicar el estudiante.");
        }
        if (Double.isNaN(nota) || nota < CatalogoNivelesLogro.NOTA_MINIMA || nota > CatalogoNivelesLogro.NOTA_MAXIMA) {
            throw new IllegalArgumentException("La nota " + nota + " está fuera de la escala 0.0 – 5.0.");
        }
        if (Double.isNaN(porcentajeEvaluacion) || porcentajeEvaluacion <= 0) {
            throw new IllegalArgumentException("El porcentaje de la evaluación debe ser mayor que 0.");
        }
    }
}
