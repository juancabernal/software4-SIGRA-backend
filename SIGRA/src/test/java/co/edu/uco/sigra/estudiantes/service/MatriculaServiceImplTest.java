package co.edu.uco.sigra.estudiantes.service;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
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
import co.edu.uco.sigra.estudiantes.service.MatriculaService.ResultadoMatricula;
import co.edu.uco.sigra.estudiantes.service.impl.MatriculaServiceImpl;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de matrículas")
class MatriculaServiceImplTest {

    private static final UUID ID_ESTUDIANTE = UUID.randomUUID();
    private static final UUID ID_ASIGNATURA = UUID.randomUUID();
    private static final UUID ID_SEMESTRE = UUID.randomUUID();
    private static final UUID ID_MATRICULA = UUID.randomUUID();
    private static final String CODIGO_ASIGNATURA = "ASIG-001";
    private static final String NOMBRE_ASIGNATURA = "Cálculo I";
    private static final String CODIGO_SEMESTRE = "2024-1";
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.of(2024, 3, 15);
    private static final LocalDate OTRO_DIA = LocalDate.of(2024, 4, 2);

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private AsignaturaRepository asignaturaRepository;

    @Mock
    private SemestreRepository semestreRepository;

    @Mock
    private CalificacionesDeMatricula calificacionesDeMatricula;

    private RelojAjustable reloj;

    private MatriculaServiceImpl service;

    @BeforeEach
    void setUp() {
        reloj = new RelojAjustable(HOY);
        service = new MatriculaServiceImpl(matriculaRepository, estudianteRepository, asignaturaRepository, semestreRepository, reloj, calificacionesDeMatricula);
    }

    private static Estudiante estudiante(EstadoRegistro estado) {
        Estudiante e = new Estudiante();
        ReflectionTestUtils.setField(e, "id", ID_ESTUDIANTE);
        e.setEstado(estado);
        return e;
    }

    private static Asignatura asignatura(EstadoAsignatura estado) {
        Asignatura a = new Asignatura(CODIGO_ASIGNATURA, NOMBRE_ASIGNATURA, null);
        ReflectionTestUtils.setField(a, "id", ID_ASIGNATURA);
        ReflectionTestUtils.setField(a, "estado", estado);
        return a;
    }

    private static Semestre semestre(EstadoSemestre estado) {
        Semestre s = new Semestre();
        s.setId(ID_SEMESTRE);
        s.setCodigo(CODIGO_SEMESTRE);
        s.setFechaInicio(HOY.minusMonths(1));
        s.setFechaFin(HOY.plusMonths(1));
        s.setEstado(estado);
        return s;
    }

    /**
     * Semestre con fechas explícitas e independientes de su marca de estado almacenada.
     * <p>
     * La vigencia se juzga por {@code fechaInicio}/{@code fechaFin}, no por {@code estado}
     * (design.md, D3), así que las pruebas que cubren esa regla necesitan poder fijar fechas
     * vencidas o futuras mientras la marca almacenada sigue diciendo {@code ACTIVO} — justo el
     * escenario del defecto que esta regla corrige.
     */
    private static Semestre semestreConFechas(LocalDate fechaInicio, LocalDate fechaFin, EstadoSemestre estado) {
        Semestre s = new Semestre();
        s.setId(ID_SEMESTRE);
        s.setCodigo(CODIGO_SEMESTRE);
        s.setFechaInicio(fechaInicio);
        s.setFechaFin(fechaFin);
        s.setEstado(estado);
        return s;
    }

    private static Matricula matricula(UUID id, Estudiante estudiante, Asignatura asignatura, Semestre semestre, EstadoMatricula estado) {
        Matricula m = new Matricula();
        m.setId(id);
        m.setEstudiante(estudiante);
        m.setAsignatura(asignatura);
        m.setSemestre(semestre);
        m.setFechaMatricula(HOY);
        m.setEstado(estado);
        return m;
    }

