package co.edu.uco.sigra.estudiantes.service;

import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estudiantes.dto.*;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.exception.MatriculaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.repository.MiProcesoConsultaRepository;
import co.edu.uco.sigra.estudiantes.service.impl.MiProcesoServiceImpl;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.calificaciones.entity.Calificacion;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MiProcesoServiceImplTest {

    @Mock
    private MiProcesoConsultaRepository repository;
    @Mock
    private CurrentUser currentUser;

    private final UUID estudianteId = UUID.randomUUID();

    @AfterEach
    void limpiarContexto() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void clasificaMateriasEnCursoCursadaYDesvinculadaYOrdena() {
        when(currentUser.required()).thenReturn(new UsuarioPrincipal(estudianteId, RolUsuario.ESTUDIANTE));
        Matricula enCurso = matricula("Zeta", EstadoSemestre.ACTIVO, EstadoMatricula.ACTIVA, 3);
        Matricula cursada = matricula("Alfa", EstadoSemestre.INACTIVO, EstadoMatricula.ACTIVA, 2);
        Matricula desvinculada = matricula("Beta", EstadoSemestre.ACTIVO, EstadoMatricula.DESVINCULADA, 1);
        when(repository.buscarMatriculasDelEstudiante(estudianteId))
                .thenReturn(List.of(enCurso, cursada, desvinculada));

        MiProcesoService service = servicio();

        assertThat(service.listarMaterias()).extracting(MiMateriaDTO::asignaturaNombre)
                .containsExactly("Zeta");
        assertThat(service.consultarHistorial()).extracting(MiMateriaDTO::asignaturaNombre)
                .containsExactly("Alfa", "Beta");
    }

    @Test
    void progresoVacioNoEjecutaConsultasDeDetalle() {
        when(currentUser.required()).thenReturn(new UsuarioPrincipal(estudianteId, RolUsuario.ESTUDIANTE));
        when(repository.buscarMatriculasDelEstudiante(estudianteId)).thenReturn(List.of());

        assertThat(servicio().consultarProgreso()).isEmpty();

        verify(repository, never()).buscarResultadosPorAsignaturas(anyList(), any());
        verify(repository, never()).buscarEvaluaciones(anyList(), anyList(), any());
        verify(repository, never()).buscarCalificaciones(anyList(), any());
        verify(repository, never()).buscarNiveles(any(), anyList(), anyList());
    }

    @Test
    void detalleAjenoOInexistenteUsaElMismo404Generico() {
        when(currentUser.required()).thenReturn(new UsuarioPrincipal(estudianteId, RolUsuario.ESTUDIANTE));
        UUID matriculaId = UUID.randomUUID();
        when(repository.buscarMatriculaPropia(matriculaId, estudianteId)).thenReturn(Optional.empty());

        MiProcesoService service = servicio();

        Throwable error = catchThrowable(() -> service.consultarMateria(matriculaId));
        assertThat(error).isInstanceOf(MatriculaNoEncontradaException.class)
                .hasMessage("No existe la matrícula solicitada.");
    }

    @Test
    void detalleMuestraEvaluacionesCalificacionesConteosYNivelSinEvaluar() {
        when(currentUser.required()).thenReturn(new UsuarioPrincipal(estudianteId, RolUsuario.ESTUDIANTE));
        UUID matriculaId = UUID.randomUUID();
        UUID asignaturaId = UUID.randomUUID();
        UUID semestreId = UUID.randomUUID();
        UUID resultadoId = UUID.randomUUID();
        UUID evaluacionId = UUID.randomUUID();
        Asignatura asignatura = mock(Asignatura.class);
        Semestre semestre = mock(Semestre.class);
        Matricula matricula = mock(Matricula.class);
        ResultadoAprendizaje resultado = mock(ResultadoAprendizaje.class);
        Evaluacion evaluacion = mock(Evaluacion.class);
        when(matricula.getId()).thenReturn(matriculaId);
        when(matricula.getAsignatura()).thenReturn(asignatura);
        when(matricula.getSemestre()).thenReturn(semestre);
        when(matricula.getEstado()).thenReturn(EstadoMatricula.ACTIVA);
        when(asignatura.getId()).thenReturn(asignaturaId);
        when(asignatura.getCodigo()).thenReturn("MAT-1");
        when(asignatura.getNombre()).thenReturn("Materia");
        when(semestre.getId()).thenReturn(semestreId);
        when(semestre.getCodigo()).thenReturn("2026-1");
        when(semestre.getEstado()).thenReturn(EstadoSemestre.ACTIVO);
        when(resultado.getId()).thenReturn(resultadoId);
        when(resultado.getCodigo()).thenReturn("RA-1");
        when(resultado.getDescripcion()).thenReturn("Describe");
        when(resultado.getAsignatura()).thenReturn(asignatura);
        when(evaluacion.getId()).thenReturn(evaluacionId);
        when(evaluacion.getResultadoAprendizaje()).thenReturn(resultado);
        when(evaluacion.getSemestre()).thenReturn(semestre);
        when(evaluacion.getNombre()).thenReturn("Parcial");
        when(evaluacion.getFechaTentativa()).thenReturn(LocalDate.of(2026, 3, 1));
        when(evaluacion.getPorcentaje()).thenReturn(java.math.BigDecimal.valueOf(50));
        when(repository.buscarMatriculaPropia(matriculaId, estudianteId)).thenReturn(Optional.of(matricula));
        when(repository.buscarResultadosPorAsignaturas(List.of(asignaturaId), EstadoRegistro.ACTIVO))
                .thenReturn(List.of(resultado));
        when(repository.buscarEvaluaciones(List.of(resultadoId), List.of(semestreId),
                co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion.ACTIVA)).thenReturn(List.of(evaluacion));
        when(repository.buscarCalificaciones(List.of(matriculaId),
                co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion.ACTIVA)).thenReturn(List.of());
        when(repository.buscarNiveles(estudianteId, List.of(semestreId), List.of(resultadoId)))
                .thenReturn(List.of());

        MiMateriaDetalleDTO detalle = servicio()
                .consultarMateria(matriculaId);

        assertThat(detalle.resultadosAprendizaje()).singleElement().satisfies(ra -> {
            assertThat(ra.nivelLogro()).isEqualTo(MiNivelLogro.SIN_EVALUAR);
            assertThat(ra.promedio()).isNull();
            assertThat(ra.evaluacionesCalificadas()).isZero();
            assertThat(ra.evaluacionesTotales()).isOne();
            assertThat(ra.evaluaciones()).singleElement().satisfies(e -> {
                assertThat(e.nota()).isNull();
                assertThat(e.observacion()).isNull();
            });
        });
    }

    @Test
    void semestreInactivoEnElBordeEsProgramadoDesdeHoyYCursadoElDiaAnterior() {
        LocalDate hoy = LocalDate.of(2026, 11, 12);
        Matricula desdeHoy = matricula("DesdeHoy", EstadoSemestre.INACTIVO, EstadoMatricula.ACTIVA, 1);
        desdeHoy.getSemestre().setFechaInicio(hoy);
        Matricula anterior = matricula("Anterior", EstadoSemestre.INACTIVO, EstadoMatricula.ACTIVA, 2);
        anterior.getSemestre().setFechaInicio(hoy.minusDays(1));

        assertThat(MiProcesoServiceImpl.situacion(desdeHoy, hoy))
                .isEqualTo(SituacionMateria.PROGRAMADA);
        assertThat(MiProcesoServiceImpl.situacion(anterior, hoy))
                .isEqualTo(SituacionMateria.CURSADA);
    }

    @Test
    void semestreInactivoSinFechaDeInicioSeClasificaComoCursada() {
        Matricula matricula = matricula("SinFecha", EstadoSemestre.INACTIVO, EstadoMatricula.ACTIVA, 1);
        matricula.getSemestre().setFechaInicio(null);

        assertThat(MiProcesoServiceImpl.situacion(matricula, LocalDate.of(2026, 11, 12)))
                .isEqualTo(SituacionMateria.CURSADA);
    }

    @Test
    void ordenamientosToleranNulosYLosDejanAlFinal() {
        when(currentUser.required()).thenReturn(new UsuarioPrincipal(estudianteId, RolUsuario.ESTUDIANTE));
        Matricula conNombre = matricula("ConNombre", EstadoSemestre.ACTIVO, EstadoMatricula.ACTIVA, 1);
        Matricula sinNombre = matricula("SinNombre", EstadoSemestre.ACTIVO, EstadoMatricula.ACTIVA, 2);
        sinNombre.getAsignatura().renombrar(null);
        when(repository.buscarMatriculasDelEstudiante(estudianteId))
                .thenReturn(List.of(sinNombre, conNombre));

        assertThat(servicio().listarMaterias()).extracting(MiMateriaDTO::asignaturaNombre)
                .containsExactly("ConNombre", (String) null);

        UUID asignaturaId = conNombre.getAsignatura().getId();
        UUID semestreId = conNombre.getSemestre().getId();
        ResultadoAprendizaje resultado = mock(ResultadoAprendizaje.class);
        Evaluacion conFecha = mock(Evaluacion.class);
        Evaluacion sinFecha = mock(Evaluacion.class);
        UUID resultadoId = UUID.randomUUID();
        when(resultado.getId()).thenReturn(resultadoId);
        when(resultado.getCodigo()).thenReturn("RA-1");
        when(resultado.getDescripcion()).thenReturn("Resultado");
        when(resultado.getAsignatura()).thenReturn(conNombre.getAsignatura());
        when(conFecha.getId()).thenReturn(UUID.randomUUID());
        when(conFecha.getResultadoAprendizaje()).thenReturn(resultado);
        when(conFecha.getSemestre()).thenReturn(conNombre.getSemestre());
        when(conFecha.getFechaTentativa()).thenReturn(LocalDate.of(2026, 2, 1));
        when(conFecha.getNombre()).thenReturn("ConFecha");
        when(sinFecha.getId()).thenReturn(UUID.randomUUID());
        when(sinFecha.getResultadoAprendizaje()).thenReturn(resultado);
        when(sinFecha.getSemestre()).thenReturn(conNombre.getSemestre());
        when(sinFecha.getFechaTentativa()).thenReturn(null);
        when(sinFecha.getNombre()).thenReturn(null);
        when(repository.buscarMatriculaPropia(conNombre.getId(), estudianteId))
                .thenReturn(Optional.of(conNombre));
        when(repository.buscarResultadosPorAsignaturas(List.of(asignaturaId), EstadoRegistro.ACTIVO))
                .thenReturn(List.of(resultado));
        when(repository.buscarEvaluaciones(List.of(resultadoId), List.of(semestreId),
                co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion.ACTIVA))
                .thenReturn(List.of(sinFecha, conFecha));
        when(repository.buscarCalificaciones(List.of(conNombre.getId()),
                co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion.ACTIVA))
                .thenReturn(List.of());
        when(repository.buscarNiveles(estudianteId, List.of(semestreId), List.of(resultadoId)))
                .thenReturn(List.of());

        assertThat(servicio().consultarMateria(conNombre.getId()).resultadosAprendizaje())
                .singleElement().satisfies(ra -> assertThat(ra.evaluaciones())
                        .extracting(MiEvaluacionDTO::nombre)
                        .containsExactly("ConFecha", (String) null));
    }

    private MiProcesoService servicio() {
        return new MiProcesoServiceImpl(repository, currentUser,
                Clock.system(ZoneId.of("America/Bogota")));
    }

    private Matricula matricula(String nombre, EstadoSemestre estadoSemestre,
                                EstadoMatricula estadoMatricula, int day) {
        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setNombre("Programa");
        Asignatura asignatura = new Asignatura("COD-" + nombre, nombre, programa);
        setId(asignatura, UUID.randomUUID());
        asignatura.activar(5);
        Semestre semestre = new Semestre();
        setId(semestre, UUID.randomUUID());
        semestre.setCodigo("2026-" + day);
        semestre.setFechaInicio(LocalDate.of(2026, 1, 1));
        semestre.setFechaFin(LocalDate.of(2026, 12, 31));
        semestre.setEstado(estadoSemestre);
        Matricula matricula = new Matricula();
        setId(matricula, UUID.randomUUID());
        matricula.setAsignatura(asignatura);
        matricula.setSemestre(semestre);
        matricula.setFechaMatricula(LocalDate.of(2026, 2, day));
        matricula.setEstado(estadoMatricula);
        return matricula;
    }

    private static void setId(Object target, UUID id) {
        try {
            Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
