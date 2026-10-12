package co.edu.uco.sigra.estadisticas.service.impl;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.repository.RegistroNivelLogroPruebaRepository;
import co.edu.uco.sigra.estadisticas.service.ProveedorNivelesLogro;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Implementación temporal de {@link ProveedorNivelesLogro} con datos de prueba
 * ({@code RegistroNivelLogroPrueba}), mientras el módulo de Estadísticas no calcule el nivel de
 * logro real a partir de las calificaciones. Pendiente de integrar: ver el javadoc de esa entidad.
 */
@Service
@RequiredArgsConstructor
public class ProveedorNivelesLogroPruebaImpl implements ProveedorNivelesLogro {

    private static final List<NivelLogro> NIVELES_INSUFICIENTES = List.of(NivelLogro.BAJO, NivelLogro.MEDIO);

    private final RegistroNivelLogroPruebaRepository registroNivelLogroPruebaRepository;

    @Override
    public long contarEvaluados(UUID resultadoAprendizajeId, UUID semestreId) {
        return registroNivelLogroPruebaRepository
                .countByResultadoAprendizaje_IdAndSemestre_Id(resultadoAprendizajeId, semestreId);
    }

    @Override
    public long contarEnBajoOMedio(UUID resultadoAprendizajeId, UUID semestreId) {
        return registroNivelLogroPruebaRepository.countByResultadoAprendizaje_IdAndSemestre_IdAndNivelLogroIn(
                resultadoAprendizajeId, semestreId, NIVELES_INSUFICIENTES);
    }
}
