package co.edu.uco.sigra.estadisticas.service.impl;

import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.dto.ConclusionEstadoResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.ConclusionFiltroDTO;
import co.edu.uco.sigra.estadisticas.dto.ConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.EstadoConsultaConclusion;
import co.edu.uco.sigra.estadisticas.dto.GenerarConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.exception.ConclusionNoEncontradaException;
import co.edu.uco.sigra.estadisticas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.estadisticas.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.estadisticas.mapper.ConclusionMapper;
import co.edu.uco.sigra.estadisticas.repository.ConclusionDiagnosticoRepository;
import co.edu.uco.sigra.estadisticas.repository.ConclusionRepository;
import co.edu.uco.sigra.estadisticas.service.ConclusionService;
import co.edu.uco.sigra.estadisticas.service.ProveedorNivelesLogro;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeNoEncontradoException;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RF-16: conclusiones automáticas. El ADMINISTRADOR ve todo; el PROFESOR solo opera sobre sus
 * asignaturas con asignación docente ACTIVA. La ausencia de identidad se rechaza.
 */
@Service
@RequiredArgsConstructor
public class ConclusionServiceImpl implements ConclusionService {

    /** Con menos calificaciones que este mínimo, el RA no tiene datos suficientes para evaluarse. */
    public static final int MINIMO_CALIFICACIONES = 3;
    /** Más del 40% en Bajo o Medio genera la conclusión; con 40% o menos, no se genera nada. */
    public static final double UMBRAL_PORCENTAJE_INSUFICIENTE = 40.0;

    public static final String MOTIVO_DATOS_INSUFICIENTES =
            "El resultado de aprendizaje no tiene datos suficientes para evaluarlo (mínimo "
                    + MINIMO_CALIFICACIONES + " calificaciones).";
    public static final String MOTIVO_DENTRO_DEL_UMBRAL =
            "El desempeño está dentro del umbral aceptado; no se generó una conclusión.";
    public static final String MOTIVO_YA_VIGENTE =
            "Ya existe una conclusión vigente para este resultado de aprendizaje y semestre.";
    public static final String MOTIVO_GENERADA = "Se generó una nueva conclusión.";

    private final ConclusionRepository conclusionRepository;
    private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    private final SemestreRepository semestreRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final ProveedorNivelesLogro proveedorNivelesLogro;
    private final ConclusionDiagnosticoRepository conclusionDiagnosticoRepository;
    private final ConclusionMapper conclusionMapper;

    @Override
    @Transactional
    public GenerarConclusionResponseDTO generarConclusion(UUID resultadoAprendizajeId, UUID semestreId,
                                                           UsuarioPrincipal principal) {
        ResultadoAprendizaje resultadoAprendizaje = buscarResultadoAprendizaje(resultadoAprendizajeId);
        verificarAlcance(principal, resultadoAprendizaje.getAsignatura().getId());
        Semestre semestre = buscarSemestre(semestreId);

        // Solo se calcula si el RA tiene datos suficientes (mínimo 3 calificaciones).
        long evaluados = proveedorNivelesLogro.contarEvaluados(resultadoAprendizajeId, semestreId);
        if (evaluados < MINIMO_CALIFICACIONES) {
            return new GenerarConclusionResponseDTO(false, MOTIVO_DATOS_INSUFICIENTES, null);
        }

        // Si ya existe una conclusión Vigente para ese (RA, semestre), no se crea un duplicado.
        if (conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                resultadoAprendizajeId, semestreId, EstadoConclusion.VIGENTE).isPresent()) {
            return new GenerarConclusionResponseDTO(false, MOTIVO_YA_VIGENTE, null);
        }

        // Si más del 40% está en Bajo o Medio, se genera; con 40% o menos, no.
        long enBajoOMedio = proveedorNivelesLogro.contarEnBajoOMedio(resultadoAprendizajeId, semestreId);
        double porcentaje = (enBajoOMedio * 100.0) / evaluados;
        if (porcentaje <= UMBRAL_PORCENTAJE_INSUFICIENTE) {
            return new GenerarConclusionResponseDTO(false, MOTIVO_DENTRO_DEL_UMBRAL, null);
        }

