package co.edu.uco.sigra.mejoras.service.impl;

import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.exception.ConclusionNoEncontradaException;
import co.edu.uco.sigra.estadisticas.repository.ConclusionRepository;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionCrearRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionResponseDTO;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraFiltroDTO;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraResponseDTO;
import co.edu.uco.sigra.mejoras.dto.ConvertirEvaluacionRequestDTO;
import co.edu.uco.sigra.mejoras.dto.RegistrarAccionMejoraRequestDTO;
import co.edu.uco.sigra.mejoras.entity.AccionMejora;
import co.edu.uco.sigra.mejoras.exception.AccionMejoraNoEncontradaException;
import co.edu.uco.sigra.mejoras.exception.AccionNoConvertibleException;
import co.edu.uco.sigra.mejoras.exception.AccionYaConvertidaException;
import co.edu.uco.sigra.mejoras.exception.ConclusionNoVigenteException;
import co.edu.uco.sigra.mejoras.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.mejoras.mapper.AccionMejoraMapper;
import co.edu.uco.sigra.mejoras.repository.AccionMejoraRepository;
import co.edu.uco.sigra.mejoras.service.AccionMejoraService;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RF-17: registro de acciones de mejora sobre conclusiones VIGENTES. Mismo patrón de alcance por
 * rol que {@code ConclusionServiceImpl} (temporal hasta RF-05): sin identidad se ve todo; con un
 * PROFESOR, solo lo de sus asignaturas.
 * <p>
 * RF-18: la conversión crea la evaluación con {@link EvaluacionService#crear}, así que aplica todas las
 * reglas de RF-10/RF-11 (semestre activo, fecha dentro del semestre, RA y asignatura activos, nombre
 * único, suma de porcentajes activos hasta 100%, dueño de la asignatura).
 */
@Service
@RequiredArgsConstructor
public class AccionMejoraServiceImpl implements AccionMejoraService {

    private final AccionMejoraRepository accionMejoraRepository;
    private final ConclusionRepository conclusionRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final AccionMejoraMapper accionMejoraMapper;
    private final EvaluacionService evaluacionService;
    private final Validator validator;

    @Override
    @Transactional
    public AccionMejoraResponseDTO registrarAccionMejora(UUID conclusionId, RegistrarAccionMejoraRequestDTO datos,
                                                           IdentidadActual identidad) {
        Conclusion conclusion = buscarConclusion(conclusionId);

        Set<UUID> alcance = alcanceDe(identidad);
        if (alcance != null && !alcance.contains(conclusion.getResultadoAprendizaje().getAsignatura().getId())) {
            throw new PermisoInsuficienteException();
        }

        if (conclusion.getEstado() != EstadoConclusion.VIGENTE) {
            throw new ConclusionNoVigenteException(conclusionId);
        }

        conclusion.marcarAtendida();
        conclusionRepository.save(conclusion);

        ResultadoAprendizaje resultadoAprendizaje = conclusion.getResultadoAprendizaje();
        AccionMejora accionMejora = new AccionMejora(datos.nombre(), datos.descripcion(), resultadoAprendizaje,
                conclusion);
        AccionMejora guardada = accionMejoraRepository.save(accionMejora);
        return accionMejoraMapper.toResponseDTO(guardada);
    }

    @Override
    @Transactional
    public List<AccionMejoraResponseDTO> consultarAcciones(AccionMejoraFiltroDTO filtro) {
        return consultarAcciones(filtro, null);
    }

    @Override
    @Transactional
    public List<AccionMejoraResponseDTO> consultarAcciones(AccionMejoraFiltroDTO filtro, IdentidadActual identidad) {
        Set<UUID> alcance = alcanceDe(identidad);

        // Mismo enfoque de filtrado en memoria que ConclusionServiceImpl.consultarConclusiones.
        return accionMejoraRepository.findAll().stream()
                .filter(a -> alcance == null || alcance.contains(a.getResultadoAprendizaje().getAsignatura().getId()))
                .filter(a -> filtro.resultadoAprendizajeId() == null
                        || filtro.resultadoAprendizajeId().equals(a.getResultadoAprendizaje().getId()))
                .filter(a -> filtro.asignaturaId() == null
                        || filtro.asignaturaId().equals(a.getResultadoAprendizaje().getAsignatura().getId()))
                .filter(a -> filtro.semestreId() == null
                        || filtro.semestreId().equals(a.getConclusionOrigen().getSemestre().getId()))
                .filter(a -> filtro.convertida() == null || filtro.convertida() == a.estaConvertida())
                .sorted(Comparator.comparing(AccionMejora::getFechaPropuesta).reversed())
                .map(accionMejoraMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public AccionMejoraResponseDTO consultarPorId(UUID id) {
        return accionMejoraMapper.toResponseDTO(buscarAccionMejora(id));
    }

    @Override
    @Transactional
    public AccionMejoraResponseDTO convertirEnEvaluacion(UUID accionId, ConvertirEvaluacionRequestDTO datos,
                                                          IdentidadActual identidad) {
        AccionMejora accionMejora = buscarAccionMejora(accionId);

        Set<UUID> alcance = alcanceDe(identidad);
        if (alcance != null && !alcance.contains(accionMejora.getResultadoAprendizaje().getAsignatura().getId())) {
            throw new PermisoInsuficienteException();
        }

        if (accionMejora.estaConvertida()) {
            throw new AccionYaConvertidaException(accionId);
        }

        // Hereda nombre y descripción de la acción; la fecha tentativa y el porcentaje los indica el Profesor.
        EvaluacionCrearRequestDTO evaluacionNueva = new EvaluacionCrearRequestDTO(accionMejora.getNombre(),
                datos.fechaTentativa(), datos.porcentaje(), accionMejora.getDescripcion());
        verificarConvertible(evaluacionNueva);

        // Mismo camino que el POST de evaluaciones: si alguna regla falla, la excepción revierte toda la
        // transacción y la acción no queda marcada como convertida.
        EvaluacionResponseDTO evaluacion = evaluacionService.crear(
                accionMejora.getResultadoAprendizaje().getId(), evaluacionNueva, identidad);

        accionMejora.marcarConvertida(evaluacion.id());
        AccionMejora actualizada = accionMejoraRepository.save(accionMejora);
        return accionMejoraMapper.toResponseDTO(actualizada);
    }

    /**
     * El servicio de evaluaciones no repite las validaciones de formato del DTO (las aplica el controlador
     * con @Valid). Como aquí el nombre y la descripción vienen de la acción de mejora, se validan antes de
     * crear: si no cumplen las reglas de RF-11 (longitud o caracteres), la conversión se rechaza.
     */
    private void verificarConvertible(EvaluacionCrearRequestDTO evaluacionNueva) {
        String detalle = validator.validate(evaluacionNueva).stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        if (!detalle.isEmpty()) {
            throw new AccionNoConvertibleException(detalle);
        }
    }

    /**
     * Asignaturas visibles para la identidad: null significa «todas» (sin identidad, o ADMINISTRADOR).
     * El PROFESOR solo ve las acciones de mejora de sus asignaturas con asignación docente ACTIVA.
     */
    private Set<UUID> alcanceDe(IdentidadActual identidad) {
        if (identidad == null || identidad.rol() == RolUsuario.ADMINISTRADOR) {
            return null;
        }
        if (identidad.rol() == RolUsuario.PROFESOR) {
            return asignacionDocenteRepository.findByProfesor_IdAndEstado(identidad.id(), EstadoRegistro.ACTIVO)
                    .stream()
                    .map(asignacion -> asignacion.getAsignatura().getId())
                    .collect(Collectors.toSet());
        }
        throw new PermisoInsuficienteException();
    }

    private Conclusion buscarConclusion(UUID id) {
        return conclusionRepository.findById(id)
                .orElseThrow(() -> new ConclusionNoEncontradaException(id));
    }

    private AccionMejora buscarAccionMejora(UUID id) {
        return accionMejoraRepository.findById(id)
                .orElseThrow(() -> new AccionMejoraNoEncontradaException(id));
    }
}
