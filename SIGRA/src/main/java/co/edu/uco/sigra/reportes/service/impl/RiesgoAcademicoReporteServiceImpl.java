package co.edu.uco.sigra.reportes.service.impl;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.reportes.dto.EstudianteRiesgoDTO;
import co.edu.uco.sigra.reportes.dto.RaNoAlcanzadoDTO;
import co.edu.uco.sigra.reportes.dto.RiesgoAcademicoReporteResponseDTO;
import co.edu.uco.sigra.reportes.dto.SeveridadRiesgo;
import co.edu.uco.sigra.reportes.exception.PermisoReporteInsuficienteException;
import co.edu.uco.sigra.reportes.exception.ReporteRecursoNoEncontradoException;
import co.edu.uco.sigra.reportes.service.RiesgoAcademicoReporteService;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * R6: lista estudiantes con nivel BAJO o MEDIO (mismo criterio de «insuficiente» que RF-16),
 * agrupados por estudiante×asignatura. No recalcula niveles; lee {@code nivel_logro_estudiante}.
 */
@Service
@RequiredArgsConstructor
public class RiesgoAcademicoReporteServiceImpl implements RiesgoAcademicoReporteService {

    static final String SIN_DATOS_DISPONIBLES = "Sin datos disponibles";
    static final List<NivelLogro> NIVELES_INSUFICIENTES = List.of(NivelLogro.BAJO, NivelLogro.MEDIO);

    private final AsignaturaRepository asignaturaRepository;
    private final ProgramaAcademicoRepository programaRepository;
    private final SemestreRepository semestreRepository;
    private final EvaluacionService evaluacionService;
    private final NivelLogroEstudianteRepository nivelLogroEstudianteRepository;

