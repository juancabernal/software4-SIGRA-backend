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
import co.edu.uco.sigra.evaluaciones.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.resultadosaprendizaje.dto.EstadoRendimiento;
import co.edu.uco.sigra.resultadosaprendizaje.dto.RendimientoAsignaturaResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.RendimientoResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.resultadosaprendizaje.service.RendimientoResultadoAprendizajeService;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
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
 * <p>
 * «Sin evaluar» se decide con las calificaciones reales (no con la tabla de niveles), así que un RA
 * sin calificaciones vigentes nunca muestra un nivel viejo. El semestre es el activo salvo que se pida otro.
 */
@Service
@RequiredArgsConstructor
public class RendimientoResultadoAprendizajeServiceImpl implements RendimientoResultadoAprendizajeService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    /** RF-15: sin 3 calificaciones por estudiante no hay nivel, y cada evaluación aporta una. */
    static final int MINIMO_EVALUACIONES = 3;

    private final AsignaturaRepository asignaturaRepository;
    private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    private final EvaluacionService evaluacionService;
    private final EvaluacionRepository evaluacionRepository;
    private final MatriculaRepository matriculaRepository;
    private final NivelLogroEstudianteRepository nivelLogroEstudianteRepository;
    private final SemestreRepository semestreRepository;

    @Override
    @Transactional
    public RendimientoAsignaturaResponseDTO consultarPorAsignatura(UUID asignaturaId, UUID semestreId) {
        if (!asignaturaRepository.existsById(asignaturaId)) {
            throw new AsignaturaNoEncontradaException(asignaturaId);
        }
        Semestre semestre;
        if (semestreId == null) {
            Optional<Semestre> semestreActivo = evaluacionService.consultarSemestreActivo();
            if (semestreActivo.isEmpty()) {
                return new RendimientoAsignaturaResponseDTO(asignaturaId, null, null, List.of());
            }
            semestre = semestreActivo.get();
        } else {
            semestre = semestreRepository.findById(semestreId)
                    .orElseThrow(() -> new SemestreNoEncontradoException(semestreId));
        }
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
        // Calificaciones reales de evaluaciones ACTIVAS con matrículas ACTIVAS: decide SIN_EVALUAR en vivo.
        Map<UUID, Long> calificadas = new HashMap<>();
        for (Object[] fila : evaluacionRepository.contarCalificacionesPorEvaluacion(
                ra.getId(), semestreId, EstadoEvaluacion.ACTIVA, EstadoMatricula.ACTIVA)) {
            calificadas.put((UUID) fila[0], (Long) fila[1]);
        }
        boolean hayCalificaciones = calificadas.values().stream().anyMatch(cantidad -> cantidad > 0);

        List<Double> promedios = nivelLogroEstudianteRepository
                .findByResultadoAprendizaje_IdAndSemestre_Id(ra.getId(), semestreId).stream()
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
        List<Evaluacion> activas = evaluacionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                ra.getId(), semestreId, EstadoEvaluacion.ACTIVA);
        BigDecimal evaluado = calcularPorcentajeEvaluado(matriculados, activas, calificadas);
        boolean completo = activas.size() >= MINIMO_EVALUACIONES
                && asignado.compareTo(CIEN) == 0 && evaluado.compareTo(CIEN) == 0;
        return new RendimientoResultadoAprendizajeResponseDTO(ra.getId(), ra.getCodigo(), estado, nivel,
                promedioNota, asignado, evaluado, activas.size(), MINIMO_EVALUACIONES, completo);
    }

    /** Suma el % de las evaluaciones ACTIVAS calificadas a todos los matriculados ACTIVOS. */
    private BigDecimal calcularPorcentajeEvaluado(int matriculados, List<Evaluacion> activas,
                                                  Map<UUID, Long> calificadas) {
        if (matriculados == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Evaluacion evaluacion : activas) {
            if (calificadas.getOrDefault(evaluacion.getId(), 0L) >= matriculados) {
                total = total.add(evaluacion.getPorcentaje());
            }
        }
        return total;
    }
}