    private void saveDevuelveArgumento() {
        when(matriculaRepository.save(any(Matricula.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("Matricular")
    class Matricular {

        private void stubTresActivos() {
            when(estudianteRepository.findById(ID_ESTUDIANTE)).thenReturn(Optional.of(estudiante(EstadoRegistro.ACTIVO)));
            when(asignaturaRepository.findById(ID_ASIGNATURA)).thenReturn(Optional.of(asignatura(EstadoAsignatura.ACTIVA)));
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestre(EstadoSemestre.ACTIVO)));
        }

        @Test
        @DisplayName("PN-35 Con terna nueva y los tres activos, crea la matrícula ACTIVA sin reactivarla")
        void caminoFeliz() {
            stubTresActivos();
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE)).thenReturn(Optional.empty());
            saveDevuelveArgumento();

            ResultadoMatricula resultado = service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE);

            assertThat(resultado.reactivada()).isFalse();

            ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
            verify(matriculaRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoMatricula.ACTIVA);
            assertThat(captor.getValue().getEstudiante().getId()).isEqualTo(ID_ESTUDIANTE);
            assertThat(captor.getValue().getAsignatura().getId()).isEqualTo(ID_ASIGNATURA);
            assertThat(captor.getValue().getSemestre().getId()).isEqualTo(ID_SEMESTRE);
        }

