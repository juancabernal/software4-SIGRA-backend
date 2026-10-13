package co.edu.uco.sigra.mejoras.service.impl;

import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.exception.ConclusionNoEncontradaException;
import co.edu.uco.sigra.estadisticas.repository.ConclusionRepository;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraFiltroDTO;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraResponseDTO;
import co.edu.uco.sigra.mejoras.dto.ConvertirEvaluacionRequestDTO;
import co.edu.uco.sigra.mejoras.dto.RegistrarAccionMejoraRequestDTO;
import co.edu.uco.sigra.mejoras.entity.AccionMejora;
import co.edu.uco.sigra.mejoras.entity.RegistroEvaluacionPrueba;
import co.edu.uco.sigra.mejoras.exception.AccionMejoraNoEncontradaException;
import co.edu.uco.sigra.mejoras.exception.AccionYaConvertidaException;
import co.edu.uco.sigra.mejoras.exception.ConclusionNoVigenteException;
import co.edu.uco.sigra.mejoras.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.mejoras.exception.PorcentajeExcedeLimiteException;
import co.edu.uco.sigra.mejoras.mapper.AccionMejoraMapper;
import co.edu.uco.sigra.mejoras.repository.AccionMejoraRepository;
import co.edu.uco.sigra.mejoras.repository.RegistroEvaluacionPruebaRepository;
import co.edu.uco.sigra.mejoras.service.AccionMejoraService;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import jakarta.transaction.Transactional;
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
 */
@Service
@RequiredArgsConstructor
public class AccionMejoraServiceImpl implements AccionMejoraService {

    private final AccionMejoraRepository accionMejoraRepository;
    private final ConclusionRepository conclusionRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final AccionMejoraMapper accionMejoraMapper;
    private final RegistroEvaluacionPruebaRepository registroEvaluacionPruebaRepository;

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

        ResultadoAprendizaje resultadoAprendizaje = accionMejora.getResultadoAprendizaje();
        Semestre semestre = accionMejora.getConclusionOrigen().getSemestre();

        double sumaActual = registroEvaluacionPruebaRepository.sumarPorcentajes(resultadoAprendizaje.getId(),
                semestre.getId());
        if (sumaActual + datos.porcentaje() > 100) {
            throw new PorcentajeExcedeLimiteException(sumaActual, datos.porcentaje());
        }

        RegistroEvaluacionPrueba registroEvaluacion = new RegistroEvaluacionPrueba(resultadoAprendizaje, semestre,
                accionMejora.getDescripcion(), datos.porcentaje());
        RegistroEvaluacionPrueba guardado = registroEvaluacionPruebaRepository.save(registroEvaluacion);

        accionMejora.marcarConvertida(guardado.getId());
        AccionMejora actualizada = accionMejoraRepository.save(accionMejora);
        return accionMejoraMapper.toResponseDTO(actualizada);
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
