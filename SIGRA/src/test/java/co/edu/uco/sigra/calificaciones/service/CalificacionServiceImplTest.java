package co.edu.uco.sigra.calificaciones.service;

import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.calificaciones.config.CalificacionConfig;
import co.edu.uco.sigra.calificaciones.dto.CalificacionModificarRequestDTO;
import co.edu.uco.sigra.calificaciones.dto.CalificacionRequestDTO;
import co.edu.uco.sigra.calificaciones.dto.CalificacionResponseDTO;
import co.edu.uco.sigra.calificaciones.dto.EvaluacionCalificableDTO;
import co.edu.uco.sigra.calificaciones.dto.FilaPlanillaDTO;
import co.edu.uco.sigra.calificaciones.dto.HistorialCalificacionDTO;
import co.edu.uco.sigra.calificaciones.dto.PlanillaCalificacionesDTO;
import co.edu.uco.sigra.calificaciones.entity.Calificacion;
import co.edu.uco.sigra.calificaciones.entity.HistorialCalificacion;
import co.edu.uco.sigra.calificaciones.entity.TipoEventoCalificacion;
import co.edu.uco.sigra.calificaciones.exception.CalificacionDuplicadaException;
import co.edu.uco.sigra.calificaciones.exception.CalificacionNoEncontradaException;
import co.edu.uco.sigra.calificaciones.exception.CalificacionReglaNegocioException;
import co.edu.uco.sigra.calificaciones.mapper.CalificacionMapper;
import co.edu.uco.sigra.calificaciones.repository.CalificacionRepository;
import co.edu.uco.sigra.calificaciones.repository.HistorialCalificacionRepository;
import co.edu.uco.sigra.calificaciones.service.impl.CalificacionServiceImpl;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.exception.MatriculaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionNoEncontradaException;
import co.edu.uco.sigra.evaluaciones.repository.EvaluacionRepository;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de calificaciones (RF-12 / RF-13). Repositorios simulados con Mockito,
 * mapper real de MapStruct y reloj fijo el 2026-10-15 a las 10:00 (America/Bogota), para decidir de
 * forma determinista si una nota queda fuera de la fecha tentativa. El 403 es AccessDeniedException,
 * que el manejador global traduce.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de calificaciones")
class CalificacionServiceImplTest {

    private static final UUID ID_PROFESOR = UUID.randomUUID();
    private static final UsuarioPrincipal PROFESOR = new UsuarioPrincipal(ID_PROFESOR, RolUsuario.PROFESOR);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 15, 10, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();
    private static final String OBSERVACION = "Buen desarrollo del parcial";

    @Mock private CalificacionRepository calificacionRepository;
    @Mock private HistorialCalificacionRepository historialRepository;
    @Mock private EvaluacionRepository evaluacionRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private AsignacionDocenteRepository asignacionDocenteRepository;

    private final CalificacionMapper mapper = Mappers.getMapper(CalificacionMapper.class);

    private CalificacionServiceImpl service;

    private Asignatura asignatura;
    private Semestre semestre;
    private Evaluacion evaluacion;
    private Matricula matricula;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-15T15:00:00Z"), CalificacionConfig.ZONA_COLOMBIA);
        service = new CalificacionServiceImpl(calificacionRepository, historialRepository, evaluacionRepository,
                matriculaRepository, asignacionDocenteRepository, mapper, reloj);