        @Test
        @DisplayName("Con un estudiante inexistente lanza excepción sin consultar asignatura, semestre ni la terna")
        void estudianteInexistente() {
            when(estudianteRepository.findById(ID_ESTUDIANTE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .isInstanceOf(EstudianteNoEncontradoException.class)
                    .hasMessageContaining(ID_ESTUDIANTE.toString());

            verify(asignaturaRepository, never()).findById(any());
            verify(semestreRepository, never()).findById(any());
            verify(matriculaRepository, never()).buscarPorTerna(any(), any(), any());
        }

        @Test
        @DisplayName("Con una asignatura inexistente lanza excepción sin consultar el semestre ni la terna")
        void asignaturaInexistente() {
            when(estudianteRepository.findById(ID_ESTUDIANTE)).thenReturn(Optional.of(estudiante(EstadoRegistro.ACTIVO)));
            when(asignaturaRepository.findById(ID_ASIGNATURA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .isInstanceOf(ReferenciaNoEncontradaException.class)
                    .hasMessageContaining(ID_ASIGNATURA.toString());

            verify(semestreRepository, never()).findById(any());
            verify(matriculaRepository, never()).buscarPorTerna(any(), any(), any());
        }

        @Test
        @DisplayName("Con un semestre inexistente lanza excepción sin consultar la terna")
        void semestreInexistente() {
            when(estudianteRepository.findById(ID_ESTUDIANTE)).thenReturn(Optional.of(estudiante(EstadoRegistro.ACTIVO)));
            when(asignaturaRepository.findById(ID_ASIGNATURA)).thenReturn(Optional.of(asignatura(EstadoAsignatura.ACTIVA)));
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .isInstanceOf(ReferenciaNoEncontradaException.class)
                    .hasMessageContaining(ID_SEMESTRE.toString());

            verify(matriculaRepository, never()).buscarPorTerna(any(), any(), any());
        }

        @Test
        @DisplayName("Con el estudiante INACTIVO lanza excepción sin consultar la terna")
        void estudianteInactivo() {
            stubTresActivos();
            when(estudianteRepository.findById(ID_ESTUDIANTE)).thenReturn(Optional.of(estudiante(EstadoRegistro.INACTIVO)));

            assertThatThrownBy(() -> service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .isInstanceOf(ReglaDeMatriculaException.class)
                    .hasMessageContaining(ID_ESTUDIANTE.toString());

            verify(matriculaRepository, never()).buscarPorTerna(any(), any(), any());
        }

        @Test
        @DisplayName("Con la asignatura en BORRADOR lanza excepción sin consultar la terna")
        void asignaturaEnBorrador() {
            stubTresActivos();
            when(asignaturaRepository.findById(ID_ASIGNATURA)).thenReturn(Optional.of(asignatura(EstadoAsignatura.BORRADOR)));

            assertThatThrownBy(() -> service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .isInstanceOf(ReglaDeMatriculaException.class)
                    .hasMessageContaining(ID_ASIGNATURA.toString());

            verify(matriculaRepository, never()).buscarPorTerna(any(), any(), any());
        }

        @Test
        @DisplayName("PN-35 Con la asignatura INACTIVA lanza excepción sin consultar la terna")
        void asignaturaInactiva() {
            stubTresActivos();
            when(asignaturaRepository.findById(ID_ASIGNATURA)).thenReturn(Optional.of(asignatura(EstadoAsignatura.INACTIVA)));

            assertThatThrownBy(() -> service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .isInstanceOf(ReglaDeMatriculaException.class)
                    .hasMessageContaining(ID_ASIGNATURA.toString());

            verify(matriculaRepository, never()).buscarPorTerna(any(), any(), any());
        }

        @Test
        @DisplayName("PN-35 Con el semestre cuya fecha de fin ya pasó lanza excepción aunque la marca almacenada diga ACTIVO")
        void semestreConFechaFinVencidaAunqueLaMarcaDigaActivo() {
            stubTresActivos();
            // El punto central del defecto corregido (design.md, D3): la marca de estado
            // almacenada sigue diciendo ACTIVO porque EstadoSemestre se recalcula de forma
            // perezosa, pero las fechas dicen que el semestre ya terminó. Debe rechazarse
            // igual, consultando solo las fechas.
            Semestre vencido = semestreConFechas(HOY.minusMonths(2), HOY.minusDays(1), EstadoSemestre.ACTIVO);
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(vencido));

            assertThatThrownBy(() -> service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .isInstanceOf(ReglaDeMatriculaException.class)
                    .hasMessageContaining(ID_SEMESTRE.toString())
                    .hasMessageContaining("no está vigente");

            verify(matriculaRepository, never()).buscarPorTerna(any(), any(), any());
            verify(matriculaRepository, never()).save(any());
        }

        @Test
        @DisplayName("PN-35 Con el semestre cuya fecha de inicio es posterior a hoy lanza excepción")
        void semestreConFechaDeInicioPosteriorAHoy() {
            stubTresActivos();
            Semestre futuro = semestreConFechas(HOY.plusDays(1), HOY.plusMonths(2), EstadoSemestre.ACTIVO);
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(futuro));

            assertThatThrownBy(() -> service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .isInstanceOf(ReglaDeMatriculaException.class)
                    .hasMessageContaining(ID_SEMESTRE.toString())
                    .hasMessageContaining("no está vigente");

            verify(matriculaRepository, never()).buscarPorTerna(any(), any(), any());
            verify(matriculaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con la fecha de inicio del semestre igual a hoy sí permite matricular")
        void semestreConFechaDeInicioIgualAHoyPermiteMatricular() {
            stubTresActivos();
            Semestre semestreEnSuPrimerDia = semestreConFechas(HOY, HOY.plusMonths(1), EstadoSemestre.ACTIVO);
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestreEnSuPrimerDia));
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE)).thenReturn(Optional.empty());
            saveDevuelveArgumento();

            ResultadoMatricula resultado = service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE);

            assertThat(resultado.matricula().getEstado()).isEqualTo(EstadoMatricula.ACTIVA);
            verify(matriculaRepository).save(any(Matricula.class));
        }

        @Test
        @DisplayName("Con la fecha de fin del semestre igual a hoy sí permite matricular")
        void semestreConFechaDeFinIgualAHoyPermiteMatricular() {
            stubTresActivos();
            Semestre semestreEnSuUltimoDia = semestreConFechas(HOY.minusMonths(1), HOY, EstadoSemestre.ACTIVO);
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestreEnSuUltimoDia));
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE)).thenReturn(Optional.empty());
            saveDevuelveArgumento();

            ResultadoMatricula resultado = service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE);

