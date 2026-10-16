package co.edu.uco.sigra.calificaciones.service.impl;

import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.calificaciones.config.CalificacionConfig;
import co.edu.uco.sigra.calificaciones.dto.CalificacionModificarRequestDTO;
import co.edu.uco.sigra.calificaciones.dto.CalificacionRequestDTO;
import co.edu.uco.sigra.calificaciones.dto.CalificacionResponseDTO;
import co.edu.uco.sigra.calificaciones.dto.EvaluacionCalificableDTO;
import co.edu.uco.sigra.calificaciones.dto.FilaPlanillaDTO;
import co.edu.uco.sigra.calificaciones.dto.HistorialCalificacionDTO;
import co.edu.uco.sigra.calificaciones.dto.PlanillaCalificacionesDTO;
import co.edu.uco.sigra.calificaciones.entity.Calificacion;
import co.edu.uco.sigra.calificaciones.entity.HistorialCalificacion;
import co.edu.uco.sigra.calificaciones.exception.CalificacionDuplicadaException;
import co.edu.uco.sigra.calificaciones.exception.CalificacionNoEncontradaException;
import co.edu.uco.sigra.calificaciones.exception.CalificacionReglaNegocioException;
import co.edu.uco.sigra.calificaciones.mapper.CalificacionMapper;
import co.edu.uco.sigra.calificaciones.repository.CalificacionRepository;
import co.edu.uco.sigra.calificaciones.repository.HistorialCalificacionRepository;
import co.edu.uco.sigra.calificaciones.service.CalificacionService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.exception.MatriculaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionNoEncontradaException;
import co.edu.uco.sigra.evaluaciones.repository.EvaluacionRepository;
import co.edu.uco.sigra.semestres.entity.Semestre;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CalificacionServiceImpl implements CalificacionService {

    private static final String SIN_ALCANCE = "La asignatura de la evaluación no está asignada a este profesor";

    private final CalificacionRepository calificacionRepository;
    private final HistorialCalificacionRepository historialRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final MatriculaRepository matriculaRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final CalificacionMapper calificacionMapper;
    private final Clock reloj;

    public CalificacionServiceImpl(CalificacionRepository calificacionRepository,
                                   HistorialCalificacionRepository historialRepository,
                                   EvaluacionRepository evaluacionRepository,
                                   MatriculaRepository matriculaRepository,
                                   AsignacionDocenteRepository asignacionDocenteRepository,
                                   CalificacionMapper calificacionMapper,
                                   @Qualifier(CalificacionConfig.RELOJ_CALIFICACIONES) Clock reloj) {
        this.calificacionRepository = calificacionRepository;
        this.historialRepository = historialRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.matriculaRepository = matriculaRepository;
        this.asignacionDocenteRepository = asignacionDocenteRepository;
        this.calificacionMapper = calificacionMapper;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ Evaluaciones calificables

    @Override
    @Transactional
    public List<EvaluacionCalificableDTO> listarEvaluaciones(UUID resultadoAprendizajeId, UUID semestreId) {
        return evaluacionRepository
                .findByResultadoAprendizaje_IdAndSemestre_IdOrderByFechaTentativaAscNombreAsc(
                        resultadoAprendizajeId, semestreId)
                .stream()
                .map(e -> new EvaluacionCalificableDTO(e.getId(), e.getNombre(), e.getFechaTentativa(),
                        e.getPorcentaje(), e.getEstado().name()))
                .toList();
    }

    // ------------------------------------------------------------------ Planilla

    @Override
    @Transactional
    public PlanillaCalificacionesDTO consultarPlanilla(UUID evaluacionId, UsuarioPrincipal usuario) {
        Evaluacion evaluacion = buscarEvaluacion(evaluacionId);
        Asignatura asignatura = evaluacion.getResultadoAprendizaje().getAsignatura();
        verificarDueno(usuario, asignatura);
        Semestre semestre = evaluacion.getSemestre();

        Map<UUID, Calificacion> porMatricula = calificacionRepository.buscarPorEvaluacion(evaluacionId).stream()
                .collect(Collectors.toMap(c -> c.getMatricula().getId(), Function.identity()));

        // Activas siempre; una inactiva (desvinculada) solo si conserva nota en esta evaluación.
        List<FilaPlanillaDTO> filas = matriculaRepository
                .buscarPorAsignaturaYSemestre(asignatura.getId(), semestre.getId(), null).stream()
                .filter(m -> m.getEstado() == EstadoMatricula.ACTIVA || porMatricula.containsKey(m.getId()))                .map(m -> aFila(m, porMatricula.get(m.getId())))
                .sorted(Comparator.comparing(FilaPlanillaDTO::nombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int calificados = (int) filas.stream().filter(f -> f.calificacionId() != null).count();

        return new PlanillaCalificacionesDTO(
                evaluacion.getId(), evaluacion.getNombre(), evaluacion.getFechaTentativa(),
                evaluacion.getPorcentaje(), evaluacion.getEstado().name(),
                semestre.getId(), semestre.getCodigo(),
                asignatura.getId(), asignatura.getCodigo(), asignatura.getNombre(),
                evaluacion.getResultadoAprendizaje().getCodigo(),
                filas.size(), calificados, filas);
    }

    // ------------------------------------------------------------------ Registrar

    @Override
    @Transactional
    public CalificacionResponseDTO registrar(UUID evaluacionId, CalificacionRequestDTO dto, UsuarioPrincipal usuario) {
        Evaluacion evaluacion = buscarEvaluacion(evaluacionId);
        Asignatura asignatura = evaluacion.getResultadoAprendizaje().getAsignatura();
        verificarDueno(usuario, asignatura);
        verificarEvaluacionActiva(evaluacion);

        Matricula matricula = matriculaRepository.findById(dto.matriculaId())
                .orElseThrow(() -> MatriculaNoEncontradaException.porId(dto.matriculaId()));
        if (!Objects.equals(matricula.getAsignatura().getId(), asignatura.getId())) {
            throw CalificacionReglaNegocioException.matriculaDeOtraAsignatura();
        }
        if (!Objects.equals(matricula.getSemestre().getId(), evaluacion.getSemestre().getId())) {
            throw CalificacionReglaNegocioException.semestreNoCoincide();
        }
        verificarMatriculaActiva(matricula);
        if (calificacionRepository.existsByEvaluacion_IdAndMatricula_Id(evaluacionId, matricula.getId())) {
            throw new CalificacionDuplicadaException();
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        boolean tarde = esFueraDeFecha(evaluacion, ahora);

        Calificacion calificacion = new Calificacion();
        calificacion.setEvaluacion(evaluacion);
        calificacion.setMatricula(matricula);
        calificacion.setNota(dto.nota());
        calificacion.setObservacion(dto.observacion());
        calificacion.setFechaRegistro(ahora);
        calificacion.setFueraDeFecha(tarde);
        Calificacion guardada = calificacionRepository.saveAndFlush(calificacion);

        historialRepository.save(HistorialCalificacion.registro(guardada, usuario.id(), usuario.rol(), tarde, ahora));
        return calificacionMapper.toResponseDTO(guardada);
    }

    // ------------------------------------------------------------------ Modificar

    @Override
    @Transactional
    public CalificacionResponseDTO modificar(UUID calificacionId, CalificacionModificarRequestDTO dto,
                                             UsuarioPrincipal usuario) {
        Calificacion calificacion = buscarCalificacion(calificacionId);
        Evaluacion evaluacion = calificacion.getEvaluacion();
        verificarDueno(usuario, evaluacion.getResultadoAprendizaje().getAsignatura());
        verificarEvaluacionActiva(evaluacion);
        verificarMatriculaActiva(calificacion.getMatricula());

        boolean mismaNota = calificacion.getNota().compareTo(dto.nota()) == 0;
        boolean mismaObservacion = calificacion.getObservacion().equals(dto.observacion());
        if (mismaNota && mismaObservacion) {
            // Sin cambios no hay evento que auditar.
            return calificacionMapper.toResponseDTO(calificacion);
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        boolean tarde = esFueraDeFecha(evaluacion, ahora);
        HistorialCalificacion evento = HistorialCalificacion.modificacion(calificacion.getId(),
                calificacion.getNota(), calificacion.getObservacion(), dto.nota(), dto.observacion(),
                usuario.id(), usuario.rol(), tarde, ahora);

        calificacion.setNota(dto.nota());
        calificacion.setObservacion(dto.observacion());
        calificacion.setFechaModificacion(ahora);
        // Una vez fuera de fecha, sigue fuera de fecha: la constancia no se borra.
        calificacion.setFueraDeFecha(calificacion.isFueraDeFecha() || tarde);

        Calificacion guardada = calificacionRepository.save(calificacion);
        historialRepository.save(evento);
        return calificacionMapper.toResponseDTO(guardada);
    }

    // ------------------------------------------------------------------ Historial

    @Override
    @Transactional
    public List<HistorialCalificacionDTO> consultarHistorial(UUID calificacionId, UsuarioPrincipal usuario) {
        Calificacion calificacion = buscarCalificacion(calificacionId);
        verificarDueno(usuario, calificacion.getEvaluacion().getResultadoAprendizaje().getAsignatura());
        return calificacionMapper.toHistorialDTOList(
                historialRepository.findByCalificacionIdOrderByFechaEventoAsc(calificacionId));
    }

    // ------------------------------------------------------------------ Auxiliares

    private Evaluacion buscarEvaluacion(UUID id) {
        return evaluacionRepository.findById(id)
                .orElseThrow(() -> new EvaluacionNoEncontradaException(id));
    }

    private Calificacion buscarCalificacion(UUID id) {
        return calificacionRepository.findById(id)
                .orElseThrow(() -> new CalificacionNoEncontradaException(id));
    }

    private void verificarDueno(UsuarioPrincipal usuario, Asignatura asignatura) {
        boolean asignada = usuario.rol() == RolUsuario.PROFESOR
                && asignacionDocenteRepository.existsByProfesor_IdAndAsignatura_IdAndEstado(
                usuario.id(), asignatura.getId(), EstadoRegistro.ACTIVO);
        if (!asignada) {
            throw new AccessDeniedException(SIN_ALCANCE);
        }
    }

    private static void verificarEvaluacionActiva(Evaluacion evaluacion) {
        if (evaluacion.getEstado() != EstadoEvaluacion.ACTIVA) {
            throw CalificacionReglaNegocioException.evaluacionInactiva();
        }
    }

    private static void verificarMatriculaActiva(Matricula matricula) {
        if (matricula.getEstado() != EstadoMatricula.ACTIVA) {            throw CalificacionReglaNegocioException.matriculaNoActiva();
        }
    }

    private static boolean esFueraDeFecha(Evaluacion evaluacion, LocalDateTime momento) {
        return momento.toLocalDate().isAfter(evaluacion.getFechaTentativa());
    }

    private static FilaPlanillaDTO aFila(Matricula matricula, Calificacion calificacion) {
        Estudiante estudiante = matricula.getEstudiante();
        boolean activa = matricula.getEstado() == EstadoMatricula.ACTIVA;        if (calificacion == null) {
            return new FilaPlanillaDTO(matricula.getId(), estudiante.getId(), estudiante.getNumeroDocumento(),
                    estudiante.getNombreCompleto(), activa, null, null, null, null, null, null);
        }
        return new FilaPlanillaDTO(matricula.getId(), estudiante.getId(), estudiante.getNumeroDocumento(),
                estudiante.getNombreCompleto(), activa, calificacion.getId(), calificacion.getNota(),
                calificacion.getObservacion(), calificacion.isFueraDeFecha(), calificacion.getFechaRegistro(),
                calificacion.getFechaModificacion());
    }
}