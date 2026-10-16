package co.edu.uco.sigra.calificaciones.service.impl;

import co.edu.uco.sigra.calificaciones.repository.CalificacionRepository;
import co.edu.uco.sigra.evaluaciones.service.ConsultaCalificacionesEvaluacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CalificacionesRegistradasEvaluacion implements ConsultaCalificacionesEvaluacion {

    private final CalificacionRepository calificacionRepository;

    @Override
    public boolean tieneCalificaciones(UUID evaluacionId) {
        return calificacionRepository.existsByEvaluacion_Id(evaluacionId);
    }
}