            assertThat(resultado.matricula().getEstado()).isEqualTo(EstadoMatricula.ACTIVA);
            verify(matriculaRepository).save(any(Matricula.class));
        }

        @Test
        @DisplayName("PN-35 Con la terna ya matriculada y ACTIVA lanza excepción y no guarda")
        void ternaConMatriculaActivaExistente() {
            stubTresActivos();
            Matricula existente = matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoMatricula.ACTIVA);
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .isInstanceOf(MatriculaDuplicadaException.class)
                    .hasMessageContaining(ID_ESTUDIANTE.toString())
                    .hasMessageContaining(ID_ASIGNATURA.toString())
                    .hasMessageContaining(ID_SEMESTRE.toString());

            verify(matriculaRepository, never()).save(any());
        }

        @Test
        @DisplayName("PN-35 Con el mismo estudiante y asignatura ya matriculados en otro semestre, crea una matrícula nueva e independiente")
        void repitenciaEnOtroSemestreCreaMatriculaIndependiente() {
            UUID idSemestreOtro = UUID.randomUUID();

            when(estudianteRepository.findById(ID_ESTUDIANTE)).thenReturn(Optional.of(estudiante(EstadoRegistro.ACTIVO)));
            when(asignaturaRepository.findById(ID_ASIGNATURA)).thenReturn(Optional.of(asignatura(EstadoAsignatura.ACTIVA)));

            Semestre semestreOtro = semestre(EstadoSemestre.ACTIVO);
            semestreOtro.setId(idSemestreOtro);
            when(semestreRepository.findById(idSemestreOtro)).thenReturn(Optional.of(semestreOtro));

            // El estudiante ya tiene una matrícula ACTIVA de la misma asignatura, pero en OTRO
            // semestre: la terna (estudiante, asignatura, semestre) es distinta, así que no debe
            // reactivarse ni fusionarse con ella.
            Matricula matriculaDelOtroSemestre = matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoMatricula.ACTIVA);
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, idSemestreOtro)).thenReturn(Optional.empty());
            saveDevuelveArgumento();

            ResultadoMatricula resultado = service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, idSemestreOtro);

            assertThat(resultado.reactivada()).isFalse();
            assertThat(resultado.matricula()).isNotSameAs(matriculaDelOtroSemestre);
            assertThat(resultado.matricula().getSemestre().getId()).isEqualTo(idSemestreOtro);
            assertThat(resultado.matricula().getEstado()).isEqualTo(EstadoMatricula.ACTIVA);

            ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
            verify(matriculaRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue()).isNotSameAs(matriculaDelOtroSemestre);
            assertThat(captor.getValue().getId()).isNotEqualTo(ID_MATRICULA);
        }

        @Test
        @DisplayName("Con la terna retirada (DESVINCULADA) existente, reactiva la misma matrícula en lugar de crear una nueva")
        void ternaConMatriculaInactivaExistenteSeReactiva() {
            stubTresActivos();
            Matricula existente = matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoMatricula.DESVINCULADA);
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE)).thenReturn(Optional.of(existente));
            when(matriculaRepository.save(any(Matricula.class))).thenAnswer(inv -> inv.getArgument(0));

            ResultadoMatricula resultado = service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE);

            assertThat(resultado.reactivada()).isTrue();
            assertThat(resultado.matricula().getId()).isEqualTo(ID_MATRICULA);
            assertThat(resultado.matricula().getEstado()).isEqualTo(EstadoMatricula.ACTIVA);

            ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
            verify(matriculaRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existente);
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoMatricula.ACTIVA);
        }

        @Test
        @DisplayName("Ninguna de las rutas de matricular invoca métodos de borrado en ningún repositorio")
        void noInvocaBorrado() {
            stubTresActivos();
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE)).thenReturn(Optional.empty());
            saveDevuelveArgumento();

            service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE);

            verify(matriculaRepository, never()).delete(any());
            verify(matriculaRepository, never()).deleteById(any());
            verify(matriculaRepository, never()).deleteAll();
            verify(estudianteRepository, never()).delete(any());
            verify(asignaturaRepository, never()).delete(any());
            verify(semestreRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Al crear la matrícula le queda la fecha del reloj, no una fecha del cliente")
        void fechaDeCreacionLaPoneElReloj() {
            stubTresActivos();
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE)).thenReturn(Optional.empty());
            saveDevuelveArgumento();

            ResultadoMatricula resultado = service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE);

            assertThat(resultado.matricula().getFechaMatricula()).isEqualTo(HOY);

            ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
            verify(matriculaRepository).save(captor.capture());
            assertThat(captor.getValue().getFechaMatricula()).isEqualTo(HOY);
        }

        @Test
        @DisplayName("Al reactivar una matrícula retirada, su fecha pasa a ser la del día de la rematrícula")
        void reactivacionActualizaLaFecha() {
            stubTresActivos();
            Matricula existente = matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoMatricula.DESVINCULADA);
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE)).thenReturn(Optional.of(existente));
            saveDevuelveArgumento();

            // La matrícula original quedó con la fecha HOY; entre el retiro y la rematrícula pasan días.
            assertThat(existente.getFechaMatricula()).isEqualTo(HOY);
            reloj.moverA(OTRO_DIA);

            ResultadoMatricula resultado = service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE);

            assertThat(resultado.matricula().getFechaMatricula()).isEqualTo(OTRO_DIA);

            ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
            verify(matriculaRepository).save(captor.capture());
            assertThat(captor.getValue().getFechaMatricula())
                    .isEqualTo(OTRO_DIA)
                    .isNotEqualTo(HOY);
        }

        @Test
        @DisplayName("La matrícula reactivada conserva su identificador original aunque su fecha cambie")
        void reactivacionConservaElIdentificador() {
            stubTresActivos();
            Matricula existente = matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoMatricula.DESVINCULADA);
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE)).thenReturn(Optional.of(existente));
            saveDevuelveArgumento();

            reloj.moverA(OTRO_DIA);

            ResultadoMatricula resultado = service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE);

            assertThat(resultado.matricula().getId()).isEqualTo(ID_MATRICULA);

            ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
            verify(matriculaRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(ID_MATRICULA);
            assertThat(captor.getValue()).isSameAs(existente);
        }
    }

    @Nested
    @DisplayName("Desvincular")
    class Desvincular {

        @Test
        @DisplayName("Con una matrícula ACTIVA la pasa a DESVINCULADA")
        void matriculaActiva() {
            Matricula existente = matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoMatricula.ACTIVA);
            when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existente));
            saveDevuelveArgumento();

            service.desvincular(ID_MATRICULA, false);

            ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
            verify(matriculaRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(ID_MATRICULA);
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoMatricula.DESVINCULADA);

            verify(matriculaRepository, never()).delete(any());
            verify(matriculaRepository, never()).deleteById(any());
            verify(matriculaRepository, never()).deleteAll();
        }

        @Test
        @DisplayName("PN-36 Con una matrícula inexistente lanza excepción sin consultar las calificaciones")
        void matriculaInexistente() {
            when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.desvincular(ID_MATRICULA, true))
                    .isInstanceOf(MatriculaNoEncontradaException.class)
                    .hasMessageContaining(ID_MATRICULA.toString());

            verify(matriculaRepository, never()).save(any());
            verifyNoInteractions(calificacionesDeMatricula);
        }

        @Test
        @DisplayName("PN-36 Con una matrícula ya DESVINCULADA lanza excepción y no guarda ni consulta las calificaciones")
        void matriculaYaInactiva() {
            Matricula existente = matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoMatricula.DESVINCULADA);
            when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.desvincular(ID_MATRICULA, true))
                    .isInstanceOf(MatriculaYaInactivaException.class)
                    .hasMessageContaining(ID_MATRICULA.toString());

            verify(matriculaRepository, never()).save(any());
            verifyNoInteractions(calificacionesDeMatricula);
        }

        @Nested
        @DisplayName("PN-36 Confirmación adicional para desvincular con calificaciones registradas")
        class ConCalificaciones {

            private Matricula existenteActiva() {
                return matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                        asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoMatricula.ACTIVA);
            }

            @Test
            @DisplayName("PN-36 Con 1 calificación registrada y sin confirmar, rechaza indicando la cantidad y no guarda")
            void unaCalificacionSinConfirmarRechaza() {
                Matricula existente = existenteActiva();
                when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existente));
                when(calificacionesDeMatricula.contarPorMatricula(ID_MATRICULA)).thenReturn(1L);

                assertThatThrownBy(() -> service.desvincular(ID_MATRICULA, false))
                        .isInstanceOf(ConfirmacionRequeridaException.class)
                        .hasMessageContaining(ID_MATRICULA.toString())
                        .hasMessageContaining("1");

                verify(matriculaRepository, never()).save(any());
            }

            @Test
            @DisplayName("PN-36 Con varias calificaciones registradas y sin confirmar, el mensaje usa la cantidad real, no un literal fijo")
            void variasCalificacionesSinConfirmarRechazaConLaCantidadReal() {
                Matricula existente = existenteActiva();
                when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existente));
                when(calificacionesDeMatricula.contarPorMatricula(ID_MATRICULA)).thenReturn(5L);

                assertThatThrownBy(() -> service.desvincular(ID_MATRICULA, false))
                        .isInstanceOf(ConfirmacionRequeridaException.class)
                        .hasMessageContaining(ID_MATRICULA.toString())
                        .hasMessageContaining("5");

                verify(matriculaRepository, never()).save(any());
            }

            @Test
            @DisplayName("PN-36 Con calificaciones registradas y confirmando, desvincula la matrícula")
            void conCalificacionesYConfirmandoDesvincula() {
                Matricula existente = existenteActiva();
                when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existente));
                when(calificacionesDeMatricula.contarPorMatricula(ID_MATRICULA)).thenReturn(3L);
                saveDevuelveArgumento();

                service.desvincular(ID_MATRICULA, true);

                ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
                verify(matriculaRepository, times(1)).save(captor.capture());
                assertThat(captor.getValue().getEstado()).isEqualTo(EstadoMatricula.DESVINCULADA);
            }

            @Test
            @DisplayName("Sin calificaciones registradas y confirmando de todos modos, desvincula sin error")
            void sinCalificacionesConfirmandoDesvinculaIgual() {
                Matricula existente = existenteActiva();
                when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existente));
                when(calificacionesDeMatricula.contarPorMatricula(ID_MATRICULA)).thenReturn(0L);
                saveDevuelveArgumento();

                service.desvincular(ID_MATRICULA, true);

                verify(matriculaRepository, times(1)).save(any(Matricula.class));
            }

            @Test
            @DisplayName("PN-36 Con calificaciones registradas y confirmando, no invoca ningún método de borrado")
            void conCalificacionesYConfirmandoNoInvocaBorrado() {
                Matricula existente = existenteActiva();
                when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existente));
                when(calificacionesDeMatricula.contarPorMatricula(ID_MATRICULA)).thenReturn(2L);
                saveDevuelveArgumento();

                service.desvincular(ID_MATRICULA, true);

                verify(matriculaRepository, never()).delete(any());
                verify(matriculaRepository, never()).deleteById(any());
                verify(matriculaRepository, never()).deleteAll();
            }

            @Test
            @DisplayName("Tras desvincular con calificaciones y confirmando, el objeto guardado conserva el mismo id y la misma fecha de matrícula")
            void conCalificacionesYConfirmandoConservaIdentidadYFecha() {
                Matricula existente = existenteActiva();
                when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existente));
                when(calificacionesDeMatricula.contarPorMatricula(ID_MATRICULA)).thenReturn(4L);
                saveDevuelveArgumento();

                service.desvincular(ID_MATRICULA, true);

                ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
                verify(matriculaRepository, times(1)).save(captor.capture());
                assertThat(captor.getValue()).isSameAs(existente);
                assertThat(captor.getValue().getId()).isEqualTo(ID_MATRICULA);
                assertThat(captor.getValue().getFechaMatricula()).isEqualTo(HOY);
            }

            @Test
            @DisplayName("Consulta las calificaciones exactamente una vez, tanto al rechazar sin confirmar como al desvincular confirmando")
            void consultaLasCalificacionesExactamenteUnaVez() {
                Matricula existenteParaRechazo = existenteActiva();
                when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existenteParaRechazo));
                when(calificacionesDeMatricula.contarPorMatricula(ID_MATRICULA)).thenReturn(1L);

                assertThatThrownBy(() -> service.desvincular(ID_MATRICULA, false))
                        .isInstanceOf(ConfirmacionRequeridaException.class);

                verify(calificacionesDeMatricula, times(1)).contarPorMatricula(ID_MATRICULA);

                Matricula existenteParaConfirmar = existenteActiva();
                when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existenteParaConfirmar));
                saveDevuelveArgumento();

                service.desvincular(ID_MATRICULA, true);

                verify(calificacionesDeMatricula, times(2)).contarPorMatricula(ID_MATRICULA);
            }

            @Test
            @DisplayName("Sin confirmar, reintentar la desvinculación dos veces seguidas rechaza ambas veces por igual, sin memoria del intento anterior")
            void reintentarSinConfirmarRechazaLasDosVeces() {
                Matricula existente = existenteActiva();
                when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existente));
                when(calificacionesDeMatricula.contarPorMatricula(ID_MATRICULA)).thenReturn(1L);

                assertThatThrownBy(() -> service.desvincular(ID_MATRICULA, false))
                        .isInstanceOf(ConfirmacionRequeridaException.class);
                assertThatThrownBy(() -> service.desvincular(ID_MATRICULA, false))
                        .isInstanceOf(ConfirmacionRequeridaException.class);

                verify(matriculaRepository, never()).save(any());
            }
        }
    }

    @Nested
    @DisplayName("Listar matriculados")
    class ListarMatriculados {

        @Test
        @DisplayName("Con una asignatura inexistente lanza excepción sin consultar el semestre")
        void asignaturaInexistente() {
            when(asignaturaRepository.existsById(ID_ASIGNATURA)).thenReturn(false);

            assertThatThrownBy(() -> service.listarMatriculados(ID_ASIGNATURA, ID_SEMESTRE, false))
                    .isInstanceOf(ReferenciaNoEncontradaException.class)
                    .hasMessageContaining(ID_ASIGNATURA.toString());

            verify(semestreRepository, never()).existsById(any());
        }

        @Test
        @DisplayName("Con un semestre inexistente lanza excepción")
        void semestreInexistente() {
            when(asignaturaRepository.existsById(ID_ASIGNATURA)).thenReturn(true);
            when(semestreRepository.existsById(ID_SEMESTRE)).thenReturn(false);

            assertThatThrownBy(() -> service.listarMatriculados(ID_ASIGNATURA, ID_SEMESTRE, false))
                    .isInstanceOf(ReferenciaNoEncontradaException.class)
                    .hasMessageContaining(ID_SEMESTRE.toString());
        }

        @Test
        @DisplayName("Con incluirInactivas en false delega la consulta con el estado ACTIVA")
        void incluirInactivasFalse() {
            when(asignaturaRepository.existsById(ID_ASIGNATURA)).thenReturn(true);
            when(semestreRepository.existsById(ID_SEMESTRE)).thenReturn(true);
            when(matriculaRepository.buscarPorAsignaturaYSemestre(ID_ASIGNATURA, ID_SEMESTRE, EstadoMatricula.ACTIVA))
                    .thenReturn(List.of(matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                            asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoMatricula.ACTIVA)));

            List<Matricula> resultado = service.listarMatriculados(ID_ASIGNATURA, ID_SEMESTRE, false);

            assertThat(resultado).hasSize(1);
            verify(matriculaRepository).buscarPorAsignaturaYSemestre(ID_ASIGNATURA, ID_SEMESTRE, EstadoMatricula.ACTIVA);
        }

        @Test
        @DisplayName("Con incluirInactivas en true delega la consulta con estado nulo")
        void incluirInactivasTrue() {
            when(asignaturaRepository.existsById(ID_ASIGNATURA)).thenReturn(true);
            when(semestreRepository.existsById(ID_SEMESTRE)).thenReturn(true);
            when(matriculaRepository.buscarPorAsignaturaYSemestre(ID_ASIGNATURA, ID_SEMESTRE, null))
                    .thenReturn(List.of());

            service.listarMatriculados(ID_ASIGNATURA, ID_SEMESTRE, true);

            verify(matriculaRepository).buscarPorAsignaturaYSemestre(ID_ASIGNATURA, ID_SEMESTRE, null);
        }

        @Test
        @DisplayName("Sin matriculados devuelve lista vacía sin lanzar excepción")
        void sinMatriculados() {
            when(asignaturaRepository.existsById(ID_ASIGNATURA)).thenReturn(true);
            when(semestreRepository.existsById(ID_SEMESTRE)).thenReturn(true);
            when(matriculaRepository.buscarPorAsignaturaYSemestre(ID_ASIGNATURA, ID_SEMESTRE, EstadoMatricula.ACTIVA))
                    .thenReturn(List.of());

            List<Matricula> resultado = service.listarMatriculados(ID_ASIGNATURA, ID_SEMESTRE, false);

            assertThat(resultado).isEmpty();
        }
    }

    @Nested
    @DisplayName("Listar asignaturas de estudiante")
    class ListarAsignaturasDeEstudiante {

        @Test
        @DisplayName("Con un estudiante inexistente lanza excepción sin consultar el historial")
        void estudianteInexistente() {
            when(estudianteRepository.existsById(ID_ESTUDIANTE)).thenReturn(false);

            assertThatThrownBy(() -> service.listarAsignaturasDeEstudiante(ID_ESTUDIANTE))
                    .isInstanceOf(EstudianteNoEncontradoException.class)
                    .hasMessageContaining(ID_ESTUDIANTE.toString());

            verify(matriculaRepository, never()).buscarPorEstudiante(any());
        }

        @Test
        @DisplayName("Con historial de varias matrículas devuelve la lista tal cual la entrega el repositorio")
        void historialConVariasMatriculas() {
            Matricula m1 = matricula(UUID.randomUUID(), estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoMatricula.ACTIVA);
            Matricula m2 = matricula(UUID.randomUUID(), estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.INACTIVO), EstadoMatricula.DESVINCULADA);
            when(estudianteRepository.existsById(ID_ESTUDIANTE)).thenReturn(true);
            when(matriculaRepository.buscarPorEstudiante(ID_ESTUDIANTE)).thenReturn(List.of(m1, m2));

            List<Matricula> resultado = service.listarAsignaturasDeEstudiante(ID_ESTUDIANTE);

            assertThat(resultado).containsExactly(m1, m2);
        }

        @Test
        @DisplayName("Sin matrículas devuelve lista vacía")
        void sinMatriculas() {
            when(estudianteRepository.existsById(ID_ESTUDIANTE)).thenReturn(true);
            when(matriculaRepository.buscarPorEstudiante(ID_ESTUDIANTE)).thenReturn(List.of());

            List<Matricula> resultado = service.listarAsignaturasDeEstudiante(ID_ESTUDIANTE);

            assertThat(resultado).isEmpty();
        }
    }

    private static Clock relojFijoEn(LocalDate dia) {
        return Clock.fixed(dia.atStartOfDay(ZONA).toInstant(), ZONA);
    }

    /**
     * Reloj fijo que además se puede mover a otro día.
     * <p>
     * Delega en {@link Clock#fixed(Instant, ZoneId)}, así que dentro de una prueba el tiempo no
     * avanza por su cuenta y las fechas son deterministas. {@code moverA} lo repunta a otro día,
     * que es lo que exige el caso de la rematrícula: hace falta un instante para la matrícula
     * original y otro, en un día distinto, para la reactivación, sobre el mismo servicio.
     * <p>
     * Se recrea en cada {@code @BeforeEach}, así que no hay estado mutable compartido entre
     * pruebas ni dependencia del orden.
     */
    private static final class RelojAjustable extends Clock {

        private Clock delegado;

        private RelojAjustable(LocalDate dia) {
            this.delegado = relojFijoEn(dia);
        }

        private void moverA(LocalDate dia) {
            this.delegado = relojFijoEn(dia);
        }

        @Override
        public ZoneId getZone() {
            return delegado.getZone();
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return delegado.withZone(zona);
        }

        @Override
        public Instant instant() {
            return delegado.instant();
        }
    }
}
