package co.edu.uco.sigra.reportes.service.impl;

import co.edu.uco.sigra.auditoria.config.AuditoriaConfig;
import co.edu.uco.sigra.auditoria.entity.AccionAuditoria;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.util.NombresPersona;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.reportes.dto.ActividadEstudianteDTO;
import co.edu.uco.sigra.reportes.dto.ActividadPropiaDTO;
import co.edu.uco.sigra.reportes.dto.ActividadRolDTO;
import co.edu.uco.sigra.reportes.dto.ConteoAccionDTO;
import co.edu.uco.sigra.reportes.dto.ResumenActividadEstudiantesDTO;
import co.edu.uco.sigra.reportes.dto.TrazabilidadUsoProfesorResponseDTO;
import co.edu.uco.sigra.reportes.dto.TrazabilidadUsoReporteResponseDTO;
import co.edu.uco.sigra.reportes.dto.TrazabilidadUsoResumenDTO;
import co.edu.uco.sigra.reportes.exception.PermisoReporteInsuficienteException;
import co.edu.uco.sigra.reportes.exception.RangoReporteInvalidoException;
import co.edu.uco.sigra.reportes.exception.ReporteRecursoNoEncontradoException;
import co.edu.uco.sigra.reportes.repository.EstudiantesProfesorReporteRepository;
import co.edu.uco.sigra.reportes.repository.EstudiantesProfesorReporteRepository.EstudianteAsignatura;
import co.edu.uco.sigra.reportes.repository.TrazabilidadUsoReporteRepository;
import co.edu.uco.sigra.reportes.repository.TrazabilidadUsoReporteRepository.ActividadUsuario;
import co.edu.uco.sigra.reportes.repository.TrazabilidadUsoReporteRepository.ConteoAccion;
import co.edu.uco.sigra.reportes.repository.TrazabilidadUsoReporteRepository.ConteoRolAccion;
import co.edu.uco.sigra.reportes.repository.TrazabilidadUsoReporteRepository.ResumenRol;
import co.edu.uco.sigra.reportes.service.TrazabilidadUsoReporteService;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * R7 – Trazabilidad de uso por rol. Lee {@code historial_auditoria} y solo devuelve agregados:
 * el administrador ve conteos por tipo de usuario (sin identificar personas) y el profesor ve su propia
 * actividad y la de los estudiantes matriculados en sus asignaturas.
 */
@Service
public class TrazabilidadUsoReporteServiceImpl implements TrazabilidadUsoReporteService {

    public static final String SIN_IDENTIFICAR = "SIN_IDENTIFICAR";
    static final int DIAS_POR_DEFECTO = 30;
    static final int DIAS_MAXIMOS = 366;
    static final String SIN_SEMESTRE = "No hay un semestre activo; no se listan estudiantes.";
    static final String SIN_ESTUDIANTES = "No tiene estudiantes matriculados en sus asignaturas para este semestre.";

    private final TrazabilidadUsoReporteRepository repository;
    private final EstudiantesProfesorReporteRepository estudiantesRepository;
    private final SemestreRepository semestreRepository;
    private final EvaluacionService evaluacionService;
    private final Clock reloj;

