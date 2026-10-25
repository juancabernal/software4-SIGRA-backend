package co.edu.uco.sigra.asignaturas.service.impl;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaUpdateDTO;
import co.edu.uco.sigra.asignaturas.dto.MiAsignaturaDTO;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.exception.CodigoAsignaturaDuplicadoException;
import co.edu.uco.sigra.asignaturas.exception.ConfirmacionRequeridaException;
import co.edu.uco.sigra.asignaturas.exception.FiltroInvalidoException;
import co.edu.uco.sigra.asignaturas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.asignaturas.exception.ProgramaInactivoException;
import co.edu.uco.sigra.asignaturas.exception.ProgramaNoEncontradoException;
import co.edu.uco.sigra.asignaturas.mapper.AsignaturaMapper;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.asignaturas.repository.ConteoPorAsignatura;
import co.edu.uco.sigra.asignaturas.repository.ConteoRaPorAsignatura;
import co.edu.uco.sigra.asignaturas.repository.EvaluacionAbiertaRepository;
import co.edu.uco.sigra.asignaturas.repository.ProfesorDeAsignatura;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.resultadosaprendizaje.service.ResultadoAprendizajeService;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AsignaturaServiceImpl implements AsignaturaService {

    private final AsignaturaRepository asignaturaRepository;
    private final ProgramaAcademicoRepository programaAcademicoRepository;
    private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final AsignaturaMapper asignaturaMapper;
    private final ResultadoAprendizajeService resultadoAprendizajeService;
    private final EvaluacionService evaluacionService;
    private final EvaluacionAbiertaRepository evaluacionAbiertaRepository;
    private final CurrentUser currentUser;

    @Autowired
    public AsignaturaServiceImpl(AsignaturaRepository asignaturaRepository,
                                 ProgramaAcademicoRepository programaAcademicoRepository,
                                 ResultadoAprendizajeRepository resultadoAprendizajeRepository,
                                 AsignacionDocenteRepository asignacionDocenteRepository,
                                 AsignaturaMapper asignaturaMapper,
                                 ResultadoAprendizajeService resultadoAprendizajeService,
                                 EvaluacionService evaluacionService,
                                 EvaluacionAbiertaRepository evaluacionAbiertaRepository,
                                 CurrentUser currentUser) {
        this.asignaturaRepository = asignaturaRepository;
        this.programaAcademicoRepository = programaAcademicoRepository;
        this.resultadoAprendizajeRepository = resultadoAprendizajeRepository;
        this.asignacionDocenteRepository = asignacionDocenteRepository;
        this.asignaturaMapper = asignaturaMapper;
        this.resultadoAprendizajeService = resultadoAprendizajeService;
        this.evaluacionService = evaluacionService;
        this.evaluacionAbiertaRepository = evaluacionAbiertaRepository;
        this.currentUser = currentUser;
    }

    /** Constructor reducido conservado para pruebas unitarias; la identidad sigue resolviéndose desde Spring Security. */
    public AsignaturaServiceImpl(AsignaturaRepository asignaturaRepository,
                                 ProgramaAcademicoRepository programaAcademicoRepository,
                                 ResultadoAprendizajeRepository resultadoAprendizajeRepository,
                                 AsignacionDocenteRepository asignacionDocenteRepository,
                                 AsignaturaMapper asignaturaMapper,
                                 ResultadoAprendizajeService resultadoAprendizajeService,
                                 EvaluacionService evaluacionService,
                                 EvaluacionAbiertaRepository evaluacionAbiertaRepository) {
        this(asignaturaRepository, programaAcademicoRepository, resultadoAprendizajeRepository,
                asignacionDocenteRepository, asignaturaMapper, resultadoAprendizajeService,
                evaluacionService, evaluacionAbiertaRepository, new CurrentUser());
    }

    @Override
    @Transactional
    public AsignaturaResponseDTO registrarAsignatura(AsignaturaRequestDTO dto) {
        // El DTO ya llega normalizado (código en mayúsculas, nombre sin espacios sobrantes).
        String codigo = dto.codigo();
        if (asignaturaRepository.existsByCodigo(codigo)) {
            throw new CodigoAsignaturaDuplicadoException(codigo);
        }

        ProgramaAcademico programa = programaAcademicoRepository.findById(dto.programaId())
                .orElseThrow(() -> new ProgramaNoEncontradoException(dto.programaId()));
        if (programa.getEstado() != EstadoRegistro.ACTIVO) {
            throw new ProgramaInactivoException(programa.getNombre());
        }

        Asignatura asignatura = new Asignatura(codigo, dto.nombre(), programa);
        return asignaturaMapper.toResponseDTO(asignaturaRepository.saveAndFlush(asignatura), 0);
    }

    @Override
    @Transactional
    public List<AsignaturaResponseDTO> consultar(AsignaturaFiltroDTO filtro) {
        return consultar(filtro, currentUser.required());
    }

    @Override
    @Transactional
    public List<AsignaturaResponseDTO> consultar(AsignaturaFiltroDTO filtro, UsuarioPrincipal identidad) {
        validarRangoRa(filtro.raMin(), filtro.raMax());
        Set<UUID> alcance = alcanceDe(identidad);

        Map<UUID, Long> raActivosPorAsignatura = asignaturaRepository.contarRaPorAsignatura(EstadoRegistro.ACTIVO)
                .stream()
                .collect(Collectors.toMap(ConteoRaPorAsignatura::getAsignaturaId, ConteoRaPorAsignatura::getCantidad));
        String texto = filtro.texto() == null ? null : normalizar(filtro.texto());

        // El catálogo de asignaturas es pequeño, así que se filtra en memoria.
        return asignaturaRepository.findAll().stream()
                .filter(a -> alcance == null || alcance.contains(a.getId()))
                .filter(a -> filtro.programaId() == null || filtro.programaId().equals(a.getPrograma().getId()))
                .filter(a -> filtro.estado() == null || filtro.estado() == a.getEstado())
                .filter(a -> texto == null
                        || normalizar(a.getNombre()).contains(texto)
                        || normalizar(a.getCodigo()).contains(texto))
                .filter(a -> {
                    long cantidadRa = raActivosPorAsignatura.getOrDefault(a.getId(), 0L);
                    return (filtro.raMin() == null || cantidadRa >= filtro.raMin())
                            && (filtro.raMax() == null || cantidadRa <= filtro.raMax());
                })
                .sorted(Comparator.comparing(Asignatura::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(a -> asignaturaMapper.toResponseDTO(a, raActivosPorAsignatura.getOrDefault(a.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public AsignaturaResponseDTO obtenerAsignatura(UUID id) {
        return obtenerAsignatura(id, currentUser.required());
    }

    @Override
    @Transactional
    public AsignaturaResponseDTO obtenerAsignatura(UUID id, UsuarioPrincipal identidad) {
        Set<UUID> alcance = alcanceDe(identidad);
        if (alcance != null && !alcance.contains(id)) {
            throw new PermisoInsuficienteException();
        }
        return asignaturaMapper.toResponseDTO(buscar(id), contarRaActivos(id));
    }

    @Override
    @Transactional
    public AsignaturaResponseDTO modificarAsignatura(UUID id, AsignaturaUpdateDTO dto) {
        Asignatura asignatura = buscar(id);
        asignatura.renombrar(dto.nombre());
        return asignaturaMapper.toResponseDTO(asignaturaRepository.save(asignatura), contarRaActivos(id));
    }

    @Override
    @Transactional
    public AsignaturaResponseDTO activar(UUID id) {
        Asignatura asignatura = buscar(id);
        if (asignatura.getEstado() == EstadoAsignatura.INACTIVA) {
            // Reactivar: primero vuelven a ACTIVO solo los RA inactivados con la asignatura. Si luego el
            // rango no es válido, activar() lanza la excepción y la transacción revierte la restauración.
            // RF-10: el servicio de RA restaura también las evaluaciones de esos RA.
            resultadoAprendizajeService.restaurarPorAsignatura(id);
        }
        long cantidadRa = contarRaActivos(id);
        asignatura.activar(cantidadRa);
        return asignaturaMapper.toResponseDTO(asignaturaRepository.save(asignatura), cantidadRa);
    }

    @Override
    @Transactional
    public AsignaturaResponseDTO inactivar(UUID id, boolean confirmar) {
        Asignatura asignatura = buscar(id);
        if (!confirmar) {
            Optional<Semestre> semestreActivo = evaluacionService.consultarSemestreActivo();
            if (semestreActivo.isPresent()) {
                long cantidadEvaluacionesAbiertas = evaluacionAbiertaRepository
                        .countByResultadoAprendizaje_Asignatura_IdAndSemestre_IdAndEstado(
                                id, semestreActivo.get().getId(), EstadoEvaluacion.ACTIVA);
                if (cantidadEvaluacionesAbiertas > 0) {
                    throw ConfirmacionRequeridaException.porEvaluacionesAbiertas(id, cantidadEvaluacionesAbiertas);
                }
            }
        }
        asignatura.inactivar();
        Asignatura guardada = asignaturaRepository.save(asignatura);
        // Cascada: el SRS (RF-06c) exime del mínimo de RA a la asignatura que se inactiva, así que sus RA
        // activos pasan a INACTIVO en la misma transacción, marcados para restaurarse si se reactiva.
        // No se borra nada.
        // RF-10: el servicio de RA inactiva también las evaluaciones activas de esos RA.
        resultadoAprendizajeService.inactivarPorAsignatura(id);
        return asignaturaMapper.toResponseDTO(guardada, contarRaActivos(id));
    }

    /**
     * Asignaturas visibles para la identidad: el ADMINISTRADOR ve todas. El PROFESOR solo ve las
     * suyas con asignación docente ACTIVA; un ESTUDIANTE no consulta el catálogo.
     */
    private Set<UUID> alcanceDe(UsuarioPrincipal identidad) {
        if (identidad == null) {
            throw new PermisoInsuficienteException();
        }
        if (identidad.rol() == RolUsuario.ADMINISTRADOR) {
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

    private void validarRangoRa(Integer raMin, Integer raMax) {
        if ((raMin != null && raMin < 0) || (raMax != null && raMax < 0)) {
            throw new FiltroInvalidoException("La cantidad de RA no puede ser negativa.");
        }
        if (raMin != null && raMax != null && raMin > raMax) {
            throw new FiltroInvalidoException("La cantidad mínima de RA no puede ser mayor que la máxima.");
        }
    }

    private static String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public List<MiAsignaturaDTO> misAsignaturas(UsuarioPrincipal identidad) {
        if (identidad == null) {
            throw new PermisoInsuficienteException();
        }
        List<Asignatura> asignaturas = switch (identidad.rol()) {
            case PROFESOR -> asignaturaRepository.findAsignadasAProfesor(identidad.id(), EstadoRegistro.ACTIVO);
            case ESTUDIANTE -> asignaturaRepository.findMatriculadasPorEstudiante(identidad.id(), EstadoMatricula.ACTIVA);
            default -> throw new PermisoInsuficienteException();
        };
        if (asignaturas.isEmpty()) {
            return List.of();
        }

        // Tres consultas agrupadas para todas las asignaturas a la vez (sin N+1).
        Set<UUID> ids = asignaturas.stream().map(Asignatura::getId).collect(Collectors.toSet());
        Map<UUID, Long> ra = porAsignatura(asignaturaRepository.contarRaDe(ids, EstadoRegistro.ACTIVO));
        Map<UUID, Long> estudiantes = porAsignatura(asignaturaRepository.contarMatriculasDe(ids, EstadoMatricula.ACTIVA));
        Map<UUID, List<String>> profesores = asignaturaRepository.profesoresDe(ids, EstadoRegistro.ACTIVO).stream()
                .collect(Collectors.groupingBy(ProfesorDeAsignatura::getAsignaturaId,
                        Collectors.mapping(ProfesorDeAsignatura::getNombre, Collectors.toList())));

        return asignaturas.stream()
                .sorted(Comparator.comparing(Asignatura::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(a -> new MiAsignaturaDTO(a.getId(), a.getCodigo(), a.getNombre(), a.getPrograma().getId(),
                        a.getPrograma().getNombre(), a.getEstado(), ra.getOrDefault(a.getId(), 0L),
                        estudiantes.getOrDefault(a.getId(), 0L),
                        profesores.getOrDefault(a.getId(), List.of()).stream()
                                .sorted(String.CASE_INSENSITIVE_ORDER).toList()))
                .toList();
    }

    private static Map<UUID, Long> porAsignatura(List<ConteoPorAsignatura> conteos) {
        return conteos.stream()
                .collect(Collectors.toMap(ConteoPorAsignatura::getAsignaturaId, ConteoPorAsignatura::getCantidad));
    }

    private Asignatura buscar(UUID id) {
        return asignaturaRepository.findById(id)
                .orElseThrow(() -> new AsignaturaNoEncontradaException(id));
    }

    private long contarRaActivos(UUID id) {
        return resultadoAprendizajeRepository.countByAsignatura_IdAndEstado(id, EstadoRegistro.ACTIVO);
    }
}