        asignatura = asignatura("ASG-101");
        semestre = semestre("2026-2");
        evaluacion = evaluacion(asignatura, semestre, HOY);
        matricula = matricula("Ana Gómez", asignatura, semestre, EstadoMatricula.ACTIVA);
    }

    // ------------------------------------------------------------------ datos de prueba

    private static Asignatura asignatura(String codigo) {
        Asignatura a = new Asignatura(codigo, "Ingeniería de Software", new ProgramaAcademico());
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        return a;
    }

    private static Semestre semestre(String codigo) {
        Semestre s = new Semestre();
        s.setId(UUID.randomUUID());
        s.setCodigo(codigo);
        s.setFechaInicio(LocalDate.of(2026, 8, 3));
        s.setFechaFin(LocalDate.of(2026, 12, 12));
        return s;
    }

    private static Evaluacion evaluacion(Asignatura asignatura, Semestre semestre, LocalDate fechaTentativa) {
        ResultadoAprendizaje ra = new ResultadoAprendizaje();
        ReflectionTestUtils.setField(ra, "id", UUID.randomUUID());
        ra.setAsignatura(asignatura);
        ra.setCodigo("RA1");
        Evaluacion e = new Evaluacion();
        e.setId(UUID.randomUUID());
        e.setResultadoAprendizaje(ra);
        e.setSemestre(semestre);
        e.setNombre("Parcial 1");
        e.setFechaTentativa(fechaTentativa);
        e.setPorcentaje(new BigDecimal("30"));
        e.setDescripcion("Primer parcial");
        e.setEstado(EstadoEvaluacion.ACTIVA);
        return e;
    }

    private static Matricula matricula(String nombre, Asignatura asignatura, Semestre semestre, EstadoMatricula estado) {
        Estudiante estudiante = new Estudiante();
        estudiante.setId(UUID.randomUUID());
        estudiante.setNombreCompleto(nombre);
        estudiante.setNumeroDocumento("1001001001");
        Matricula m = new Matricula();
        m.setId(UUID.randomUUID());
        m.setEstudiante(estudiante);
        m.setAsignatura(asignatura);
        m.setSemestre(semestre);
        m.setEstado(estado);
        return m;
    }

    private Calificacion calificacionExistente(BigDecimal nota, boolean fueraDeFecha) {
        Calificacion c = new Calificacion();
        c.setId(UUID.randomUUID());
        c.setEvaluacion(evaluacion);
        c.setMatricula(matricula);
        c.setNota(nota);
        c.setObservacion(OBSERVACION);
        c.setFechaRegistro(AHORA.minusDays(3));
        c.setFueraDeFecha(fueraDeFecha);
        return c;
    }

    private void profesorAsignado() {
        when(asignacionDocenteRepository.existsByProfesor_IdAndAsignatura_IdAndEstado(
                ID_PROFESOR, asignatura.getId(), EstadoRegistro.ACTIVO)).thenReturn(true);
    }

    private CalificacionRequestDTO request(BigDecimal nota) {
        return new CalificacionRequestDTO(matricula.getId(), nota, OBSERVACION);
    }

    private void evaluacionYMatriculaExisten() {
        when(evaluacionRepository.findById(evaluacion.getId())).thenReturn(Optional.of(evaluacion));
        when(matriculaRepository.findById(matricula.getId())).thenReturn(Optional.of(matricula));
    }

    // ------------------------------------------------------------------ Registrar

    @Nested
    @DisplayName("Registrar (RF-12)")
    class Registrar {

        @Test
        @DisplayName("En la fecha tentativa registra sin marca de fuera de fecha y deja el evento REGISTRO")
        void registraATiempo() {
            profesorAsignado();
            evaluacionYMatriculaExisten();
            when(calificacionRepository.saveAndFlush(any(Calificacion.class))).thenAnswer(inv -> {
                Calificacion c = inv.getArgument(0);
                c.setId(UUID.randomUUID());
                return c;
            });

            CalificacionResponseDTO respuesta = service.registrar(evaluacion.getId(),
                    request(new BigDecimal("4.5")), PROFESOR);

            assertThat(respuesta.nota()).isEqualByComparingTo("4.5");
            assertThat(respuesta.fueraDeFecha()).isFalse();
            assertThat(respuesta.fechaRegistro()).isEqualTo(AHORA);
            assertThat(respuesta.estudianteNombre()).isEqualTo("Ana Gómez");

            ArgumentCaptor<HistorialCalificacion> evento = ArgumentCaptor.forClass(HistorialCalificacion.class);
            verify(historialRepository).save(evento.capture());
            assertThat(evento.getValue().getTipoEvento()).isEqualTo(TipoEventoCalificacion.REGISTRO);
            assertThat(evento.getValue().getNotaAnterior()).isNull();
            assertThat(evento.getValue().getNotaNueva()).isEqualByComparingTo("4.5");
            assertThat(evento.getValue().getUsuarioId()).isEqualTo(ID_PROFESOR);
            assertThat(evento.getValue().getRolUsuario()).isEqualTo(RolUsuario.PROFESOR);
        }

        @Test
        @DisplayName("Después de la fecha tentativa queda marcada fueraDeFecha, también en el historial")
        void registraTarde() {
            evaluacion.setFechaTentativa(HOY.minusDays(1));
            profesorAsignado();
            evaluacionYMatriculaExisten();
            when(calificacionRepository.saveAndFlush(any(Calificacion.class))).thenAnswer(inv -> inv.getArgument(0));

            CalificacionResponseDTO respuesta = service.registrar(evaluacion.getId(),
                    request(new BigDecimal("3.0")), PROFESOR);

            assertThat(respuesta.fueraDeFecha()).isTrue();
            ArgumentCaptor<HistorialCalificacion> evento = ArgumentCaptor.forClass(HistorialCalificacion.class);
            verify(historialRepository).save(evento.capture());
            assertThat(evento.getValue().isFueraDeFecha()).isTrue();
        }

        @Test
        @DisplayName("Evaluación inexistente: EvaluacionNoEncontradaException")
        void evaluacionInexistente() {
            UUID id = UUID.randomUUID();
            when(evaluacionRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registrar(id, request(new BigDecimal("4.0")), PROFESOR))
                    .isInstanceOf(EvaluacionNoEncontradaException.class);
            verifyNoInteractions(calificacionRepository, historialRepository);
        }

        @Test
        @DisplayName("Evaluación inactiva: no admite calificaciones")
        void evaluacionInactiva() {
            evaluacion.setEstado(EstadoEvaluacion.INACTIVA);
            profesorAsignado();
            when(evaluacionRepository.findById(evaluacion.getId())).thenReturn(Optional.of(evaluacion));

            assertThatThrownBy(() -> service.registrar(evaluacion.getId(), request(new BigDecimal("4.0")), PROFESOR))
                    .isInstanceOf(CalificacionReglaNegocioException.class)
                    .hasMessageContaining("inactiva");
            verifyNoInteractions(calificacionRepository);
        }

        @Test
        @DisplayName("Matrícula inexistente: MatriculaNoEncontradaException")
        void matriculaInexistente() {
            profesorAsignado();
            when(evaluacionRepository.findById(evaluacion.getId())).thenReturn(Optional.of(evaluacion));
            when(matriculaRepository.findById(matricula.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registrar(evaluacion.getId(), request(new BigDecimal("4.0")), PROFESOR))
                    .isInstanceOf(MatriculaNoEncontradaException.class);
        }

        @Test
        @DisplayName("Matrícula desvinculada: se rechaza")
        void matriculaNoActiva() {
            matricula.setEstado(EstadoMatricula.DESVINCULADA);
            profesorAsignado();
            evaluacionYMatriculaExisten();

            assertThatThrownBy(() -> service.registrar(evaluacion.getId(), request(new BigDecimal("4.0")), PROFESOR))
                    .isInstanceOf(CalificacionReglaNegocioException.class)
                    .hasMessageContaining("no está activa");
            verify(calificacionRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Semestre de la matrícula distinto al de la evaluación: se rechaza")
        void semestreDistinto() {
            matricula.setSemestre(semestre("2026-1"));
            profesorAsignado();
            evaluacionYMatriculaExisten();

            assertThatThrownBy(() -> service.registrar(evaluacion.getId(), request(new BigDecimal("4.0")), PROFESOR))
                    .isInstanceOf(CalificacionReglaNegocioException.class)
                    .hasMessageContaining("semestre");
        }

        @Test
        @DisplayName("Matrícula de otra asignatura: se rechaza")
        void otraAsignatura() {
            matricula.setAsignatura(asignatura("ASG-999"));
            profesorAsignado();
            evaluacionYMatriculaExisten();

            assertThatThrownBy(() -> service.registrar(evaluacion.getId(), request(new BigDecimal("4.0")), PROFESOR))
                    .isInstanceOf(CalificacionReglaNegocioException.class)
                    .hasMessageContaining("asignatura");
        }

        @Test
        @DisplayName("Ya existe nota para (evaluación, estudiante): CalificacionDuplicadaException")
        void duplicada() {
            profesorAsignado();
            evaluacionYMatriculaExisten();
            when(calificacionRepository.existsByEvaluacion_IdAndMatricula_Id(evaluacion.getId(), matricula.getId()))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.registrar(evaluacion.getId(), request(new BigDecimal("4.0")), PROFESOR))
                    .isInstanceOf(CalificacionDuplicadaException.class);
            verify(calificacionRepository, never()).saveAndFlush(any());
            verifyNoInteractions(historialRepository);
        }

        @Test
        @DisplayName("Profesor sin asignación activa en la asignatura: 403")
        void profesorAjeno() {
            when(evaluacionRepository.findById(evaluacion.getId())).thenReturn(Optional.of(evaluacion));
            when(asignacionDocenteRepository.existsByProfesor_IdAndAsignatura_IdAndEstado(
                    ID_PROFESOR, asignatura.getId(), EstadoRegistro.ACTIVO)).thenReturn(false);

            assertThatThrownBy(() -> service.registrar(evaluacion.getId(), request(new BigDecimal("4.0")), PROFESOR))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(matriculaRepository, calificacionRepository);
        }

        @Test
        @DisplayName("Un rol distinto de PROFESOR no registra notas: 403")
        void otroRol() {
            when(evaluacionRepository.findById(evaluacion.getId())).thenReturn(Optional.of(evaluacion));
            UsuarioPrincipal administrador = new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ADMINISTRADOR);

            assertThatThrownBy(() -> service.registrar(evaluacion.getId(), request(new BigDecimal("4.0")), administrador))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(asignacionDocenteRepository);
        }
    }

    // ------------------------------------------------------------------ Modificar

    @Nested
    @DisplayName("Modificar (RF-13)")
    class Modificar {

        @Test
        @DisplayName("Cambia nota y observación y deja el antes y el después en el historial")
        void modificaYAudita() {
            Calificacion existente = calificacionExistente(new BigDecimal("3.5"), false);
            profesorAsignado();
            when(calificacionRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
            when(calificacionRepository.save(any(Calificacion.class))).thenAnswer(inv -> inv.getArgument(0));

            CalificacionResponseDTO respuesta = service.modificar(existente.getId(),
                    new CalificacionModificarRequestDTO(new BigDecimal("4.0"), "Ajuste tras la revisión"), PROFESOR);

            assertThat(respuesta.nota()).isEqualByComparingTo("4.0");
            assertThat(respuesta.observacion()).isEqualTo("Ajuste tras la revisión");
            assertThat(respuesta.fechaModificacion()).isEqualTo(AHORA);

            ArgumentCaptor<HistorialCalificacion> evento = ArgumentCaptor.forClass(HistorialCalificacion.class);
            verify(historialRepository).save(evento.capture());
            HistorialCalificacion h = evento.getValue();
            assertThat(h.getTipoEvento()).isEqualTo(TipoEventoCalificacion.MODIFICACION);
            assertThat(h.getCalificacionId()).isEqualTo(existente.getId());
            assertThat(h.getNotaAnterior()).isEqualByComparingTo("3.5");
            assertThat(h.getNotaNueva()).isEqualByComparingTo("4.0");
            assertThat(h.getObservacionAnterior()).isEqualTo(OBSERVACION);
            assertThat(h.getObservacionNueva()).isEqualTo("Ajuste tras la revisión");
            assertThat(h.getFechaEvento()).isEqualTo(AHORA);
        }

        @Test
        @DisplayName("Sin cambios (3.5 = 3.50 y misma observación) no guarda ni audita")
        void sinCambios() {
            Calificacion existente = calificacionExistente(new BigDecimal("3.5"), false);
            profesorAsignado();
            when(calificacionRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

            service.modificar(existente.getId(),
                    new CalificacionModificarRequestDTO(new BigDecimal("3.50"), OBSERVACION), PROFESOR);

            verify(calificacionRepository, never()).save(any());
            verifyNoInteractions(historialRepository);
        }

        @Test
        @DisplayName("Modificar después de la fecha tentativa marca fueraDeFecha")
        void modificaTarde() {
            evaluacion.setFechaTentativa(HOY.minusDays(2));
            Calificacion existente = calificacionExistente(new BigDecimal("3.5"), false);
            profesorAsignado();
            when(calificacionRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
            when(calificacionRepository.save(any(Calificacion.class))).thenAnswer(inv -> inv.getArgument(0));

            CalificacionResponseDTO respuesta = service.modificar(existente.getId(),
                    new CalificacionModificarRequestDTO(new BigDecimal("2.0"), OBSERVACION), PROFESOR);

            assertThat(respuesta.fueraDeFecha()).isTrue();
        }

        @Test
        @DisplayName("Una marca de fuera de fecha no se borra al modificar a tiempo")
        void marcaNoSeBorra() {
            Calificacion existente = calificacionExistente(new BigDecimal("3.5"), true);
            profesorAsignado();
            when(calificacionRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
            when(calificacionRepository.save(any(Calificacion.class))).thenAnswer(inv -> inv.getArgument(0));

            CalificacionResponseDTO respuesta = service.modificar(existente.getId(),
                    new CalificacionModificarRequestDTO(new BigDecimal("4.0"), OBSERVACION), PROFESOR);

            assertThat(respuesta.fueraDeFecha()).isTrue();
        }

        @Test
        @DisplayName("Calificación inexistente: CalificacionNoEncontradaException")
        void inexistente() {
            UUID id = UUID.randomUUID();
            when(calificacionRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.modificar(id,
                    new CalificacionModificarRequestDTO(new BigDecimal("4.0"), OBSERVACION), PROFESOR))
                    .isInstanceOf(CalificacionNoEncontradaException.class);
        }

        @Test
        @DisplayName("Matrícula desvinculada: la nota se conserva pero no se modifica")
        void matriculaDesvinculada() {
            matricula.setEstado(EstadoMatricula.DESVINCULADA);
            Calificacion existente = calificacionExistente(new BigDecimal("3.5"), false);
            profesorAsignado();
            when(calificacionRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.modificar(existente.getId(),
                    new CalificacionModificarRequestDTO(new BigDecimal("4.0"), OBSERVACION), PROFESOR))
                    .isInstanceOf(CalificacionReglaNegocioException.class);
            verifyNoInteractions(historialRepository);
        }

        @Test
        @DisplayName("Evaluación inactiva: no se modifican sus notas")
        void evaluacionInactiva() {
            evaluacion.setEstado(EstadoEvaluacion.INACTIVA);
            Calificacion existente = calificacionExistente(new BigDecimal("3.5"), false);
            profesorAsignado();
            when(calificacionRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.modificar(existente.getId(),
                    new CalificacionModificarRequestDTO(new BigDecimal("4.0"), OBSERVACION), PROFESOR))
                    .isInstanceOf(CalificacionReglaNegocioException.class)
                    .hasMessageContaining("inactiva");
        }
    }

    // ------------------------------------------------------------------ Planilla e historial

    @Nested
    @DisplayName("Planilla e historial")
    class Consultas {

        @Test
        @DisplayName("Planilla: matrículas activas y desvinculadas con nota, ordenadas por nombre")
        void planilla() {
            Matricula zoe = matricula("Zoe Pérez", asignatura, semestre, EstadoMatricula.ACTIVA);
            Matricula retiradaConNota = matricula("Carlos Ruiz", asignatura, semestre, EstadoMatricula.DESVINCULADA);
            Matricula retiradaSinNota = matricula("Beto Díaz", asignatura, semestre, EstadoMatricula.DESVINCULADA);
            Calificacion notaZoe = calificacionExistente(new BigDecimal("4.2"), false);
            notaZoe.setMatricula(zoe);
            Calificacion notaCarlos = calificacionExistente(new BigDecimal("2.0"), true);
            notaCarlos.setMatricula(retiradaConNota);

            profesorAsignado();
            when(evaluacionRepository.findById(evaluacion.getId())).thenReturn(Optional.of(evaluacion));
            when(calificacionRepository.buscarPorEvaluacion(evaluacion.getId())).thenReturn(List.of(notaZoe, notaCarlos));
            when(matriculaRepository.buscarPorAsignaturaYSemestre(asignatura.getId(), semestre.getId(), null))
                    .thenReturn(List.of(zoe, matricula, retiradaConNota, retiradaSinNota));

            PlanillaCalificacionesDTO planilla = service.consultarPlanilla(evaluacion.getId(), PROFESOR);

            assertThat(planilla.filas()).extracting(FilaPlanillaDTO::nombreCompleto)
                    .containsExactly("Ana Gómez", "Carlos Ruiz", "Zoe Pérez");
            assertThat(planilla.totalEstudiantes()).isEqualTo(3);
            assertThat(planilla.totalCalificados()).isEqualTo(2);
            assertThat(planilla.filas().get(0).calificacionId()).isNull();
            assertThat(planilla.filas().get(1).matriculaActiva()).isFalse();
            assertThat(planilla.filas().get(1).fueraDeFecha()).isTrue();
            assertThat(planilla.semestreCodigo()).isEqualTo("2026-2");
            assertThat(planilla.resultadoAprendizajeCodigo()).isEqualTo("RA1");
        }

        @Test
        @DisplayName("Lista las evaluaciones del RA en el semestre para el selector de la planilla")
        void listarEvaluaciones() {
            UUID raId = evaluacion.getResultadoAprendizaje().getId();
            when(evaluacionRepository.findByResultadoAprendizaje_IdAndSemestre_IdOrderByFechaTentativaAscNombreAsc(
                    raId, semestre.getId())).thenReturn(List.of(evaluacion));

            List<EvaluacionCalificableDTO> lista = service.listarEvaluaciones(raId, semestre.getId());

            assertThat(lista).hasSize(1);
            assertThat(lista.get(0).nombre()).isEqualTo("Parcial 1");
            assertThat(lista.get(0).estado()).isEqualTo("ACTIVA");
        }

        @Test
        @DisplayName("Historial: devuelve los eventos en orden y valida el dueño")
        void historial() {
            Calificacion existente = calificacionExistente(new BigDecimal("4.0"), false);
            HistorialCalificacion registro = HistorialCalificacion.registro(existente, ID_PROFESOR,
                    RolUsuario.PROFESOR, false, AHORA.minusDays(3));
            profesorAsignado();
            when(calificacionRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
            when(historialRepository.findByCalificacionIdOrderByFechaEventoAsc(existente.getId()))
                    .thenReturn(List.of(registro));

            List<HistorialCalificacionDTO> historial = service.consultarHistorial(existente.getId(), PROFESOR);

            assertThat(historial).hasSize(1);
            assertThat(historial.get(0).tipoEvento()).isEqualTo("REGISTRO");
            assertThat(historial.get(0).rolUsuario()).isEqualTo("PROFESOR");
        }
    }
}