    @Override
    @Transactional(readOnly = true)
    public RiesgoAcademicoReporteResponseDTO consultar(UUID semestreId, UUID programaId, UUID facultadId,
                                                       UUID asignaturaId, Integer minRaNoAlcanzados,
                                                       UsuarioPrincipal principal) {
        verificarRol(principal);
        int umbral = minRaNoAlcanzados == null || minRaNoAlcanzados < 1 ? 1 : minRaNoAlcanzados;

        Optional<Semestre> semestre = resolverSemestre(semestreId);
        if (programaId != null && !programaRepository.existsById(programaId)) {
            throw new ReporteRecursoNoEncontradoException("El programa académico indicado no existe.");
        }
        if (asignaturaId != null && !asignaturaRepository.existsById(asignaturaId)) {
            throw new ReporteRecursoNoEncontradoException("La asignatura indicada no existe.");
        }
        if (semestre.isEmpty()) {
            return new RiesgoAcademicoReporteResponseDTO(null, null, 0, List.of(), SIN_DATOS_DISPONIBLES);
        }

        // La facultad se resuelve en la consulta (asignatura → programa → facultad), dentro del alcance del rol.
        List<Asignatura> candidatas = asignaturasVisibles(principal, facultadId);
        if (asignaturaId != null) {
            // El permiso se comprueba sobre el alcance completo del rol: una asignatura propia pero de otra
            // facultad no es un 403, solo queda fuera del resultado por el filtro de facultad.
            List<Asignatura> alcance = facultadId == null ? candidatas : asignaturasVisibles(principal, null);
            if (alcance.stream().noneMatch(a -> a.getId().equals(asignaturaId))) {
                throw new PermisoReporteInsuficienteException();
            }
            candidatas = candidatas.stream().filter(a -> a.getId().equals(asignaturaId)).toList();
        }
        if (programaId != null) {
            candidatas = candidatas.stream().filter(a -> a.getPrograma().getId().equals(programaId)).toList();
        }
        if (candidatas.isEmpty()) {
            return vacio(semestre.get());
        }

        List<UUID> asignaturaIds = candidatas.stream().map(Asignatura::getId).toList();
        List<NivelLogroEstudiante> filas = nivelLogroEstudianteRepository
                .buscarInsuficientesPorSemestreYAsignaturas(
                        semestre.get().getId(), NIVELES_INSUFICIENTES, EstadoRegistro.ACTIVO, asignaturaIds);

        List<EstudianteRiesgoDTO> estudiantes = agrupar(filas).stream()
                .filter(e -> e.cantidadRaNoAlcanzados() >= umbral)
                .sorted(Comparator
                        .comparingInt(EstudianteRiesgoDTO::cantidadRaNoAlcanzados).reversed()
                        .thenComparing(EstudianteRiesgoDTO::nombreCompleto, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(EstudianteRiesgoDTO::asignaturaCodigo))
                .toList();

        return new RiesgoAcademicoReporteResponseDTO(
                semestre.get().getId(),
                semestre.get().getCodigo(),
                estudiantes.size(),
                estudiantes,
                estudiantes.isEmpty() ? SIN_DATOS_DISPONIBLES : null);
    }

    private RiesgoAcademicoReporteResponseDTO vacio(Semestre semestre) {
        return new RiesgoAcademicoReporteResponseDTO(
                semestre.getId(), semestre.getCodigo(), 0, List.of(), SIN_DATOS_DISPONIBLES);
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

    private List<Asignatura> asignaturasVisibles(UsuarioPrincipal principal, UUID facultadId) {
        if (principal.rol() == RolUsuario.ADMINISTRADOR) {
            return facultadId == null
                    ? asignaturaRepository.findAllConProgramaYFacultad()
                    : asignaturaRepository.findAllDeFacultad(facultadId);
        }
        return facultadId == null
                ? asignaturaRepository.findAsignadasAProfesor(principal.id(), EstadoRegistro.ACTIVO)
                : asignaturaRepository.findAsignadasAProfesorEnFacultad(principal.id(), EstadoRegistro.ACTIVO, facultadId);
    }

    private List<EstudianteRiesgoDTO> agrupar(List<NivelLogroEstudiante> filas) {
        record Clave(UUID estudianteId, UUID asignaturaId) {
        }

        Map<Clave, List<NivelLogroEstudiante>> porClave = new LinkedHashMap<>();
        for (NivelLogroEstudiante fila : filas) {
            UUID estudianteId = fila.getEstudiante().getId();
            UUID asignaturaId = fila.getResultadoAprendizaje().getAsignatura().getId();
            porClave.computeIfAbsent(new Clave(estudianteId, asignaturaId), k -> new ArrayList<>()).add(fila);
        }

        List<EstudianteRiesgoDTO> resultado = new ArrayList<>();
        for (List<NivelLogroEstudiante> grupo : porClave.values()) {
            NivelLogroEstudiante primera = grupo.getFirst();
            Estudiante estudiante = primera.getEstudiante();
            Asignatura asignatura = primera.getResultadoAprendizaje().getAsignatura();

            List<RaNoAlcanzadoDTO> ras = grupo.stream()
                    .sorted(Comparator.comparing(n -> n.getResultadoAprendizaje().getCodigo()))
                    .map(this::aRaDto)
                    .toList();

            int cantidad = ras.size();
            resultado.add(new EstudianteRiesgoDTO(
                    estudiante.getId(),
                    estudiante.getNombreCompleto(),
                    estudiante.getPrimerNombre(),
                    estudiante.getSegundoNombre(),
                    estudiante.getPrimerApellido(),
                    estudiante.getSegundoApellido(),
                    estudiante.getCorreoInstitucional(),
                    estudiante.getNumeroDocumento(),
                    asignatura.getId(),
                    asignatura.getCodigo(),
                    asignatura.getNombre(),
                    asignatura.getPrograma().getId(),
                    asignatura.getPrograma().getNombre(),
                    asignatura.getPrograma().getFacultad() == null ? null : asignatura.getPrograma().getFacultad().getId(),
                    asignatura.getPrograma().getFacultad() == null ? null : asignatura.getPrograma().getFacultad().getNombre(),
                    cantidad,
                    severidadDe(cantidad),
                    ras));
        }
        return resultado;
    }

    private RaNoAlcanzadoDTO aRaDto(NivelLogroEstudiante fila) {
        ResultadoAprendizaje ra = fila.getResultadoAprendizaje();
        return new RaNoAlcanzadoDTO(
                ra.getId(), ra.getCodigo(), ra.getDescripcion(), fila.getNivelLogro(), fila.getPromedioPonderado());
    }

    static SeveridadRiesgo severidadDe(int cantidad) {
        if (cantidad >= 3) {
            return SeveridadRiesgo.ALTO_RIESGO;
        }
        if (cantidad == 2) {
            return SeveridadRiesgo.RIESGO;
        }
        return SeveridadRiesgo.ATENCION;
    }
}
