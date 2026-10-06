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
import co.edu.uco.sigra.asignaturas.exception.FiltroInvalidoException;
import co.edu.uco.sigra.asignaturas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.asignaturas.exception.ProgramaInactivoException;
import co.edu.uco.sigra.asignaturas.exception.ProgramaNoEncontradoException;
import co.edu.uco.sigra.asignaturas.mapper.AsignaturaMapper;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.asignaturas.repository.ConteoPorAsignatura;
import co.edu.uco.sigra.asignaturas.repository.ConteoRaPorAsignatura;
import co.edu.uco.sigra.asignaturas.repository.ProfesorDeAsignatura;
import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsignaturaServiceImpl implements AsignaturaService {

    private final AsignaturaRepository asignaturaRepository;
    private final ProgramaAcademicoRepository programaAcademicoRepository;
    private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final AsignaturaMapper asignaturaMapper;

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
        return consultar(filtro, null);
    }

    @Override
    @Transactional
    public List<AsignaturaResponseDTO> consultar(AsignaturaFiltroDTO filtro, IdentidadActual identidad) {
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
        return obtenerAsignatura(id, null);
    }

    @Override
    @Transactional
    public AsignaturaResponseDTO obtenerAsignatura(UUID id, IdentidadActual identidad) {
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
            resultadoAprendizajeRepository.restaurarInactivadosConAsignatura(id, EstadoRegistro.ACTIVO);
        }
        long cantidadRa = contarRaActivos(id);
        asignatura.activar(cantidadRa);
        return asignaturaMapper.toResponseDTO(asignaturaRepository.save(asignatura), cantidadRa);
    }

    @Override
    @Transactional
    public AsignaturaResponseDTO inactivar(UUID id) {
        Asignatura asignatura = buscar(id);
        asignatura.inactivar();
        Asignatura guardada = asignaturaRepository.save(asignatura);
        // Cascada: el SRS (RF-06c) exime del mínimo de RA a la asignatura que se inactiva, así que sus RA
        // activos pasan a INACTIVO en la misma transacción, marcados para restaurarse si se reactiva.
        // No se borra nada.
        resultadoAprendizajeRepository.inactivarActivosPorAsignatura(id, EstadoRegistro.INACTIVO, EstadoRegistro.ACTIVO);
        return asignaturaMapper.toResponseDTO(guardada, contarRaActivos(id));
    }

    /**
     * Asignaturas visibles para la identidad: null significa «todas» (sin identidad, o ADMINISTRADOR).
     * El PROFESOR solo ve las suyas con asignación docente ACTIVA; un ESTUDIANTE no consulta el catálogo.
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
    public List<MiAsignaturaDTO> misAsignaturas(IdentidadActual identidad) {
        List<Asignatura> asignaturas = switch (identidad.rol()) {
            case PROFESOR -> asignaturaRepository.findAsignadasAProfesor(identidad.id(), EstadoRegistro.ACTIVO);
            case ESTUDIANTE -> asignaturaRepository.findMatriculadasPorEstudiante(identidad.id(), EstadoRegistro.ACTIVO);
            default -> throw new PermisoInsuficienteException();
        };
        if (asignaturas.isEmpty()) {
            return List.of();
        }

        // Tres consultas agrupadas para todas las asignaturas a la vez (sin N+1).
        Set<UUID> ids = asignaturas.stream().map(Asignatura::getId).collect(Collectors.toSet());
        Map<UUID, Long> ra = porAsignatura(asignaturaRepository.contarRaDe(ids, EstadoRegistro.ACTIVO));
        Map<UUID, Long> estudiantes = porAsignatura(asignaturaRepository.contarMatriculasDe(ids, EstadoRegistro.ACTIVO));
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
