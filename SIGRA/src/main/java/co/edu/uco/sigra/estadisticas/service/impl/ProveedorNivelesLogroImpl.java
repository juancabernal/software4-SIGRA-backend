package co.edu.uco.sigra.estadisticas.service.impl;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
import co.edu.uco.sigra.estadisticas.service.ProveedorNivelesLogro;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Implementación real de {@link ProveedorNivelesLogro} (RF-16) sobre los niveles de logro que calcula
 * el recálculo de estadísticas (RF-14 / RF-15) en {@code nivel_logro_estudiante}.
 * <p>
 * Lleva {@link Primary} porque convive con {@code ProveedorNivelesLogroPruebaImpl} hasta que el equipo
 * retire esa implementación temporal; mientras ambas existan, {@code ConclusionServiceImpl} recibe esta.
 * «Evaluado» = estudiante con nivel de logro calculado, es decir, con 3 o más calificaciones en el RA.
 */
@Service
@Primary
@RequiredArgsConstructor
public class ProveedorNivelesLogroImpl implements ProveedorNivelesLogro {

    private static final List<NivelLogro> NIVELES_INSUFICIENTES = List.of(NivelLogro.BAJO, NivelLogro.MEDIO);

    private final NivelLogroEstudianteRepository nivelLogroEstudianteRepository;

    @Override
    public long contarEvaluados(UUID resultadoAprendizajeId, UUID semestreId) {
        return nivelLogroEstudianteRepository
                .countByResultadoAprendizaje_IdAndSemestre_IdAndNivelLogroIsNotNull(resultadoAprendizajeId, semestreId);
    }

    @Override
    public long contarEnBajoOMedio(UUID resultadoAprendizajeId, UUID semestreId) {
        return nivelLogroEstudianteRepository.countByResultadoAprendizaje_IdAndSemestre_IdAndNivelLogroIn(
                resultadoAprendizajeId, semestreId, NIVELES_INSUFICIENTES);
    }
}