    public TrazabilidadUsoReporteServiceImpl(TrazabilidadUsoReporteRepository repository,
                                             EstudiantesProfesorReporteRepository estudiantesRepository,
                                             SemestreRepository semestreRepository,
                                             EvaluacionService evaluacionService,
                                             @Qualifier(AuditoriaConfig.RELOJ_AUDITORIA) Clock reloj) {
        this.repository = repository;
        this.estudiantesRepository = estudiantesRepository;
        this.semestreRepository = semestreRepository;
        this.evaluacionService = evaluacionService;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public TrazabilidadUsoReporteResponseDTO consultarGeneral(LocalDate desde, LocalDate hasta, UUID semestreId,
                                                              RolUsuario rol) {
        Semestre semestre = semestreId == null ? null : buscarSemestre(semestreId);
        Rango rango = resolverRango(desde, hasta, semestre);

        Map<String, Map<AccionAuditoria, Long>> conteos = new LinkedHashMap<>();
        for (ConteoRolAccion fila : repository.contarPorRolYAccion(rango.inicio(), rango.fin())) {
            conteos.computeIfAbsent(claveRol(fila.getRol()), k -> new EnumMap<>(AccionAuditoria.class))
                    .merge(fila.getAccion(), valor(fila.getTotal()), Long::sum);
        }
        Map<String, ResumenRol> resumenes = repository.resumirPorRol(rango.inicio(), rango.fin()).stream()
                .collect(Collectors.toMap(r -> claveRol(r.getRol()), Function.identity(), (a, b) -> a));

        List<String> claves = new ArrayList<>();
        if (rol != null) {
            claves.add(rol.name());
        } else {
            for (RolUsuario valor : RolUsuario.values()) {
                claves.add(valor.name());
            }
            if (conteos.containsKey(SIN_IDENTIFICAR)) {
                claves.add(SIN_IDENTIFICAR);
            }
        }

        List<ActividadRolDTO> porRol = claves.stream().map(clave -> {
            Map<AccionAuditoria, Long> acciones = conteos.getOrDefault(clave, Map.of());
            ResumenRol resumen = resumenes.get(clave);
            long usuarios = resumen == null || SIN_IDENTIFICAR.equals(clave) ? 0 : valor(resumen.getUsuarios());
            return new ActividadRolDTO(clave, sumar(acciones), usuarios,
                    resumen == null ? null : resumen.getUltimaActividad(), aLista(acciones));
        }).toList();

        long total = porRol.stream().mapToLong(ActividadRolDTO::totalEventos).sum();
        long usuarios = porRol.stream().mapToLong(ActividadRolDTO::usuariosDistintos).sum();
        long loginsExitosos = contarAccion(porRol, AccionAuditoria.LOGIN_EXITOSO);
        long loginsFallidos = contarAccion(porRol, AccionAuditoria.LOGIN_FALLIDO);

        return new TrazabilidadUsoReporteResponseDTO(rango.desde(), rango.hasta(),
                semestre == null ? null : semestre.getId(), semestre == null ? null : semestre.getCodigo(), rol,
                new TrazabilidadUsoResumenDTO(total, usuarios, loginsExitosos, loginsFallidos), porRol);
    }

    @Override
    @Transactional(readOnly = true)
    public TrazabilidadUsoProfesorResponseDTO consultarProfesor(LocalDate desde, LocalDate hasta, UUID semestreId,
                                                                UsuarioPrincipal principal) {
        if (principal == null || principal.rol() != RolUsuario.PROFESOR) {
            throw new PermisoReporteInsuficienteException();
        }
        Optional<Semestre> semestre = semestreId == null
                ? evaluacionService.consultarSemestreActivo()
                : Optional.of(buscarSemestre(semestreId));
        Rango rango = resolverRango(desde, hasta, semestre.orElse(null));

        ActividadPropiaDTO propia = actividadPropia(principal.id(), rango);

        if (semestre.isEmpty()) {
            return new TrazabilidadUsoProfesorResponseDTO(rango.desde(), rango.hasta(), null, null, propia,
                    new ResumenActividadEstudiantesDTO(0, 0, 0, 0), List.of(), SIN_SEMESTRE);
        }

        List<EstudianteAsignatura> filas = estudiantesRepository.listarEstudiantesDelProfesor(
                principal.id(), semestre.get().getId(), EstadoMatricula.ACTIVA, EstadoRegistro.ACTIVO);
        Map<UUID, List<EstudianteAsignatura>> porEstudiante = filas.stream()
                .collect(Collectors.groupingBy(EstudianteAsignatura::getEstudianteId, LinkedHashMap::new,
                        Collectors.toList()));

        Map<UUID, ActividadUsuario> actividad = porEstudiante.isEmpty() ? Map.of()
                : repository.resumirPorUsuarios(porEstudiante.keySet(), AccionAuditoria.LOGIN_EXITOSO,
                        rango.inicio(), rango.fin()).stream()
                .collect(Collectors.toMap(ActividadUsuario::getUsuarioId, Function.identity(), (a, b) -> a));

        List<ActividadEstudianteDTO> estudiantes = porEstudiante.values().stream().map(grupo -> {
            EstudianteAsignatura primera = grupo.getFirst();
            ActividadUsuario uso = actividad.get(primera.getEstudianteId());
            return new ActividadEstudianteDTO(
                    primera.getEstudianteId(),
                    NombresPersona.completo(primera.getPrimerNombre(), primera.getSegundoNombre(),
                            primera.getPrimerApellido(), primera.getSegundoApellido()),
                    primera.getCorreoInstitucional(),
                    grupo.stream().map(EstudianteAsignatura::getAsignaturaCodigo).distinct().toList(),
                    uso == null ? 0 : valor(uso.getIngresos()),
                    uso == null ? 0 : valor(uso.getTotal()),
                    uso == null ? null : uso.getUltimaActividad());
        }).toList();

        long conActividad = estudiantes.stream().filter(e -> e.totalEventos() > 0).count();
        long ingresos = estudiantes.stream().mapToLong(ActividadEstudianteDTO::ingresos).sum();
        var resumen = new ResumenActividadEstudiantesDTO(
                estudiantes.size(), conActividad, estudiantes.size() - conActividad, ingresos);

        return new TrazabilidadUsoProfesorResponseDTO(rango.desde(), rango.hasta(), semestre.get().getId(),
                semestre.get().getCodigo(), propia, resumen, estudiantes,
                estudiantes.isEmpty() ? SIN_ESTUDIANTES : null);
    }

    private ActividadPropiaDTO actividadPropia(UUID usuarioId, Rango rango) {
        List<ConteoAccion> filas = repository.contarPropiasPorAccion(usuarioId, rango.inicio(), rango.fin());
        Map<AccionAuditoria, Long> acciones = new EnumMap<>(AccionAuditoria.class);
        filas.forEach(f -> acciones.merge(f.getAccion(), valor(f.getTotal()), Long::sum));
        LocalDateTime ultima = filas.stream().map(ConteoAccion::getUltimaActividad).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        return new ActividadPropiaDTO(sumar(acciones), ultima, aLista(acciones));
    }

    /**
     * Con semestre, el rango base son sus fechas y {@code desde}/{@code hasta} solo lo acotan. Sin semestre,
     * {@code hasta} es hoy y {@code desde} los {@value #DIAS_POR_DEFECTO} días anteriores, si no se indican.
     */
    Rango resolverRango(LocalDate desde, LocalDate hasta, Semestre semestre) {
        LocalDate inicio;
        LocalDate fin;
        if (semestre != null) {
            inicio = desde == null || desde.isBefore(semestre.getFechaInicio()) ? semestre.getFechaInicio() : desde;
            fin = hasta == null || hasta.isAfter(semestre.getFechaFin()) ? semestre.getFechaFin() : hasta;
            if (desde != null && hasta != null && desde.isAfter(hasta)) {
                throw new RangoReporteInvalidoException("La fecha inicial no puede ser posterior a la fecha final.");
            }
            if (inicio.isAfter(fin)) {
                throw new RangoReporteInvalidoException("El rango de fechas queda fuera del semestre seleccionado.");
            }
        } else {
            fin = hasta == null ? LocalDate.now(reloj) : hasta;
            inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO - 1L) : desde;
            if (inicio.isAfter(fin)) {
                throw new RangoReporteInvalidoException("La fecha inicial no puede ser posterior a la fecha final.");
            }
        }
        if (ChronoUnit.DAYS.between(inicio, fin) + 1 > DIAS_MAXIMOS) {
            throw new RangoReporteInvalidoException(
                    "El rango de fechas no puede superar " + DIAS_MAXIMOS + " días.");
        }
        return new Rango(inicio, fin);
    }

