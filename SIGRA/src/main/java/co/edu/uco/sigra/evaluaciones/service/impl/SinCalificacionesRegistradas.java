package co.edu.uco.sigra.evaluaciones.service.impl;

import co.edu.uco.sigra.evaluaciones.service.ConsultaCalificacionesEvaluacion;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementación TEMPORAL de {@link ConsultaCalificacionesEvaluacion} mientras el módulo de calificaciones
 * (RF-12) no exista: ninguna evaluación tiene calificaciones, así que todos sus campos son editables.
 * <p>
 * Pendiente de integrar: cuando RF-12 entregue sus calificaciones, se reemplaza por una implementación
 * que las consulte y se elimina esta clase.
 */
@Component
public class SinCalificacionesRegistradas implements ConsultaCalificacionesEvaluacion {

    @Override
    public boolean tieneCalificaciones(UUID evaluacionId) {
        return false;
    }
}
