package co.edu.uco.sigra.estadisticas.service.impl;

import co.edu.uco.sigra.estadisticas.repository.RegistroCalificacionPruebaRepository;
import co.edu.uco.sigra.estadisticas.service.CalificacionPonderada;
import co.edu.uco.sigra.estadisticas.service.FuenteCalificaciones;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Implementación temporal de {@link FuenteCalificaciones} con datos de prueba
 * ({@code RegistroCalificacionPrueba}), mientras no existan los módulos de evaluaciones y calificaciones.
 * Pendiente de integrar: ver el javadoc de esa entidad.
 */
@Service
@RequiredArgsConstructor
public class FuenteCalificacionesPruebaImpl implements FuenteCalificaciones {

    private final RegistroCalificacionPruebaRepository registroCalificacionPruebaRepository;

    @Override
    public List<CalificacionPonderada> listarCalificaciones(UUID resultadoAprendizajeId, UUID semestreId) {
        return registroCalificacionPruebaRepository.buscarPorRaYSemestre(resultadoAprendizajeId, semestreId)
                .stream()
                .map(r -> new CalificacionPonderada(r.getEstudiante().getId(), r.getNota(), r.getPorcentajeEvaluacion()))
                .toList();
    }
}
