package co.edu.uco.sigra.reportes.service.impl;

import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.reportes.dto.MatriculasProgramaSemestreReporteFilaDTO;
import co.edu.uco.sigra.reportes.dto.MatriculasProgramaSemestreReporteResponseDTO;
import co.edu.uco.sigra.reportes.dto.MatriculasProgramaSemestreReporteResumenDTO;
import co.edu.uco.sigra.reportes.repository.MatriculasProgramaSemestreReporteRepository;
import co.edu.uco.sigra.reportes.service.MatriculasProgramaSemestreReporteService;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import co.edu.uco.sigra.reportes.exception.ReporteRecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatriculasProgramaSemestreReporteServiceImpl implements MatriculasProgramaSemestreReporteService {

    private final MatriculasProgramaSemestreReporteRepository repository;
    private final SemestreRepository semestreRepository;
    private final ProgramaAcademicoRepository programaRepository;

    @Override
    @Transactional(readOnly = true)
    public MatriculasProgramaSemestreReporteResponseDTO consultar(UUID semestreId, UUID programaId, UUID facultadId) {
        if (semestreId != null && !semestreRepository.existsById(semestreId)) {
            throw new ReporteRecursoNoEncontradoException("El semestre indicado no existe.");
        }
        if (programaId != null && !programaRepository.existsById(programaId)) {
            throw new ReporteRecursoNoEncontradoException("El programa académico indicado no existe.");
        }
        List<MatriculasProgramaSemestreReporteRepository.Fila> filas = repository.agrupar(
                semestreId, programaId, facultadId, EstadoMatricula.ACTIVA, EstadoMatricula.DESVINCULADA);
        long total = filas.stream().mapToLong(f -> f.getTotalMatriculas()).sum();
        long activas = filas.stream().mapToLong(f -> f.getActivas()).sum();
        long desvinculadas = filas.stream().mapToLong(f -> f.getDesvinculadas()).sum();
        long programas = filas.stream().map(MatriculasProgramaSemestreReporteRepository.Fila::getProgramaId).distinct().count();
        long semestres = filas.stream().map(MatriculasProgramaSemestreReporteRepository.Fila::getSemestreId).distinct().count();
        var resumen = new MatriculasProgramaSemestreReporteResumenDTO(total, activas, desvinculadas,
                repository.contarEstudiantesDistintos(semestreId, programaId, facultadId), programas, semestres);
        var respuesta = filas.stream().map(f -> new MatriculasProgramaSemestreReporteFilaDTO(
                f.getSemestreId(), f.getSemestreCodigo(), f.getSemestreFechaInicio(), f.getSemestreEstado(),
                f.getProgramaId(), f.getProgramaCodigo(), f.getProgramaNombre(), f.getFacultadNombre(),
                f.getTotalMatriculas(), f.getActivas(), f.getDesvinculadas(), f.getEstudiantes(), f.getAsignaturas()
        )).toList();
        return new MatriculasProgramaSemestreReporteResponseDTO(semestreId, programaId, facultadId, resumen, respuesta);
    }
}
