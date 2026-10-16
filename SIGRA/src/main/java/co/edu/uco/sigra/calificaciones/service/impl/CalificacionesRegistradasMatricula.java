package co.edu.uco.sigra.calificaciones.service.impl;

import co.edu.uco.sigra.calificaciones.repository.CalificacionRepository;
import co.edu.uco.sigra.estudiantes.service.CalificacionesDeMatricula;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CalificacionesRegistradasMatricula implements CalificacionesDeMatricula {

    private final CalificacionRepository calificacionRepository;

    @Override
    public long contarPorMatricula(UUID matriculaId) {
        return calificacionRepository.countByMatricula_Id(matriculaId);
    }
}