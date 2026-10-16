package co.edu.uco.sigra.evaluaciones.service.impl;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.evaluaciones.config.EvaluacionConfig;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionActualizarRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionCrearRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionResponseDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionesRaResumenDTO;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionNoEncontradaException;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionReglaNegocioException;
import co.edu.uco.sigra.evaluaciones.exception.NombreEvaluacionDuplicadoException;
import co.edu.uco.sigra.evaluaciones.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.evaluaciones.mapper.EvaluacionMapper;
import co.edu.uco.sigra.evaluaciones.repository.EvaluacionRepository;
import co.edu.uco.sigra.evaluaciones.service.ConsultaCalificacionesEvaluacion;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeNoEncontradoException;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Reglas de RF-10 / RF-11 aprobadas:
 * <ul>
 *   <li>Se crea sobre un RA ACTIVO de una asignatura ACTIVA, en el semestre activo (el que contiene la
 *       fecha de hoy), con la fecha tentativa dentro de ese semestre.</li>
 *   <li>La suma de porcentajes de las evaluaciones ACTIVAS de un (RA, semestre) no supera 100; se valida al
 *       crear, al modificar el porcentaje y al reactivar.</li>
 *   <li>Nombre único por (RA, semestre) sin distinguir mayúsculas.</li>
 *   <li>Con calificaciones registradas solo se editan nombre y descripción. Una evaluación inactiva no
 *       se edita. Las de semestres que no son el activo son de solo lectura.</li>
 *   <li>El profesor solo opera sobre asignaturas con asignación docente ACTIVA.</li>
 * </ul>
 */
