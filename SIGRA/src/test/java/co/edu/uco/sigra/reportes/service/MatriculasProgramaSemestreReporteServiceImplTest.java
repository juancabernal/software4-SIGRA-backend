package co.edu.uco.sigra.reportes.service;

import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.reportes.dto.MatriculasProgramaSemestreReporteResponseDTO;
import co.edu.uco.sigra.reportes.repository.MatriculasProgramaSemestreReporteRepository;
import co.edu.uco.sigra.reportes.service.impl.MatriculasProgramaSemestreReporteServiceImpl;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MatriculasProgramaSemestreReporteServiceImplTest {

    private final MatriculasProgramaSemestreReporteRepository repository = mock();
    private final SemestreRepository semestreRepository = mock();
    private final ProgramaAcademicoRepository programaRepository = mock();
    private final MatriculasProgramaSemestreReporteService service =
            new MatriculasProgramaSemestreReporteServiceImpl(repository, semestreRepository, programaRepository);

    @Test
    void resumeFilasYUsaConsultaIndependienteDeEstudiantes() {
        UUID semestre = UUID.randomUUID();
        UUID programaUno = UUID.randomUUID();
        UUID programaDos = UUID.randomUUID();
        UUID facultad = UUID.randomUUID();
        var filaUno = fila(semestre, programaUno, "Programa A", 3, 2, 1, 2, 1);
        var filaDos = fila(semestre, programaDos, "Programa B", 4, 1, 3, 2, 2);
        when(semestreRepository.existsById(semestre)).thenReturn(true);
        when(programaRepository.existsById(programaUno)).thenReturn(true);
        when(repository.agrupar(eq(semestre), eq(programaUno), eq(facultad),
                eq(EstadoMatricula.ACTIVA), eq(EstadoMatricula.DESVINCULADA)))
                .thenReturn(List.of(filaUno, filaDos));
        when(repository.contarEstudiantesDistintos(semestre, programaUno, facultad)).thenReturn(5L);

        MatriculasProgramaSemestreReporteResponseDTO response =
                service.consultar(semestre, programaUno, facultad);

        assertThat(response.resumen().totalMatriculas()).isEqualTo(7);
        assertThat(response.resumen().activas()).isEqualTo(3);
        assertThat(response.resumen().desvinculadas()).isEqualTo(4);
        assertThat(response.resumen().estudiantesDistintos()).isEqualTo(5);
        assertThat(response.resumen().programas()).isEqualTo(2);
        assertThat(response.resumen().semestres()).isEqualTo(1);
        assertThat(response.filas()).hasSize(2);
        assertThat(response.filas().get(0).semestreId()).isEqualTo(semestre);
        assertThat(response.filas().get(0).semestreCodigo()).isEqualTo("2026-1");
        assertThat(response.filas().get(0).semestreFechaInicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(response.filas().get(0).semestreEstado()).isEqualTo(EstadoSemestre.ACTIVO);
        assertThat(response.filas().get(0).programaId()).isEqualTo(programaUno);
        assertThat(response.filas().get(0).programaCodigo()).isEqualTo("P-Programa A");
        assertThat(response.filas().get(0).programaNombre()).isEqualTo("Programa A");
        assertThat(response.filas().get(0).facultadNombre()).isEqualTo("Facultad");
        assertThat(response.filas().get(0).totalMatriculas()).isEqualTo(3);
        assertThat(response.filas().get(0).activas()).isEqualTo(2);
        assertThat(response.filas().get(0).desvinculadas()).isEqualTo(1);
        assertThat(response.filas().get(0).estudiantes()).isEqualTo(2);
        assertThat(response.filas().get(0).asignaturas()).isEqualTo(1);
        assertThat(response.filas().get(1).programaId()).isEqualTo(programaDos);
        verify(repository).contarEstudiantesDistintos(semestre, programaUno, facultad);
    }

    @Test
    void sinFilasDevuelveResumenEnCeroYConservaFiltrosNulos() {
        when(repository.agrupar(null, null, null, EstadoMatricula.ACTIVA, EstadoMatricula.DESVINCULADA))
                .thenReturn(List.of());
        when(repository.contarEstudiantesDistintos(null, null, null)).thenReturn(0L);

        var response = service.consultar(null, null, null);

        assertThat(response.semestreId()).isNull();
        assertThat(response.programaId()).isNull();
        assertThat(response.facultadId()).isNull();
        assertThat(response.filas()).isEmpty();
        assertThat(response.resumen().totalMatriculas()).isZero();
        assertThat(response.resumen().estudiantesDistintos()).isZero();
        verify(repository).agrupar(null, null, null, EstadoMatricula.ACTIVA, EstadoMatricula.DESVINCULADA);
        verify(repository).contarEstudiantesDistintos(null, null, null);
    }

    @Test
    void semestreInexistenteEs404YNoConsultaReporte() {
        UUID id = UUID.randomUUID();
        when(semestreRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.consultar(id, null, null))
                .isInstanceOf(co.edu.uco.sigra.reportes.exception.ReporteRecursoNoEncontradoException.class)
                .hasMessage("El semestre indicado no existe.");
        verifyNoInteractions(repository);
    }

    @Test
    void programaInexistenteEs404YNoConsultaReporte() {
        UUID id = UUID.randomUUID();
        when(semestreRepository.existsById(any())).thenReturn(true);
        when(programaRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.consultar(UUID.randomUUID(), id, null))
                .isInstanceOf(co.edu.uco.sigra.reportes.exception.ReporteRecursoNoEncontradoException.class)
                .hasMessage("El programa académico indicado no existe.");
        verifyNoInteractions(repository);
    }

    @Test
    void facultadNoSeValidaYDevuelveVacio() {
        UUID semestre = UUID.randomUUID();
        UUID programa = UUID.randomUUID();
        when(semestreRepository.existsById(semestre)).thenReturn(true);
        when(programaRepository.existsById(programa)).thenReturn(true);
        when(repository.agrupar(any(), any(), any(), eq(EstadoMatricula.ACTIVA), eq(EstadoMatricula.DESVINCULADA)))
                .thenReturn(List.of());
        when(repository.contarEstudiantesDistintos(any(), any(), any())).thenReturn(0L);

        var response = service.consultar(semestre, programa, UUID.randomUUID());

        assertThat(response.filas()).isEmpty();
        verify(programaRepository).existsById(any());
        verify(repository).agrupar(any(), any(), any(), eq(EstadoMatricula.ACTIVA), eq(EstadoMatricula.DESVINCULADA));
    }

    private MatriculasProgramaSemestreReporteRepository.Fila fila(
            UUID semestreId, UUID programaId, String nombre, long total, long activas,
            long desvinculadas, long estudiantes, long asignaturas) {
        var fila = mock(MatriculasProgramaSemestreReporteRepository.Fila.class);
        when(fila.getSemestreId()).thenReturn(semestreId);
        when(fila.getSemestreCodigo()).thenReturn("2026-1");
        when(fila.getSemestreFechaInicio()).thenReturn(LocalDate.of(2026, 1, 1));
        when(fila.getSemestreEstado()).thenReturn(EstadoSemestre.ACTIVO);
        when(fila.getProgramaId()).thenReturn(programaId);
        when(fila.getProgramaCodigo()).thenReturn("P-" + nombre);
        when(fila.getProgramaNombre()).thenReturn(nombre);
        when(fila.getFacultadNombre()).thenReturn("Facultad");
        when(fila.getTotalMatriculas()).thenReturn(total);
        when(fila.getActivas()).thenReturn(activas);
        when(fila.getDesvinculadas()).thenReturn(desvinculadas);
        when(fila.getEstudiantes()).thenReturn(estudiantes);
        when(fila.getAsignaturas()).thenReturn(asignaturas);
        return fila;
    }
}
