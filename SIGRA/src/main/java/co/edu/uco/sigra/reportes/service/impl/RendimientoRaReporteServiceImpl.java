package co.edu.uco.sigra.reportes.service.impl;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.dto.EstadisticasResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.service.CatalogoNivelesLogro;
import co.edu.uco.sigra.estadisticas.service.NivelLogroService;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.repository.EvaluacionRepository;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.reportes.dto.AsignaturaRendimientoReporteDTO;
import co.edu.uco.sigra.reportes.dto.RaRendimientoReporteDTO;
import co.edu.uco.sigra.reportes.dto.RendimientoRaReporteResponseDTO;
import co.edu.uco.sigra.reportes.exception.PermisoReporteInsuficienteException;
import co.edu.uco.sigra.reportes.exception.ReporteRecursoNoEncontradoException;
import co.edu.uco.sigra.reportes.service.RendimientoRaReporteService;
import co.edu.uco.sigra.resultadosaprendizaje.dto.EstadoRendimiento;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * No recalcula nada: lee las estadísticas ya calculadas por el módulo de estadísticas (mismo nivel,
 * promedio y distribución) y solo las agrupa por asignatura. El nivel del RA se clasifica con
 * {@link CatalogoNivelesLogro}, igual que el rendimiento por asignatura.
 */
@Service
@RequiredArgsConstructor
public class RendimientoRaReporteServiceImpl implements RendimientoRaReporteService {

    static final String SIN_DATOS_DISPONIBLES = "Sin datos disponibles";
    static final String SIN_EVALUAR = "Sin evaluar";
    static final String SIN_DATOS_SUFICIENTES = "Sin datos suficientes para calcular";

    private final AsignaturaRepository asignaturaRepository;
    private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    private final ProgramaAcademicoRepository programaRepository;
    private final SemestreRepository semestreRepository;
    private final EvaluacionService evaluacionService;
    private final EvaluacionRepository evaluacionRepository;
    private final NivelLogroService nivelLogroService;

    @Override
    @Transactional(readOnly = true)
    public RendimientoRaReporteResponseDTO consultar(UUID semestreId, UUID programaId, UUID asignaturaId,
                                                     UsuarioPrincipal principal) {
        verificarRol(principal);
        Optional<Semestre> semestre = resolverSemestre(semestreId);
        if (programaId != null && !programaRepository.existsById(programaId)) {
            throw new ReporteRecursoNoEncontradoException("El programa académico indicado no existe.");
        }
        if (asignaturaId != null && !asignaturaRepository.existsById(asignaturaId)) {
            throw new ReporteRecursoNoEncontradoException("La asignatura indicada no existe.");
        }
        if (semestre.isEmpty()) {
            return new RendimientoRaReporteResponseDTO(null, null, List.of(), SIN_DATOS_DISPONIBLES);
        }

        List<Asignatura> candidatas = asignaturasVisibles(principal);
        if (asignaturaId != null) {
            if (candidatas.stream().noneMatch(a -> a.getId().equals(asignaturaId))) {
                throw new PermisoReporteInsuficienteException();
            }
            candidatas = candidatas.stream().filter(a -> a.getId().equals(asignaturaId)).toList();
        }
        if (programaId != null) {
            candidatas = candidatas.stream().filter(a -> a.getPrograma().getId().equals(programaId)).toList();
        }

        UUID idSemestre = semestre.get().getId();
        List<AsignaturaRendimientoReporteDTO> asignaturas = candidatas.stream()
                .sorted(Comparator.comparing(Asignatura::getCodigo))
                .map(a -> construirAsignatura(a, idSemestre, principal))
                .filter(a -> !a.resultados().isEmpty())
                .toList();
        return new RendimientoRaReporteResponseDTO(idSemestre, semestre.get().getCodigo(), asignaturas,
                asignaturas.isEmpty() ? SIN_DATOS_DISPONIBLES : null);
    }

    private void verificarRol(UsuarioPrincipal principal) {
        if (principal == null
                || (principal.rol() != RolUsuario.ADMINISTRADOR && principal.rol() != RolUsuario.PROFESOR)) {
            throw new PermisoReporteInsuficienteException();
        }
    }

    private Optional<Semestre> resolverSemestre(UUID semestreId) {
        if (semestreId == null) {
            return evaluacionService.consultarSemestreActivo();
        }
        return Optional.of(semestreRepository.findById(semestreId)
                .orElseThrow(() -> new ReporteRecursoNoEncontradoException("El semestre indicado no existe.")));
    }

    private List<Asignatura> asignaturasVisibles(UsuarioPrincipal principal) {
        if (principal.rol() == RolUsuario.ADMINISTRADOR) {
            return asignaturaRepository.findAll();
        }
        return asignaturaRepository.findAsignadasAProfesor(principal.id(), EstadoRegistro.ACTIVO);
    }

    private AsignaturaRendimientoReporteDTO construirAsignatura(Asignatura asignatura, UUID semestreId,
                                                                UsuarioPrincipal principal) {
        List<RaRendimientoReporteDTO> resultados = resultadoAprendizajeRepository
                .findByAsignatura_IdAndEstadoOrderByCodigoAsc(asignatura.getId(), EstadoRegistro.ACTIVO).stream()
                .map(ra -> construirRa(ra, semestreId, principal))
                .toList();
        return new AsignaturaRendimientoReporteDTO(asignatura.getId(), asignatura.getCodigo(), asignatura.getNombre(),
                asignatura.getPrograma().getId(), asignatura.getPrograma().getNombre(), resultados);
    }

    private RaRendimientoReporteDTO construirRa(ResultadoAprendizaje ra, UUID semestreId, UsuarioPrincipal principal) {
        EstadisticasResultadoAprendizajeResponseDTO estadisticas =
                nivelLogroService.consultarEstadisticas(ra.getId(), semestreId, principal);
        long calificaciones = evaluacionRepository
                .contarCalificacionesPorEvaluacion(ra.getId(), semestreId, EstadoEvaluacion.ACTIVA, EstadoMatricula.ACTIVA)
                .stream().mapToLong(fila -> (Long) fila[1]).sum();

        EstadoRendimiento estado;
        NivelLogro nivel = null;
        String mensaje = null;
        if (calificaciones == 0) {
            estado = EstadoRendimiento.SIN_EVALUAR;
            mensaje = SIN_EVALUAR;
        } else if (estadisticas.promedioNota() == null) {
            estado = EstadoRendimiento.DATOS_INSUFICIENTES;
            mensaje = SIN_DATOS_SUFICIENTES;
        } else {
            estado = EstadoRendimiento.CON_NIVEL;
            nivel = CatalogoNivelesLogro.clasificar(estadisticas.promedioNota());
        }
        return new RaRendimientoReporteDTO(ra.getId(), ra.getCodigo(), ra.getDescripcion(), calificaciones, estado,
                nivel, estadisticas.promedioNota(), estadisticas.evaluados(), estadisticas.sinDatosSuficientes(),
                estadisticas.sinEvaluar(), estadisticas.distribucion(), mensaje);
    }
}