@Service
public class EvaluacionServiceImpl implements EvaluacionService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final EvaluacionRepository evaluacionRepository;
    private final EvaluacionMapper evaluacionMapper;
    private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    private final SemestreRepository semestreRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final ConsultaCalificacionesEvaluacion consultaCalificaciones;
    private final Clock reloj;

    public EvaluacionServiceImpl(EvaluacionRepository evaluacionRepository,
                                 EvaluacionMapper evaluacionMapper,
                                 ResultadoAprendizajeRepository resultadoAprendizajeRepository,
                                 SemestreRepository semestreRepository,
                                 AsignacionDocenteRepository asignacionDocenteRepository,
                                 ConsultaCalificacionesEvaluacion consultaCalificaciones,
                                 @Qualifier(EvaluacionConfig.RELOJ_EVALUACIONES) Clock reloj) {
        this.evaluacionRepository = evaluacionRepository;
        this.evaluacionMapper = evaluacionMapper;
        this.resultadoAprendizajeRepository = resultadoAprendizajeRepository;
        this.semestreRepository = semestreRepository;
        this.asignacionDocenteRepository = asignacionDocenteRepository;
        this.consultaCalificaciones = consultaCalificaciones;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ RF-10a

    @Override
    @Transactional
    public EvaluacionResponseDTO crear(UUID resultadoAprendizajeId, EvaluacionCrearRequestDTO dto,
                                       UsuarioPrincipal principal) {
        ResultadoAprendizaje resultado = buscarResultado(resultadoAprendizajeId);
        verificarDueno(principal, resultado.getAsignatura());
        verificarResultadoYAsignaturaActivos(resultado);

        Semestre semestre = obtenerSemestreActivo()
                .orElseThrow(EvaluacionReglaNegocioException::sinSemestreActivo);
        verificarFechaEnSemestre(dto.fechaTentativa(), semestre);

        if (evaluacionRepository.existsByResultadoAprendizaje_IdAndSemestre_IdAndNombreIgnoreCase(
                resultadoAprendizajeId, semestre.getId(), dto.nombre())) {
            throw new NombreEvaluacionDuplicadoException();
        }
        verificarPorcentaje(sumarActivas(resultadoAprendizajeId, semestre.getId()), dto.porcentaje());

        Evaluacion evaluacion = evaluacionMapper.toEntity(dto);
        evaluacion.setResultadoAprendizaje(resultado);
        evaluacion.setSemestre(semestre);
        evaluacion.setEstado(EstadoEvaluacion.ACTIVA);
        evaluacion.setInactivadaConRa(false);

        return aResponse(evaluacionRepository.save(evaluacion));
    }

    // ------------------------------------------------------------------ RF-10b

    @Override
    @Transactional
    public EvaluacionResponseDTO modificar(UUID id, EvaluacionActualizarRequestDTO dto, UsuarioPrincipal principal) {
        Evaluacion evaluacion = buscarEvaluacion(id);
        verificarDueno(principal, evaluacion.getResultadoAprendizaje().getAsignatura());
        verificarSemestreActivo(evaluacion);
        if (evaluacion.getEstado() == EstadoEvaluacion.INACTIVA) {
            throw EvaluacionReglaNegocioException.modificarInactiva();
        }

        boolean cambiaFecha = !dto.fechaTentativa().equals(evaluacion.getFechaTentativa());
        boolean cambiaPorcentaje = dto.porcentaje().compareTo(evaluacion.getPorcentaje()) != 0;
        if ((cambiaFecha || cambiaPorcentaje) && consultaCalificaciones.tieneCalificaciones(id)) {
            throw EvaluacionReglaNegocioException.camposBloqueadosPorCalificaciones();
        }

        UUID raId = evaluacion.getResultadoAprendizaje().getId();
        UUID semestreId = evaluacion.getSemestre().getId();
        if (cambiaFecha) {
            verificarFechaEnSemestre(dto.fechaTentativa(), evaluacion.getSemestre());
        }
        if (evaluacionRepository.existsByResultadoAprendizaje_IdAndSemestre_IdAndNombreIgnoreCaseAndIdNot(
                raId, semestreId, dto.nombre(), id)) {
            throw new NombreEvaluacionDuplicadoException();
        }
        if (cambiaPorcentaje) {
            BigDecimal asignadoSinEsta = evaluacionRepository.sumarPorcentajesExcluyendo(
                    raId, semestreId, EstadoEvaluacion.ACTIVA, id);
            verificarPorcentaje(asignadoSinEsta, dto.porcentaje());
        }

        evaluacion.setNombre(dto.nombre());
        evaluacion.setFechaTentativa(dto.fechaTentativa());
        evaluacion.setPorcentaje(dto.porcentaje());
        evaluacion.setDescripcion(dto.descripcion());
        return aResponse(evaluacionRepository.save(evaluacion));
    }

    // ------------------------------------------------------------------ RF-10c

    @Override
    @Transactional
    public EvaluacionResponseDTO inactivar(UUID id, UsuarioPrincipal principal) {
        Evaluacion evaluacion = buscarEvaluacion(id);
        verificarDueno(principal, evaluacion.getResultadoAprendizaje().getAsignatura());
        verificarSemestreActivo(evaluacion);
        if (evaluacion.getEstado() == EstadoEvaluacion.INACTIVA) {
            throw EvaluacionReglaNegocioException.yaInactiva();
        }
        // Las calificaciones registradas no se tocan: solo cambia el estado (eliminación lógica).
        evaluacion.setEstado(EstadoEvaluacion.INACTIVA);
        evaluacion.setInactivadaConRa(false);
        return aResponse(evaluacionRepository.save(evaluacion));
    }

    @Override
    @Transactional
    public EvaluacionResponseDTO reactivar(UUID id, UsuarioPrincipal principal) {
        Evaluacion evaluacion = buscarEvaluacion(id);
        ResultadoAprendizaje resultado = evaluacion.getResultadoAprendizaje();
        verificarDueno(principal, resultado.getAsignatura());
        verificarSemestreActivo(evaluacion);
        if (evaluacion.getEstado() == EstadoEvaluacion.ACTIVA) {
            throw EvaluacionReglaNegocioException.yaActiva();
        }
        verificarResultadoYAsignaturaActivos(resultado);
        verificarPorcentaje(sumarActivas(resultado.getId(), evaluacion.getSemestre().getId()),
                evaluacion.getPorcentaje());

        evaluacion.setEstado(EstadoEvaluacion.ACTIVA);
        evaluacion.setInactivadaConRa(false);
        return aResponse(evaluacionRepository.save(evaluacion));
    }

    // ------------------------------------------------------------------ Consultas

    @Override
    @Transactional
    public EvaluacionesRaResumenDTO consultarPorResultadoAprendizaje(UUID resultadoAprendizajeId, UUID semestreId,
                                                                    UsuarioPrincipal principal) {
        ResultadoAprendizaje resultado = buscarResultado(resultadoAprendizajeId);
        verificarDueno(principal, resultado.getAsignatura());

        Semestre semestre = semestreId != null
                ? semestreRepository.findById(semestreId).orElseThrow(() -> new SemestreNoEncontradoException(semestreId))
                : obtenerSemestreActivo().orElseThrow(EvaluacionReglaNegocioException::sinSemestreActivo);

        List<EvaluacionResponseDTO> evaluaciones = evaluacionRepository
                .findByResultadoAprendizaje_IdAndSemestre_IdOrderByFechaTentativaAscNombreAsc(
                        resultadoAprendizajeId, semestre.getId())
                .stream()
                .map(this::aResponse)
                .toList();
        BigDecimal asignado = sumarActivas(resultadoAprendizajeId, semestre.getId());

        return new EvaluacionesRaResumenDTO(resultadoAprendizajeId, semestre.getId(), semestre.getCodigo(),
                asignado, disponible(asignado), evaluaciones);
    }

    @Override
    @Transactional
    public EvaluacionResponseDTO consultarPorId(UUID id, UsuarioPrincipal principal) {
        Evaluacion evaluacion = buscarEvaluacion(id);
        verificarDueno(principal, evaluacion.getResultadoAprendizaje().getAsignatura());
        return aResponse(evaluacion);
    }

    @Override
    @Transactional
    public BigDecimal consultarPorcentajeAsignado(UUID resultadoAprendizajeId, UUID semestreId) {
        return sumarActivas(resultadoAprendizajeId, semestreId);
    }

    // ------------------------------------------------------------------ Cascadas (las dispara el sistema)

    @Override
    @Transactional
    public void inactivarPorResultadoAprendizaje(UUID resultadoAprendizajeId) {
        evaluacionRepository.inactivarActivasPorResultado(resultadoAprendizajeId,
                EstadoEvaluacion.INACTIVA, EstadoEvaluacion.ACTIVA);
    }

    @Override
    @Transactional
    public void restaurarPorResultadoAprendizaje(UUID resultadoAprendizajeId) {
        evaluacionRepository.restaurarInactivadasConResultado(resultadoAprendizajeId, EstadoEvaluacion.ACTIVA);
    }

    @Override
    @Transactional
    public void inactivarPorAsignatura(UUID asignaturaId) {
        evaluacionRepository.inactivarActivasPorAsignatura(asignaturaId,
                EstadoEvaluacion.INACTIVA, EstadoEvaluacion.ACTIVA);
    }

    @Override
    @Transactional
    public void restaurarPorAsignatura(UUID asignaturaId) {
        evaluacionRepository.restaurarInactivadasPorAsignatura(asignaturaId,
                EstadoEvaluacion.ACTIVA, EstadoRegistro.ACTIVO);
    }

    // ------------------------------------------------------------------ Auxiliares

    private Evaluacion buscarEvaluacion(UUID id) {
        return evaluacionRepository.findById(id)
                .orElseThrow(() -> new EvaluacionNoEncontradaException(id));
    }

    private ResultadoAprendizaje buscarResultado(UUID id) {
        return resultadoAprendizajeRepository.findById(id)
                .orElseThrow(() -> new ResultadoAprendizajeNoEncontradoException(id));
    }

    /**
     * La identidad es obligatoria. El PROFESOR necesita asignación docente ACTIVA en la asignatura;
     * cualquier otro rol no gestiona evaluaciones (matriz RBAC: Propio solo para el profesor).
     */
    private void verificarDueno(UsuarioPrincipal principal, Asignatura asignatura) {
        if (principal == null || principal.rol() != RolUsuario.PROFESOR) {
            throw new PermisoInsuficienteException();
        }
        boolean asignada = asignacionDocenteRepository
                .findByProfesor_IdAndEstado(principal.id(), EstadoRegistro.ACTIVO)
                .stream()
                .anyMatch(asignacion -> asignacion.getAsignatura().getId().equals(asignatura.getId()));
        if (!asignada) {
            throw new PermisoInsuficienteException();
        }
    }

    private void verificarResultadoYAsignaturaActivos(ResultadoAprendizaje resultado) {
        if (resultado.getEstado() != EstadoRegistro.ACTIVO) {
            throw EvaluacionReglaNegocioException.resultadoAprendizajeInactivo();
        }
        if (resultado.getAsignatura().getEstado() != EstadoAsignatura.ACTIVA) {
            throw EvaluacionReglaNegocioException.asignaturaNoActiva();
        }
    }

    /**
     * El semestre cuyas fechas contienen hoy. Se calcula por fechas y no por la columna estado, que solo
     * se recalcula al crear o extender el semestre. Los semestres no se solapan, así que hay a lo sumo uno.
     */
    private Optional<Semestre> obtenerSemestreActivo() {
        LocalDate hoy = LocalDate.now(reloj);
        return semestreRepository.findAllByOrderByFechaInicioDesc().stream()
                .filter(s -> !hoy.isBefore(s.getFechaInicio()) && !hoy.isAfter(s.getFechaFin()))
                .findFirst();
    }

    /** Las evaluaciones de semestres que no son el activo solo se consultan. */
    private void verificarSemestreActivo(Evaluacion evaluacion) {
        boolean esDelActivo = obtenerSemestreActivo()
                .map(activo -> Objects.equals(activo.getId(), evaluacion.getSemestre().getId()))
                .orElse(false);
        if (!esDelActivo) {
            throw EvaluacionReglaNegocioException.semestreNoActivo();
        }
    }

    private void verificarFechaEnSemestre(LocalDate fecha, Semestre semestre) {
        if (fecha.isBefore(semestre.getFechaInicio()) || fecha.isAfter(semestre.getFechaFin())) {
            throw EvaluacionReglaNegocioException.fechaFueraDelSemestre(semestre.getCodigo(),
                    semestre.getFechaInicio(), semestre.getFechaFin());
        }
    }

    /** RF-11: lo ya asignado más el nuevo porcentaje no puede superar 100. */
    private void verificarPorcentaje(BigDecimal asignado, BigDecimal nuevo) {
        if (asignado.add(nuevo).compareTo(CIEN) > 0) {
            throw EvaluacionReglaNegocioException.porcentajeExcedido(disponible(asignado));
        }
    }

    private BigDecimal sumarActivas(UUID resultadoAprendizajeId, UUID semestreId) {
        return evaluacionRepository.sumarPorcentajes(resultadoAprendizajeId, semestreId, EstadoEvaluacion.ACTIVA);
    }

    private static BigDecimal disponible(BigDecimal asignado) {
        return CIEN.subtract(asignado).max(BigDecimal.ZERO);
    }

    private EvaluacionResponseDTO aResponse(Evaluacion evaluacion) {
        return evaluacionMapper.toResponseDTO(evaluacion,
                consultaCalificaciones.tieneCalificaciones(evaluacion.getId()));
    }
}
