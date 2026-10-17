package co.edu.uco.sigra.estadisticas.service.impl;

import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.dto.DistribucionNivelLogroDTO;
import co.edu.uco.sigra.estadisticas.dto.EstadisticasResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.GenerarConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RangoNivelLogroResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RecalculoEstadisticasResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.estadisticas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.estadisticas.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
import co.edu.uco.sigra.estadisticas.service.CalificacionPonderada;
import co.edu.uco.sigra.estadisticas.service.CatalogoNivelesLogro;
import co.edu.uco.sigra.estadisticas.service.ConclusionService;
import co.edu.uco.sigra.estadisticas.service.FuenteCalificaciones;
import co.edu.uco.sigra.estadisticas.service.NivelLogroService;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.repository.EstudianteRepository;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeNoEncontradoException;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * RF-14 / RF-15: nivel de logro por estudiante y estadísticas descriptivas por (RA, semestre).
 * <p>
 * Reglas confirmadas:
 * <ul>
 *   <li>Nivel del estudiante en el RA = promedio ponderado de sus calificaciones, normalizado por la suma
 *       de los porcentajes de las evaluaciones calificadas (Σ nota·% / Σ %), redondeado a 2 decimales y
 *       clasificado con el catálogo de 4 rangos ({@link CatalogoNivelesLogro}).</li>
 *   <li>Umbral: el nivel solo se calcula si el estudiante tiene 3 o más calificaciones en ese RA.</li>
 *   <li>Al terminar cada recálculo se llama a {@link ConclusionService#generarConclusion} (RF-16).</li>
 * </ul>
 * Alcance por rol: mismo patrón que {@code ConclusionServiceImpl}; sin identidad falla cerrado (403).
 */
@Service
@RequiredArgsConstructor
public class NivelLogroServiceImpl implements NivelLogroService {

    /** Calificaciones mínimas del estudiante en el RA para calcular su nivel de logro. */
    public static final int MINIMO_CALIFICACIONES_ESTUDIANTE = 3;

    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");
    private static final double CIEN = 100.0;

    private final NivelLogroEstudianteRepository nivelLogroEstudianteRepository;
    private final FuenteCalificaciones fuenteCalificaciones;
    private final ConclusionService conclusionService;
    private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    private final SemestreRepository semestreRepository;
    private final EstudianteRepository estudianteRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;

    @Override
    @Transactional
    public RecalculoEstadisticasResponseDTO recalcular(UUID resultadoAprendizajeId, UUID semestreId,
                                                       UsuarioPrincipal principal) {
        ResultadoAprendizaje resultadoAprendizaje = buscarResultadoAprendizaje(resultadoAprendizajeId);
        Semestre semestre = buscarSemestre(semestreId);
        verificarAlcance(resultadoAprendizaje, principal);

        Map<UUID, List<CalificacionPonderada>> porEstudiante = fuenteCalificaciones
                .listarCalificaciones(resultadoAprendizajeId, semestreId).stream()
                .collect(Collectors.groupingBy(CalificacionPonderada::estudianteId));

        Map<UUID, NivelLogroEstudiante> existentes = nivelLogroEstudianteRepository
                .findByResultadoAprendizaje_IdAndSemestre_Id(resultadoAprendizajeId, semestreId).stream()
                .collect(Collectors.toMap(NivelLogroEstudiante::getEstudianteId, Function.identity()));

        LocalDateTime ahora = LocalDateTime.now(ZONA_COLOMBIA);
        List<NivelLogroEstudiante> aGuardar = new ArrayList<>();

        porEstudiante.forEach((estudianteId, calificaciones) -> {
            NivelLogroEstudiante fila = existentes.computeIfAbsent(estudianteId, id -> new NivelLogroEstudiante(
                    resultadoAprendizaje, semestre, estudianteRepository.getReferenceById(id)));
            if (calificaciones.size() >= MINIMO_CALIFICACIONES_ESTUDIANTE) {
                double promedio = redondear(promedioPonderado(calificaciones));
                fila.actualizar(calificaciones.size(), promedio, CatalogoNivelesLogro.clasificar(promedio), ahora);
            } else {
                fila.actualizar(calificaciones.size(), null, null, ahora);
            }
            aGuardar.add(fila);
        });

        // Quien tenía nivel y ya no tiene calificaciones en el RA queda en 0, sin promedio ni nivel.
        existentes.forEach((estudianteId, fila) -> {
            if (!porEstudiante.containsKey(estudianteId)) {
                fila.actualizar(0, null, null, ahora);
                aGuardar.add(fila);
            }
        });

        nivelLogroEstudianteRepository.saveAll(aGuardar);
        nivelLogroEstudianteRepository.flush();

        // RF-16: la conclusión se evalúa justo al recalcular. Es idempotente; generada=false no es error.
        GenerarConclusionResponseDTO conclusion = conclusionService.generarConclusion(resultadoAprendizajeId, semestreId,
                principal);
        return new RecalculoEstadisticasResponseDTO(construirEstadisticas(resultadoAprendizaje, semestre), conclusion);
    }

    @Override
    @Transactional
    public EstadisticasResultadoAprendizajeResponseDTO consultarEstadisticas(UUID resultadoAprendizajeId,
                                                                             UUID semestreId,
                                                                             UsuarioPrincipal principal) {
        ResultadoAprendizaje resultadoAprendizaje = buscarResultadoAprendizaje(resultadoAprendizajeId);
        Semestre semestre = buscarSemestre(semestreId);
        verificarAlcance(resultadoAprendizaje, principal);
        return construirEstadisticas(resultadoAprendizaje, semestre);
    }

    @Override
    public List<RangoNivelLogroResponseDTO> consultarCatalogo() {
        return CatalogoNivelesLogro.rangos().stream()
                .map(r -> new RangoNivelLogroResponseDTO(r.nivel(), r.notaMinima(), r.notaLimite(), r.limiteIncluido()))
                .toList();
    }

    /** Σ (nota × porcentaje) / Σ porcentaje: normaliza aunque los porcentajes calificados no sumen 100. */
    static double promedioPonderado(List<CalificacionPonderada> calificaciones) {
        double sumaPonderada = 0;
        double sumaPorcentajes = 0;
        for (CalificacionPonderada c : calificaciones) {
            sumaPonderada += c.nota() * c.porcentajeEvaluacion();
            sumaPorcentajes += c.porcentajeEvaluacion();
        }
        return sumaPonderada / sumaPorcentajes;
    }

    private EstadisticasResultadoAprendizajeResponseDTO construirEstadisticas(ResultadoAprendizaje resultadoAprendizaje,
                                                                              Semestre semestre) {
        List<NivelLogroEstudiante> filas = nivelLogroEstudianteRepository
                .findByResultadoAprendizaje_IdAndSemestre_Id(resultadoAprendizaje.getId(), semestre.getId());

        Set<UUID> conCalificaciones = filas.stream()
                .filter(f -> f.getCantidadCalificaciones() > 0)
                .map(NivelLogroEstudiante::getEstudianteId)
                .collect(Collectors.toSet());
        List<NivelLogroEstudiante> evaluados = filas.stream().filter(f -> f.getNivelLogro() != null).toList();
        long sinDatosSuficientes = filas.stream()
                .filter(f -> f.getCantidadCalificaciones() > 0 && f.getNivelLogro() == null)
                .count();

        Set<UUID> matriculadosActivos = matriculaRepository
                .buscarPorAsignaturaYSemestre(resultadoAprendizaje.getAsignatura().getId(), semestre.getId(),
                        EstadoMatricula.ACTIVA).stream()
                .map(m -> m.getEstudiante().getId())
                .collect(Collectors.toSet());
        long sinEvaluar = matriculadosActivos.stream().filter(id -> !conCalificaciones.contains(id)).count();
        Set<UUID> todos = new HashSet<>(matriculadosActivos);
        todos.addAll(conCalificaciones);

        Double promedioNota = evaluados.isEmpty() ? null : redondear(evaluados.stream()
                .mapToDouble(NivelLogroEstudiante::getPromedioPonderado).average().orElseThrow());
        Double promedioPorcentaje = promedioNota == null ? null
                : redondear(promedioNota / CatalogoNivelesLogro.NOTA_MAXIMA * CIEN);

        Map<NivelLogro, Long> conteo = evaluados.stream()
                .collect(Collectors.groupingBy(NivelLogroEstudiante::getNivelLogro, Collectors.counting()));
        List<DistribucionNivelLogroDTO> distribucion = Arrays.stream(NivelLogro.values())
                .map(nivel -> {
                    long cantidad = conteo.getOrDefault(nivel, 0L);
                    double porcentaje = evaluados.isEmpty() ? 0.0 : redondear(cantidad * CIEN / evaluados.size());
                    return new DistribucionNivelLogroDTO(nivel, cantidad, porcentaje);
                })
                .toList();

        LocalDateTime fechaCalculo = filas.stream()
                .map(NivelLogroEstudiante::getFechaCalculo)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new EstadisticasResultadoAprendizajeResponseDTO(
                resultadoAprendizaje.getId(), resultadoAprendizaje.getCodigo(),
                resultadoAprendizaje.getAsignatura().getId(), semestre.getId(), semestre.getCodigo(),
                todos.size(), evaluados.size(), sinDatosSuficientes, sinEvaluar,
                promedioNota, promedioPorcentaje, distribucion, fechaCalculo);
    }

    /**
     * ADMINISTRADOR: todo. PROFESOR: solo los RA de asignaturas donde tiene asignación docente ACTIVA.
     * Sin identidad o con cualquier otro rol: 403 (falla cerrado, igual que ConclusionServiceImpl).
     */
    private void verificarAlcance(ResultadoAprendizaje resultadoAprendizaje, UsuarioPrincipal principal) {
        if (principal == null) {
            throw new PermisoInsuficienteException();
        }
        if (principal.rol() == RolUsuario.ADMINISTRADOR) {
            return;
        }
        if (principal.rol() == RolUsuario.PROFESOR) {
            UUID asignaturaId = resultadoAprendizaje.getAsignatura().getId();
            boolean asignado = asignacionDocenteRepository
                    .findByProfesor_IdAndEstado(principal.id(), EstadoRegistro.ACTIVO).stream()
                    .anyMatch(a -> a.getAsignatura().getId().equals(asignaturaId));
            if (asignado) {
                return;
            }
        }
        throw new PermisoInsuficienteException();
    }

    private static double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private ResultadoAprendizaje buscarResultadoAprendizaje(UUID id) {
        return resultadoAprendizajeRepository.findById(id)
                .orElseThrow(() -> new ResultadoAprendizajeNoEncontradoException(id));
    }

    private Semestre buscarSemestre(UUID id) {
        return semestreRepository.findById(id)
                .orElseThrow(() -> new SemestreNoEncontradoException(id));
    }
}
