package co.edu.uco.sigra.resultadosaprendizaje.service.impl;

import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
import co.edu.uco.sigra.estadisticas.service.CatalogoNivelesLogro;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.repository.EvaluacionRepository;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.resultadosaprendizaje.dto.EstadoRendimiento;
import co.edu.uco.sigra.resultadosaprendizaje.dto.RendimientoAsignaturaResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.RendimientoResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.resultadosaprendizaje.service.RendimientoResultadoAprendizajeService;
import co.edu.uco.sigra.semestres.entity.Semestre;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Calcula el rendimiento de un RA a partir de los niveles ya calculados por estudiante
 * (módulo estadísticas) y de las evaluaciones (módulo evaluaciones). No persiste nada.
 * <p>
 * Nivel del RA = clasificación del promedio de los promedios ponderados de los estudiantes
 * con nivel calculado (mínimo 3 calificaciones por estudiante, RF-15).
 */
@Service
@RequiredArgsConstructor
public class RendimientoResultadoAprendizajeServiceImpl implements RendimientoResultadoAprendizajeService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final AsignaturaRepository asignaturaRepository;
    private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    private final EvaluacionService evaluacionService;
    private final EvaluacionRepository evaluacionRepository;
    private final MatriculaRepository matriculaRepository;
    private final NivelLogroEstudianteRepository nivelLogroEstudianteRepository;

    @Override
    @Transactional
    public RendimientoAsignaturaResponseDTO consultarPorAsignatura(UUID asignaturaId) {
        if (!asignaturaRepository.existsById(asignaturaId)) {
            throw new AsignaturaNoEncontradaException(asignaturaId);
        }
        Optional<Semestre> semestreActivo = evaluacionService.consultarSemestreActivo();
        if (semestreActivo.isEmpty()) {
            return new RendimientoAsignaturaResponseDTO(asignaturaId, null, null, List.of());
        }
        Semestre semestre = semestreActivo.get();
        int matriculados = matriculaRepository
                .buscarPorAsignaturaYSemestre(asignaturaId, semestre.getId(), EstadoMatricula.ACTIVA).size();

        List<RendimientoResultadoAprendizajeResponseDTO> resultados = resultadoAprendizajeRepository
                .findByAsignatura_IdAndEstadoOrderByCodigoAsc(asignaturaId, EstadoRegistro.ACTIVO).stream()
                .map(ra -> calcular(ra, semestre.getId(), matriculados))
                .toList();
        return new RendimientoAsignaturaResponseDTO(asignaturaId, semestre.getId(), semestre.getCodigo(), resultados);
    }

    private RendimientoResultadoAprendizajeResponseDTO calcular(ResultadoAprendizaje ra, UUID semestreId,
                                                                int matriculados) {
        List<NivelLogroEstudiante> niveles =
                nivelLogroEstudianteRepository.findByResultadoAprendizaje_IdAndSemestre_Id(ra.getId(), semestreId);
        boolean hayCalificaciones = niveles.stream().anyMatch(n -> n.getCantidadCalificaciones() > 0);
        List<Double> promedios = niveles.stream()
                .filter(n -> n.getNivelLogro() != null && n.getPromedioPonderado() != null)
                .map(NivelLogroEstudiante::getPromedioPonderado)
                .toList();

        EstadoRendimiento estado;
        NivelLogro nivel = null;
        Double promedioNota = null;
        if (!hayCalificaciones) {
            estado = EstadoRendimiento.SIN_EVALUAR;
        } else if (promedios.isEmpty()) {
            estado = EstadoRendimiento.DATOS_INSUFICIENTES;
        } else {
            double media = promedios.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            promedioNota = BigDecimal.valueOf(media).setScale(2, RoundingMode.HALF_UP).doubleValue();
            nivel = CatalogoNivelesLogro.clasificar(promedioNota);
            estado = EstadoRendimiento.CON_NIVEL;
        }

        BigDecimal asignado = evaluacionRepository.sumarPorcentajes(ra.getId(), semestreId, EstadoEvaluacion.ACTIVA);
        BigDecimal evaluado = calcularPorcentajeEvaluado(ra.getId(), semestreId, matriculados);
        boolean completo = asignado.compareTo(CIEN) == 0 && evaluado.compareTo(CIEN) == 0;
        return new RendimientoResultadoAprendizajeResponseDTO(ra.getId(), ra.getCodigo(), estado, nivel,
                promedioNota, asignado, evaluado, completo);
    }

    /** Suma el % de las evaluaciones ACTIVAS calificadas a todos los matriculados ACTIVOS. */
    private BigDecimal calcularPorcentajeEvaluado(UUID raId, UUID semestreId, int matriculados) {
        if (matriculados == 0) {
            return BigDecimal.ZERO;
        }
        Map<UUID, Long> calificadas = new HashMap<>();
        for (Object[] fila : evaluacionRepository.contarCalificacionesPorEvaluacion(
                raId, semestreId, EstadoEvaluacion.ACTIVA, EstadoMatricula.ACTIVA)) {
            calificadas.put((UUID) fila[0], (Long) fila[1]);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Evaluacion evaluacion : evaluacionRepository
                .findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(raId, semestreId, EstadoEvaluacion.ACTIVA)) {
            if (calificadas.getOrDefault(evaluacion.getId(), 0L) >= matriculados) {
                total = total.add(evaluacion.getPorcentaje());
            }
        }
        return total;
    }
}