    private Semestre buscarSemestre(UUID semestreId) {
        return semestreRepository.findById(semestreId)
                .orElseThrow(() -> new ReporteRecursoNoEncontradoException("El semestre indicado no existe."));
    }

    private static String claveRol(RolUsuario rol) {
        return rol == null ? SIN_IDENTIFICAR : rol.name();
    }

    private static long valor(Long numero) {
        return numero == null ? 0 : numero;
    }

    private static long sumar(Map<AccionAuditoria, Long> acciones) {
        return acciones.values().stream().mapToLong(Long::longValue).sum();
    }

    private static List<ConteoAccionDTO> aLista(Map<AccionAuditoria, Long> acciones) {
        return acciones.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new ConteoAccionDTO(e.getKey(), e.getValue()))
                .toList();
    }

    private static long contarAccion(List<ActividadRolDTO> porRol, AccionAuditoria accion) {
        return porRol.stream().flatMap(r -> r.acciones().stream())
                .filter(a -> a.accion() == accion).mapToLong(ConteoAccionDTO::total).sum();
    }

    private static final LocalTime FIN_DEL_DIA = LocalTime.of(23, 59, 59, 999_999_000);

    record Rango(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio() {
            return desde.atStartOfDay();
        }

        LocalDateTime fin() {
            // PostgreSQL guarda microsegundos: con LocalTime.MAX el valor se redondearía al día siguiente.
            return hasta.atTime(FIN_DEL_DIA);
        }
    }
}
