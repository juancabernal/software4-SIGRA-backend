package co.edu.uco.sigra.estudiantes.service.impl;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
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
import org.springframework.beans.factory.annotation.Qualifier;
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

    // Constructor explícito (no @RequiredArgsConstructor) porque Lombok no copia @Qualifier.
    public MatriculaServiceImpl(MatriculaRepository matriculaRepository,
                                EstudianteRepository estudianteRepository,
                                AsignaturaRepository asignaturaRepository,
                                SemestreRepository semestreRepository,
                                @Qualifier(MatriculaConfig.RELOJ_MATRICULAS) Clock reloj,
                                CalificacionesDeMatricula calificacionesDeMatricula,
                                EstudianteService estudianteService) {
        this.matriculaRepository = matriculaRepository;
        this.estudianteRepository = estudianteRepository;
        this.asignaturaRepository = asignaturaRepository;
        this.semestreRepository = semestreRepository;
        this.reloj = reloj;
        this.calificacionesDeMatricula = calificacionesDeMatricula;
        this.estudianteService = estudianteService;
    }

    @Override
    @Transactional
    public ResultadoMatricula matricular(UUID estudianteId, UUID asignaturaId, UUID semestreId) {
        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> EstudianteNoEncontradoException.porId(estudianteId));

        Asignatura asignatura = asignaturaRepository.findById(asignaturaId)
                .orElseThrow(() -> ReferenciaNoEncontradaException.asignatura(asignaturaId));

        Semestre semestre = semestreRepository.findById(semestreId)
                .orElseThrow(() -> ReferenciaNoEncontradaException.semestre(semestreId));

        // El estado de la ficha del estudiante sí es EstadoRegistro: es una baja de registro,
        // no una matrícula retirada.
        if (estudiante.getEstado() != EstadoRegistro.ACTIVO) {
            throw ReglaDeMatriculaException.estudianteInactivo(estudianteId);
        }

        if (asignatura.getEstado() != EstadoAsignatura.ACTIVA) {
            throw ReglaDeMatriculaException.asignaturaNoActiva(asignaturaId);
        }

        LocalDate hoy = LocalDate.now(reloj);

        // La vigencia del semestre se juzga por sus fechas, no por su marca de estado
        // almacenada: EstadoSemestre se recalcula de forma perezosa, solo cuando el módulo
        // de semestres consulta ese semestre, así que el valor guardado puede estar obsoleto
        // en cualquier dirección. Las fechas son la fuente de verdad de la que ese estado se
        // deriva, y consultarlas aquí evita además una llamada hacia otro módulo (y su posible
        // escritura lateral) como efecto de una matrícula.
        if (hoy.isBefore(semestre.getFechaInicio()) || hoy.isAfter(semestre.getFechaFin())) {
            throw ReglaDeMatriculaException.semestreNoVigente(semestreId);
        }

        var existente = matriculaRepository.buscarPorTerna(estudianteId, asignaturaId, semestreId);

        if (existente.isPresent()) {
            Matricula matricula = existente.get();

            if (matricula.getEstado() == EstadoMatricula.ACTIVA) {
                throw MatriculaDuplicadaException.porTerna(estudianteId, asignaturaId, semestreId);
            }

            // Reactiva la matrícula retirada de la misma terna en lugar de crear una fila nueva,
            // y actualiza la fecha a la de la rematrícula: es cuando el vínculo vigente empezó.
            matricula.setEstado(EstadoMatricula.ACTIVA);
            matricula.setFechaMatricula(hoy);
            Matricula reactivada = matriculaRepository.save(matricula);
            return new ResultadoMatricula(reactivada, true);
        }

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setAsignatura(asignatura);
        matricula.setSemestre(semestre);
        matricula.setFechaMatricula(hoy);
        matricula.setEstado(EstadoMatricula.ACTIVA);

        Matricula creada = matriculaRepository.save(matricula);
        return new ResultadoMatricula(creada, false);
    }

    @Override
    @Transactional
    public void desvincular(UUID matriculaId, boolean confirmado) {
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> MatriculaNoEncontradaException.porId(matriculaId));

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
        if (!asignaturaRepository.existsById(asignaturaId)) {
            throw ReferenciaNoEncontradaException.asignatura(asignaturaId);
        }

        if (!semestreRepository.existsById(semestreId)) {
            throw ReferenciaNoEncontradaException.semestre(semestreId);
        }

        EstadoMatricula estado = incluirInactivas ? null : EstadoMatricula.ACTIVA;
        return matriculaRepository.buscarPorAsignaturaYSemestre(asignaturaId, semestreId, estado);
    }

    @Override
    @Transactional
    public List<Matricula> listarAsignaturasDeEstudiante(UUID estudianteId) {
        if (!estudianteRepository.existsById(estudianteId)) {
            throw EstudianteNoEncontradoException.porId(estudianteId);
        }

        return matriculaRepository.buscarPorEstudiante(estudianteId);
    }

    @Override
    @Transactional
    public ResultadoAltaConMatricula registrarYMatricular(EstudianteRequestDTO estudianteDTO, UUID asignaturaId, UUID semestreId) {
        // Registrar primero y matricular después es lo que hace que un fallo de matrícula
        // deshaga el alta: EstudianteServiceImpl.registrar también es @Transactional con
        // propagación REQUIRED, así que se adhiere a esta misma transacción, y cualquier
        // excepción de negocio lanzada por this.matricular (son todas RuntimeException) marca
        // el rollback completo, sin dejar al estudiante creado.
        EstudianteResponseDTO estudianteRegistrado = estudianteService.registrar(estudianteDTO);

        ResultadoMatricula resultadoMatricula = this.matricular(estudianteRegistrado.id(), asignaturaId, semestreId);

        return new ResultadoAltaConMatricula(estudianteRegistrado, resultadoMatricula.matricula());
    }
}