        Conclusion conclusion = new Conclusion(resultadoAprendizaje, semestre, porcentaje);
        fijarDiagnostico(conclusion, resultadoAprendizajeId, semestreId, semestre, evaluados);
        Conclusion guardada = conclusionRepository.save(conclusion);
        return new GenerarConclusionResponseDTO(true, MOTIVO_GENERADA, conclusionMapper.toResponseDTO(guardada));
    }

    /**
     * Completa la foto del diagnóstico (RF-16, avance de información): no cambia ninguna regla
     * de cuándo se genera una conclusión, solo guarda más datos del momento en que se generó.
     */
    private void fijarDiagnostico(Conclusion conclusion, UUID resultadoAprendizajeId, UUID semestreId,
                                   Semestre semestre, long evaluados) {
        long enBajo = conclusionDiagnosticoRepository.countByResultadoAprendizaje_IdAndSemestre_IdAndNivelLogro(
                resultadoAprendizajeId, semestreId, NivelLogro.BAJO);
        long enMedio = conclusionDiagnosticoRepository.countByResultadoAprendizaje_IdAndSemestre_IdAndNivelLogro(
                resultadoAprendizajeId, semestreId, NivelLogro.MEDIO);

        List<Conclusion> anteriores = conclusionRepository.buscarAnteriores(
                resultadoAprendizajeId, semestre.getFechaInicio());
        Double porcentajeAnterior = null;
        String semestreAnteriorCodigo = null;
        if (!anteriores.isEmpty()) {
            Conclusion anterior = anteriores.get(0);
            porcentajeAnterior = anterior.getPorcentajeInsuficiente();
            semestreAnteriorCodigo = anterior.getSemestre().getCodigo();
        }

        conclusion.fijarDiagnostico((int) evaluados, (int) enBajo, (int) enMedio, UMBRAL_PORCENTAJE_INSUFICIENTE,
                porcentajeAnterior, semestreAnteriorCodigo);
    }

    @Override
    @Transactional
    public List<ConclusionResponseDTO> consultarConclusiones(ConclusionFiltroDTO filtro, UsuarioPrincipal principal) {
        Set<UUID> alcance = alcanceDe(principal);

        // El volumen de conclusiones es pequeño (una por RA y semestre insuficiente), igual que el
        // catálogo de asignaturas: se filtra en memoria sobre la lista completa.
        return conclusionRepository.findAll().stream()
                .filter(c -> alcance == null || alcance.contains(c.getResultadoAprendizaje().getAsignatura().getId()))
                .filter(c -> filtro.resultadoAprendizajeId() == null
                        || filtro.resultadoAprendizajeId().equals(c.getResultadoAprendizaje().getId()))
                .filter(c -> filtro.semestreId() == null || filtro.semestreId().equals(c.getSemestre().getId()))
                .filter(c -> filtro.asignaturaId() == null
                        || filtro.asignaturaId().equals(c.getResultadoAprendizaje().getAsignatura().getId()))
                .filter(c -> filtro.estado() == null || filtro.estado() == c.getEstado())
                .sorted(Comparator.comparing(Conclusion::getFechaGeneracion).reversed())
                .map(conclusionMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public ConclusionResponseDTO consultarPorId(UUID id, UsuarioPrincipal principal) {
        Conclusion conclusion = buscarConclusion(id);
        verificarAlcance(principal, conclusion.getResultadoAprendizaje().getAsignatura().getId());
        return conclusionMapper.toResponseDTO(conclusion);
    }

    /**
     * Asignaturas visibles para la identidad: el ADMINISTRADOR ve todas.
     * El PROFESOR solo ve las conclusiones de sus asignaturas con asignación docente ACTIVA.
     */
    private Set<UUID> alcanceDe(UsuarioPrincipal principal) {
        if (principal == null) {
            throw new PermisoInsuficienteException();
        }
        if (principal.rol() == RolUsuario.ADMINISTRADOR) {
            return null;
        }
        if (principal.rol() == RolUsuario.PROFESOR) {
            return asignacionDocenteRepository.findByProfesor_IdAndEstado(principal.id(), EstadoRegistro.ACTIVO)
                    .stream()
                    .map(asignacion -> asignacion.getAsignatura().getId())
                    .collect(Collectors.toSet());
        }
        throw new PermisoInsuficienteException();
    }

    private void verificarAlcance(UsuarioPrincipal principal, UUID asignaturaId) {
        Set<UUID> alcance = alcanceDe(principal);
        if (alcance != null && !alcance.contains(asignaturaId)) {
            throw new PermisoInsuficienteException();
        }
    }

    @Override
    @Transactional
    public ConclusionEstadoResponseDTO consultarEstado(UUID resultadoAprendizajeId, UUID semestreId,
                                                         UsuarioPrincipal principal) {
        ResultadoAprendizaje resultadoAprendizaje = buscarResultadoAprendizaje(resultadoAprendizajeId);
        verificarAlcance(principal, resultadoAprendizaje.getAsignatura().getId());
        buscarSemestre(semestreId);

        Optional<Conclusion> vigente = conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                resultadoAprendizajeId, semestreId, EstadoConclusion.VIGENTE);
        if (vigente.isPresent()) {
            Conclusion conclusion = vigente.get();
            return new ConclusionEstadoResponseDTO(EstadoConsultaConclusion.VIGENTE,
                    conclusionMapper.toResponseDTO(conclusion), null, conclusion.getPorcentajeInsuficiente());
        }

        long evaluados = proveedorNivelesLogro.contarEvaluados(resultadoAprendizajeId, semestreId);
        if (evaluados < MINIMO_CALIFICACIONES) {
            return new ConclusionEstadoResponseDTO(EstadoConsultaConclusion.SIN_DATOS_SUFICIENTES, null,
                    (int) evaluados, null);
        }

        long enBajoOMedio = proveedorNivelesLogro.contarEnBajoOMedio(resultadoAprendizajeId, semestreId);
        double porcentaje = (enBajoOMedio * 100.0) / evaluados;
        if (porcentaje <= UMBRAL_PORCENTAJE_INSUFICIENTE) {
            return new ConclusionEstadoResponseDTO(EstadoConsultaConclusion.SATISFACTORIO, null,
                    (int) evaluados, porcentaje);
        }

        Optional<Conclusion> atendida = conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                resultadoAprendizajeId, semestreId, EstadoConclusion.ATENDIDA);
        if (atendida.isPresent()) {
            Conclusion conclusion = atendida.get();
            return new ConclusionEstadoResponseDTO(EstadoConsultaConclusion.ATENDIDA,
                    conclusionMapper.toResponseDTO(conclusion), (int) evaluados, porcentaje);
        }

        // Supera el umbral y hay datos suficientes, pero todavía no hay una conclusión Atendida
        // registrada (p.ej. aún no corre el recálculo que genera la Vigente): no hay nada pendiente
        // que mostrarle al profesor por ahora.
        return new ConclusionEstadoResponseDTO(EstadoConsultaConclusion.SATISFACTORIO, null,
                (int) evaluados, porcentaje);
    }

    private ResultadoAprendizaje buscarResultadoAprendizaje(UUID id) {
        return resultadoAprendizajeRepository.findById(id)
                .orElseThrow(() -> new ResultadoAprendizajeNoEncontradoException(id));
    }

    private Semestre buscarSemestre(UUID id) {
        return semestreRepository.findById(id)
                .orElseThrow(() -> new SemestreNoEncontradoException(id));
    }

    private Conclusion buscarConclusion(UUID id) {
        return conclusionRepository.findById(id)
                .orElseThrow(() -> new ConclusionNoEncontradaException(id));
    }
}
