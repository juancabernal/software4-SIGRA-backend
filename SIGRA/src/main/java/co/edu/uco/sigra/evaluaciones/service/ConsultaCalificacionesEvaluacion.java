package co.edu.uco.sigra.evaluaciones.service;

import java.util.UUID;

/**
 * Puerto hacia el módulo de calificaciones (RF-12): lo único que evaluaciones necesita saber de él es si
 * una evaluación ya tiene calificaciones registradas, para bloquear la fecha tentativa y el porcentaje
 * (RF-10b).
 * <p>
 * Mientras RF-12 no exista, la implementación es {@code SinCalificacionesRegistradas}, que responde
 * siempre {@code false}. Cuando exista, se agrega una implementación que consulte las calificaciones
 * reales y se retira la temporal; {@code EvaluacionServiceImpl} no cambia, porque solo depende de esta
 * interfaz.
 */
public interface ConsultaCalificacionesEvaluacion {

    boolean tieneCalificaciones(UUID evaluacionId);
}
