package co.edu.uco.sigra.estudiantes.service.impl;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estudiantes.config.MatriculaConfig;
import co.edu.uco.sigra.estudiantes.dto.EstudianteRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteResponseDTO;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.exception.ConfirmacionRequeridaException;
import co.edu.uco.sigra.estudiantes.exception.EstudianteNoEncontradoException;
import co.edu.uco.sigra.estudiantes.exception.MatriculaDuplicadaException;
import co.edu.uco.sigra.estudiantes.exception.MatriculaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.exception.MatriculaYaInactivaException;
import co.edu.uco.sigra.estudiantes.exception.ReferenciaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.exception.ReglaDeMatriculaException;
import co.edu.uco.sigra.estudiantes.repository.EstudianteRepository;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.estudiantes.service.CalificacionesDeMatricula;
import co.edu.uco.sigra.estudiantes.service.EstudianteService;
import co.edu.uco.sigra.estudiantes.service.MatriculaService;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MatriculaServiceImpl implements MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final EstudianteRepository estudianteRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final SemestreRepository semestreRepository;
    private final Clock reloj;
    private final CalificacionesDeMatricula calificacionesDeMatricula;
    private final EstudianteService estudianteService;
    private final AsignacionDocenteRepository asignacionDocenteRepository;
    private final CurrentUser currentUser;

    // Constructor explícito porque Lombok no copia @Qualifier al parámetro del reloj.
    @Autowired
    public MatriculaServiceImpl(MatriculaRepository matriculaRepository,
                                EstudianteRepository estudianteRepository,
                                AsignaturaRepository asignaturaRepository,
                                SemestreRepository semestreRepository,
                                @Qualifier(MatriculaConfig.RELOJ_MATRICULAS) Clock reloj,
                                CalificacionesDeMatricula calificacionesDeMatricula,
                                EstudianteService estudianteService,
                                AsignacionDocenteRepository asignacionDocenteRepository,
                                CurrentUser currentUser) {
        this.matriculaRepository = matriculaRepository;
        this.estudianteRepository = estudianteRepository;
        this.asignaturaRepository = asignaturaRepository;
        this.semestreRepository = semestreRepository;
        this.reloj = reloj;
        this.calificacionesDeMatricula = calificacionesDeMatricula;
        this.estudianteService = estudianteService;
        this.asignacionDocenteRepository = asignacionDocenteRepository;
        this.currentUser = currentUser;
    }

    /** Constructor reducido para las pruebas del flujo funcional de matrículas. */
    public MatriculaServiceImpl(MatriculaRepository matriculaRepository,
                                EstudianteRepository estudianteRepository,
                                AsignaturaRepository asignaturaRepository,
                                SemestreRepository semestreRepository,
                                Clock reloj,
                                CalificacionesDeMatricula calificacionesDeMatricula,
                                EstudianteService estudianteService) {
        this(matriculaRepository, estudianteRepository, asignaturaRepository, semestreRepository,
                reloj, calificacionesDeMatricula, estudianteService, null, new CurrentUser());
    }

    /** Constructor reducido conservado para pruebas que solo ejercitan autorización y consultas. */
    public MatriculaServiceImpl(MatriculaRepository matriculaRepository,
                                EstudianteRepository estudianteRepository,
                                AsignaturaRepository asignaturaRepository,
                                SemestreRepository semestreRepository) {
        this(matriculaRepository, estudianteRepository, asignaturaRepository, semestreRepository,
                Clock.system(MatriculaConfig.ZONA_COLOMBIA), new CalificacionesDeMatriculaPorDefecto(),
                null, null, new CurrentUser());
    }

    @Override
    @Transactional
    public ResultadoMatricula matricular(UUID estudianteId, UUID asignaturaId, UUID semestreId) {
        return matricularInterno(estudianteId, asignaturaId, semestreId, currentUser.required());
    }

    @Override
    @Transactional
    public ResultadoMatricula matricular(UUID estudianteId, UUID asignaturaId, UUID semestreId,
                                         UsuarioPrincipal principal) {
        return matricularInterno(estudianteId, asignaturaId, semestreId, principal);
    }

    private ResultadoMatricula matricularInterno(UUID estudianteId, UUID asignaturaId, UUID semestreId,
                                                 UsuarioPrincipal principal) {
        asegurarAlcance(asignaturaId, principal);
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> EstudianteNoEncontradoException.porId(estudianteId));

        Asignatura asignatura = asignaturaRepository.findById(asignaturaId)
                .orElseThrow(() -> ReferenciaNoEncontradaException.asignatura(asignaturaId));

        Semestre semestre = semestreRepository.findById(semestreId)
                .orElseThrow(() -> ReferenciaNoEncontradaException.semestre(semestreId));

        if (estudiante.getEstado() != EstadoRegistro.ACTIVO) {
            throw ReglaDeMatriculaException.estudianteInactivo(estudianteId);
        }
        if (asignatura.getEstado() != EstadoAsignatura.ACTIVA) {
            throw ReglaDeMatriculaException.asignaturaNoActiva(asignaturaId);
        }

        LocalDate hoy = LocalDate.now(reloj);
        if (hoy.isBefore(semestre.getFechaInicio()) || hoy.isAfter(semestre.getFechaFin())) {
            throw ReglaDeMatriculaException.semestreNoVigente(semestreId);
        }

        var existente = matriculaRepository.buscarPorTerna(estudianteId, asignaturaId, semestreId);
        if (existente.isPresent()) {
            Matricula matricula = existente.get();
            if (matricula.getEstado() == EstadoMatricula.ACTIVA) {
                throw MatriculaDuplicadaException.porTerna(estudianteId, asignaturaId, semestreId);
            }
            matricula.setEstado(EstadoMatricula.ACTIVA);
            matricula.setFechaMatricula(hoy);
            return new ResultadoMatricula(matriculaRepository.save(matricula), true);
        }

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setAsignatura(asignatura);
        matricula.setSemestre(semestre);
        matricula.setFechaMatricula(hoy);
        matricula.setEstado(EstadoMatricula.ACTIVA);
        return new ResultadoMatricula(matriculaRepository.save(matricula), false);
    }

    @Override
    @Transactional
    public void desvincular(UUID matriculaId, boolean confirmado) {
        desvincular(matriculaId, confirmado, currentUser.required());
    }

    @Override
    @Transactional
    public void desvincular(UUID matriculaId, boolean confirmado, UsuarioPrincipal principal) {
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> MatriculaNoEncontradaException.porId(matriculaId));
        asegurarAlcance(matricula.getAsignatura().getId(), principal);

        if (matricula.getEstado() == EstadoMatricula.DESVINCULADA) {
            throw MatriculaYaInactivaException.porId(matriculaId);
        }

        long cantidadCalificaciones = calificacionesDeMatricula.contarPorMatricula(matriculaId);
        if (cantidadCalificaciones > 0 && !confirmado) {
            throw ConfirmacionRequeridaException.porCalificacionesRegistradas(matriculaId, cantidadCalificaciones);
        }

        matricula.setEstado(EstadoMatricula.DESVINCULADA);
        matriculaRepository.save(matricula);
    }

    @Override
    @Transactional
    public List<Matricula> listarMatriculados(UUID asignaturaId, UUID semestreId, boolean incluirInactivas) {
        return listarMatriculadosInterno(asignaturaId, semestreId, incluirInactivas, currentUser.required());
    }

    @Override
    @Transactional
    public List<Matricula> listarMatriculados(UUID asignaturaId, UUID semestreId, boolean incluirInactivas,
                                              UsuarioPrincipal principal) {
        return listarMatriculadosInterno(asignaturaId, semestreId, incluirInactivas, principal);
    }

    private List<Matricula> listarMatriculadosInterno(UUID asignaturaId, UUID semestreId,
                                                      boolean incluirInactivas, UsuarioPrincipal principal) {
        asegurarAlcance(asignaturaId, principal);
        if (!asignaturaRepository.existsById(asignaturaId)) {
            throw ReferenciaNoEncontradaException.asignatura(asignaturaId);
        }
        if (!semestreRepository.existsById(semestreId)) {
            throw ReferenciaNoEncontradaException.semestre(semestreId);
        }
        EstadoMatricula estado = incluirInactivas ? null : EstadoMatricula.ACTIVA;
        return matriculaRepository.buscarPorAsignaturaYSemestre(asignaturaId, semestreId, estado);
    }

    private void asegurarAlcance(UUID asignaturaId, UsuarioPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No hay una identidad autenticada válida");
        }
        if (principal.rol() == RolUsuario.ADMINISTRADOR) {
            return;
        }
        if (principal.rol() != RolUsuario.PROFESOR
                || asignacionDocenteRepository == null
                || !asignacionDocenteRepository.existsByProfesor_IdAndAsignatura_IdAndEstado(
                principal.id(), asignaturaId, EstadoRegistro.ACTIVO)) {
            throw new AccessDeniedException("El profesor no tiene asignada esta asignatura");
        }
    }

    @Override
    @Transactional
    public List<Matricula> listarAsignaturasDeEstudiante(UUID estudianteId) {
        return listarAsignaturasDeEstudiante(estudianteId, currentUser.required());
    }

    @Override
    @Transactional
    public List<Matricula> listarAsignaturasDeEstudiante(UUID estudianteId, UsuarioPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No hay una identidad autenticada válida");
        }
        if (!estudianteRepository.existsById(estudianteId)) {
            throw EstudianteNoEncontradoException.porId(estudianteId);
        }
        if (principal.rol() == RolUsuario.ADMINISTRADOR
                || (principal.rol() == RolUsuario.ESTUDIANTE && principal.id().equals(estudianteId))) {
            return matriculaRepository.buscarPorEstudiante(estudianteId);
        }
        if (principal.rol() == RolUsuario.PROFESOR) {
            return matriculaRepository.buscarPorEstudianteYProfesor(
                    estudianteId, principal.id(), EstadoMatricula.ACTIVA, EstadoRegistro.ACTIVO,
                    EstadoRegistro.ACTIVO);
        }
        throw new AccessDeniedException("El usuario no tiene acceso a las matrículas del estudiante");
    }

    @Override
    @Transactional
    public ResultadoAltaConMatricula registrarYMatricular(EstudianteRequestDTO estudianteDTO,
                                                            UUID asignaturaId, UUID semestreId) {
        if (estudianteService == null) {
            throw new IllegalStateException("EstudianteService no está disponible");
        }
        EstudianteResponseDTO estudianteRegistrado = estudianteService.registrar(estudianteDTO);
        ResultadoMatricula resultadoMatricula = this.matricular(estudianteRegistrado.id(), asignaturaId, semestreId);
        return new ResultadoAltaConMatricula(estudianteRegistrado, resultadoMatricula.matricula());
    }
}
