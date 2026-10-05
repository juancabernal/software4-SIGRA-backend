package co.edu.uco.sigra.estudiantes.service.impl;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.exception.EstudianteNoEncontradoException;
import co.edu.uco.sigra.estudiantes.exception.MatriculaDuplicadaException;
import co.edu.uco.sigra.estudiantes.exception.MatriculaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.exception.MatriculaYaInactivaException;
import co.edu.uco.sigra.estudiantes.exception.ReferenciaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.exception.ReglaDeMatriculaException;
import co.edu.uco.sigra.estudiantes.repository.EstudianteRepository;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.estudiantes.service.MatriculaService;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatriculaServiceImpl implements MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final EstudianteRepository estudianteRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final SemestreRepository semestreRepository;

    @Override
    @Transactional
    public ResultadoMatricula matricular(UUID estudianteId, UUID asignaturaId, UUID semestreId) {
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

        if (semestre.getEstado() != EstadoSemestre.ACTIVO) {
            throw ReglaDeMatriculaException.semestreNoActivo(semestreId);
        }

        var existente = matriculaRepository.buscarPorTerna(estudianteId, asignaturaId, semestreId);

        if (existente.isPresent()) {
            Matricula matricula = existente.get();

            if (matricula.getEstado() == EstadoRegistro.ACTIVO) {
                throw MatriculaDuplicadaException.porTerna(estudianteId, asignaturaId, semestreId);
            }

            // Reactiva la matrícula retirada de la misma terna en lugar de crear una fila nueva.
            matricula.setEstado(EstadoRegistro.ACTIVO);
            Matricula reactivada = matriculaRepository.save(matricula);
            return new ResultadoMatricula(reactivada, true);
        }

        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setAsignatura(asignatura);
        matricula.setSemestre(semestre);
        matricula.setEstado(EstadoRegistro.ACTIVO);

        Matricula creada = matriculaRepository.save(matricula);
        return new ResultadoMatricula(creada, false);
    }

    @Override
    @Transactional
    public void desvincular(UUID matriculaId) {
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> MatriculaNoEncontradaException.porId(matriculaId));

        if (matricula.getEstado() == EstadoRegistro.INACTIVO) {
            throw MatriculaYaInactivaException.porId(matriculaId);
        }

        matricula.setEstado(EstadoRegistro.INACTIVO);
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

        EstadoRegistro estado = incluirInactivas ? null : EstadoRegistro.ACTIVO;
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
}
