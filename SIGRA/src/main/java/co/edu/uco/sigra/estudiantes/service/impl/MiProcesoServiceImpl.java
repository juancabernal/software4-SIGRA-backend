package co.edu.uco.sigra.estudiantes.service.impl;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.estudiantes.dto.*;
import co.edu.uco.sigra.estudiantes.config.MatriculaConfig;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.exception.MatriculaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.repository.MiProcesoConsultaRepository;
import co.edu.uco.sigra.estudiantes.service.MiProcesoService;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.calificaciones.entity.Calificacion;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MiProcesoServiceImpl implements MiProcesoService {

    private final MiProcesoConsultaRepository consultaRepository;
    private final CurrentUser currentUser;
    private final Clock reloj;

    @Autowired
    public MiProcesoServiceImpl(MiProcesoConsultaRepository consultaRepository,
                                CurrentUser currentUser,
                                @Qualifier(MatriculaConfig.RELOJ_MATRICULAS) Clock reloj) {
        this.consultaRepository = consultaRepository;
        this.currentUser = currentUser;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MiMateriaDTO> listarMaterias() {
        UUID estudianteId = currentUser.required().id();
        LocalDate hoy = LocalDate.now(reloj);
        return materiasDelEstudiante(estudianteId).stream()
                .map(m -> Map.entry(m, situacion(m, hoy)))
                .filter(e -> e.getValue() == SituacionMateria.EN_CURSO
                        || e.getValue() == SituacionMateria.PROGRAMADA)
                .sorted(Comparator.comparingInt((Map.Entry<Matricula, SituacionMateria> e) ->
                                e.getValue() == SituacionMateria.EN_CURSO ? 0 : 1)
                        .thenComparing(e -> e.getKey().getAsignatura().getNombre(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Map.Entry::getKey)
                .map(this::aMateria)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MiMateriaDetalleDTO consultarMateria(UUID matriculaId) {
        UUID estudianteId = currentUser.required().id();
        Matricula matricula = consultaRepository.buscarMatriculaPropia(matriculaId, estudianteId)
                .orElseThrow(() -> MatriculaNoEncontradaException.porId(matriculaId));
        return ensamblarDetalles(List.of(matricula), estudianteId).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MiMateriaDetalleDTO> consultarProgreso() {
        UUID estudianteId = currentUser.required().id();
        LocalDate hoy = LocalDate.now(reloj);
        List<Matricula> materias = materiasDelEstudiante(estudianteId).stream()
                .filter(m -> situacion(m, hoy) == SituacionMateria.EN_CURSO)
                .sorted(Comparator.comparing((Matricula m) -> m.getAsignatura().getNombre(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return ensamblarDetalles(materias, estudianteId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MiMateriaDTO> consultarHistorial() {
        UUID estudianteId = currentUser.required().id();
        LocalDate hoy = LocalDate.now(reloj);
        return materiasDelEstudiante(estudianteId).stream()
                .filter(m -> situacion(m, hoy) == SituacionMateria.CURSADA
                        || situacion(m, hoy) == SituacionMateria.DESVINCULADA)
                .sorted(Comparator.comparing((Matricula m) -> m.getSemestre().getFechaInicio(),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(m -> m.getAsignatura().getNombre(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::aMateria)
                .toList();
    }

    private List<Matricula> materiasDelEstudiante(UUID estudianteId) {
        return consultaRepository.buscarMatriculasDelEstudiante(estudianteId);
    }

    private List<MiMateriaDetalleDTO> ensamblarDetalles(List<Matricula> matriculas, UUID estudianteId) {
        if (matriculas.isEmpty()) {
            return List.of();
        }

        List<UUID> asignaturaIds = matriculas.stream().map(m -> m.getAsignatura().getId()).distinct().toList();
        List<UUID> semestreIds = matriculas.stream().map(m -> m.getSemestre().getId()).distinct().toList();
        List<UUID> matriculaIds = matriculas.stream().map(Matricula::getId).toList();
        List<ResultadoAprendizaje> resultados = consultaRepository.buscarResultadosPorAsignaturas(
                asignaturaIds, EstadoRegistro.ACTIVO);
        List<UUID> resultadoIds = resultados.stream().map(ResultadoAprendizaje::getId).toList();
        List<Evaluacion> evaluaciones = resultadoIds.isEmpty()
                ? List.of()
                : consultaRepository.buscarEvaluaciones(resultadoIds, semestreIds, EstadoEvaluacion.ACTIVA);
        List<Calificacion> calificaciones = consultaRepository.buscarCalificaciones(
                matriculaIds, EstadoEvaluacion.ACTIVA);
        List<NivelLogroEstudiante> niveles = resultadoIds.isEmpty()
                ? List.of()
                : consultaRepository.buscarNiveles(estudianteId, semestreIds, resultadoIds);

        Map<UUID, List<ResultadoAprendizaje>> resultadosPorAsignatura = resultados.stream()
                .collect(Collectors.groupingBy(r -> r.getAsignatura().getId()));
        Map<UUID, List<Evaluacion>> evaluacionesPorResultado = evaluaciones.stream()
                .collect(Collectors.groupingBy(e -> e.getResultadoAprendizaje().getId()));
        Map<String, Calificacion> calificacionPorMatriculaEvaluacion = calificaciones.stream()
                .collect(Collectors.toMap(c -> clave(c.getMatricula().getId(), c.getEvaluacion().getId()),
                        Function.identity(), (primera, segunda) -> primera));
        Map<String, NivelLogroEstudiante> nivelPorEstudianteSemestreResultado = niveles.stream()
                .collect(Collectors.toMap(n -> clave(estudianteId, n.getSemestre().getId(), n.getResultadoAprendizaje().getId()),
                        Function.identity(), (primero, segundo) -> primero));

        return matriculas.stream()
                .map(m -> detalle(m, resultadosPorAsignatura.getOrDefault(m.getAsignatura().getId(), List.of()),
                        evaluacionesPorResultado, calificacionPorMatriculaEvaluacion,
                        nivelPorEstudianteSemestreResultado, estudianteId))
                .toList();
    }

    private MiMateriaDetalleDTO detalle(
            Matricula matricula,
            List<ResultadoAprendizaje> resultados,
            Map<UUID, List<Evaluacion>> evaluacionesPorResultado,
            Map<String, Calificacion> calificaciones,
            Map<String, NivelLogroEstudiante> niveles,
            UUID estudianteId) {
        List<MiResultadoAprendizajeDTO> resultadoDTOs = resultados.stream()
                .sorted(Comparator.comparing(ResultadoAprendizaje::getCodigo))
                .map(r -> {
                    List<Evaluacion> evaluaciones = evaluacionesPorResultado
                            .getOrDefault(r.getId(), List.of()).stream()
                            .filter(e -> e.getSemestre().getId().equals(matricula.getSemestre().getId()))
                            .sorted(Comparator.comparing(Evaluacion::getFechaTentativa,
                                            Comparator.nullsLast(Comparator.naturalOrder()))
                                    .thenComparing(Evaluacion::getNombre,
                                            Comparator.nullsLast(Comparator.naturalOrder())))
                            .toList();
                    List<MiEvaluacionDTO> evaluacionDTOs = evaluaciones.stream()
                            .map(e -> {
                                Calificacion calificacion = calificaciones.get(
                                        clave(matricula.getId(), e.getId()));
                                return new MiEvaluacionDTO(e.getNombre(), e.getDescripcion(),
                                        e.getFechaTentativa(), e.getPorcentaje(),
                                        calificacion == null ? null : calificacion.getNota(),
                                        calificacion == null ? null : calificacion.getObservacion());
                            }).toList();
                    NivelLogroEstudiante nivel = niveles.get(
                            clave(estudianteId, matricula.getSemestre().getId(), r.getId()));
                    return new MiResultadoAprendizajeDTO(r.getCodigo(), r.getDescripcion(),
                            nivel == null || nivel.getNivelLogro() == null
                                    ? MiNivelLogro.SIN_EVALUAR : convertirNivel(nivel.getNivelLogro()),
                            nivel == null || nivel.getNivelLogro() == null ? null : nivel.getPromedioPonderado(),
                            (int) evaluaciones.stream().filter(e -> calificaciones.containsKey(
                                    clave(matricula.getId(), e.getId()))).count(),
                            evaluaciones.size(), evaluacionDTOs);
                }).toList();
        return new MiMateriaDetalleDTO(matricula.getId(), matricula.getAsignatura().getCodigo(),
                matricula.getAsignatura().getNombre(), programaNombre(matricula.getAsignatura()),
                matricula.getSemestre().getCodigo(), matricula.getFechaMatricula(),
                situacion(matricula, LocalDate.now(reloj)), resultadoDTOs);
    }

    private MiMateriaDTO aMateria(Matricula matricula) {
        Asignatura asignatura = matricula.getAsignatura();
        return new MiMateriaDTO(matricula.getId(), asignatura.getCodigo(), asignatura.getNombre(),
                programaNombre(asignatura), matricula.getSemestre().getCodigo(),
                matricula.getFechaMatricula(), situacion(matricula, LocalDate.now(reloj)));
    }

    private static String programaNombre(Asignatura asignatura) {
        return asignatura.getPrograma() == null ? null : asignatura.getPrograma().getNombre();
    }

    public static SituacionMateria situacion(Matricula matricula, LocalDate hoy) {
        if (matricula.getEstado() == EstadoMatricula.DESVINCULADA) {
            return SituacionMateria.DESVINCULADA;
        }
        if (matricula.getSemestre().getEstado() == co.edu.uco.sigra.semestres.entity.EstadoSemestre.ACTIVO) {
            return SituacionMateria.EN_CURSO;
        }
        LocalDate inicio = matricula.getSemestre().getFechaInicio();
        return inicio != null && !inicio.isBefore(hoy)
                ? SituacionMateria.PROGRAMADA : SituacionMateria.CURSADA;
    }

    private static MiNivelLogro convertirNivel(NivelLogro nivel) {
        return switch (nivel) {
            case BAJO -> MiNivelLogro.BAJO;
            case MEDIO -> MiNivelLogro.MEDIO;
            case ALTO -> MiNivelLogro.ALTO;
            case SOBRESALIENTE -> MiNivelLogro.SOBRESALIENTE;
        };
    }

    private static String clave(UUID primero, UUID segundo) {
        return primero + ":" + segundo;
    }

    private static String clave(UUID primero, UUID segundo, UUID tercero) {
        return primero + ":" + segundo + ":" + tercero;
    }
}
