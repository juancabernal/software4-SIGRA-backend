package co.edu.uco.sigra.estudiantes.service;

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

import java.time.LocalDate;
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

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private AsignaturaRepository asignaturaRepository;

    @Mock
    private SemestreRepository semestreRepository;

    private MatriculaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MatriculaServiceImpl(matriculaRepository, estudianteRepository, asignaturaRepository, semestreRepository);
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
        s.setFechaInicio(LocalDate.now().minusMonths(1));
        s.setFechaFin(LocalDate.now().plusMonths(1));
        s.setEstado(estado);
        return s;
    }

    private static Matricula matricula(UUID id, Estudiante estudiante, Asignatura asignatura, Semestre semestre, EstadoRegistro estado) {
        Matricula m = new Matricula();
        m.setId(id);
        m.setEstudiante(estudiante);
        m.setAsignatura(asignatura);
        m.setSemestre(semestre);
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
        @DisplayName("Con terna nueva y los tres activos, crea la matrícula ACTIVA sin reactivarla")
        void caminoFeliz() {
            stubTresActivos();
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE)).thenReturn(Optional.empty());
            saveDevuelveArgumento();

            ResultadoMatricula resultado = service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE);

            assertThat(resultado.reactivada()).isFalse();

            ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
            verify(matriculaRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
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
        @DisplayName("Con la asignatura INACTIVA lanza excepción sin consultar la terna")
        void asignaturaInactiva() {
            stubTresActivos();
            when(asignaturaRepository.findById(ID_ASIGNATURA)).thenReturn(Optional.of(asignatura(EstadoAsignatura.INACTIVA)));

            assertThatThrownBy(() -> service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .isInstanceOf(ReglaDeMatriculaException.class)
                    .hasMessageContaining(ID_ASIGNATURA.toString());

            verify(matriculaRepository, never()).buscarPorTerna(any(), any(), any());
        }

        @Test
        @DisplayName("Con el semestre INACTIVO lanza excepción sin consultar la terna")
        void semestreInactivo() {
            stubTresActivos();
            when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestre(EstadoSemestre.INACTIVO)));

            assertThatThrownBy(() -> service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .isInstanceOf(ReglaDeMatriculaException.class)
                    .hasMessageContaining(ID_SEMESTRE.toString());

            verify(matriculaRepository, never()).buscarPorTerna(any(), any(), any());
        }

        @Test
        @DisplayName("Con la terna ya matriculada y ACTIVA lanza excepción y no guarda")
        void ternaConMatriculaActivaExistente() {
            stubTresActivos();
            Matricula existente = matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoRegistro.ACTIVO);
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE))
                    .isInstanceOf(MatriculaDuplicadaException.class)
                    .hasMessageContaining(ID_ESTUDIANTE.toString())
                    .hasMessageContaining(ID_ASIGNATURA.toString())
                    .hasMessageContaining(ID_SEMESTRE.toString());

            verify(matriculaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con la terna retirada (INACTIVA) existente, reactiva la misma matrícula en lugar de crear una nueva")
        void ternaConMatriculaInactivaExistenteSeReactiva() {
            stubTresActivos();
            Matricula existente = matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoRegistro.INACTIVO);
            when(matriculaRepository.buscarPorTerna(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE)).thenReturn(Optional.of(existente));
            when(matriculaRepository.save(any(Matricula.class))).thenAnswer(inv -> inv.getArgument(0));

            ResultadoMatricula resultado = service.matricular(ID_ESTUDIANTE, ID_ASIGNATURA, ID_SEMESTRE);

            assertThat(resultado.reactivada()).isTrue();
            assertThat(resultado.matricula().getId()).isEqualTo(ID_MATRICULA);
            assertThat(resultado.matricula().getEstado()).isEqualTo(EstadoRegistro.ACTIVO);

            ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
            verify(matriculaRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existente);
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
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
    }

    @Nested
    @DisplayName("Desvincular")
    class Desvincular {

        @Test
        @DisplayName("Con una matrícula ACTIVA la pasa a INACTIVA")
        void matriculaActiva() {
            Matricula existente = matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoRegistro.ACTIVO);
            when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existente));
            saveDevuelveArgumento();

            service.desvincular(ID_MATRICULA);

            ArgumentCaptor<Matricula> captor = ArgumentCaptor.forClass(Matricula.class);
            verify(matriculaRepository, times(1)).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(ID_MATRICULA);
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoRegistro.INACTIVO);

            verify(matriculaRepository, never()).delete(any());
            verify(matriculaRepository, never()).deleteById(any());
            verify(matriculaRepository, never()).deleteAll();
        }

        @Test
        @DisplayName("Con una matrícula inexistente lanza excepción")
        void matriculaInexistente() {
            when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.desvincular(ID_MATRICULA))
                    .isInstanceOf(MatriculaNoEncontradaException.class)
                    .hasMessageContaining(ID_MATRICULA.toString());

            verify(matriculaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con una matrícula ya INACTIVA lanza excepción y no guarda")
        void matriculaYaInactiva() {
            Matricula existente = matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoRegistro.INACTIVO);
            when(matriculaRepository.findById(ID_MATRICULA)).thenReturn(Optional.of(existente));

            assertThatThrownBy(() -> service.desvincular(ID_MATRICULA))
                    .isInstanceOf(MatriculaYaInactivaException.class)
                    .hasMessageContaining(ID_MATRICULA.toString());

            verify(matriculaRepository, never()).save(any());
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
        @DisplayName("Con incluirInactivas en false delega la consulta con el estado ACTIVO")
        void incluirInactivasFalse() {
            when(asignaturaRepository.existsById(ID_ASIGNATURA)).thenReturn(true);
            when(semestreRepository.existsById(ID_SEMESTRE)).thenReturn(true);
            when(matriculaRepository.buscarPorAsignaturaYSemestre(ID_ASIGNATURA, ID_SEMESTRE, EstadoRegistro.ACTIVO))
                    .thenReturn(List.of(matricula(ID_MATRICULA, estudiante(EstadoRegistro.ACTIVO),
                            asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoRegistro.ACTIVO)));

            List<Matricula> resultado = service.listarMatriculados(ID_ASIGNATURA, ID_SEMESTRE, false);

            assertThat(resultado).hasSize(1);
            verify(matriculaRepository).buscarPorAsignaturaYSemestre(ID_ASIGNATURA, ID_SEMESTRE, EstadoRegistro.ACTIVO);
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
            when(matriculaRepository.buscarPorAsignaturaYSemestre(ID_ASIGNATURA, ID_SEMESTRE, EstadoRegistro.ACTIVO))
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
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.ACTIVO), EstadoRegistro.ACTIVO);
            Matricula m2 = matricula(UUID.randomUUID(), estudiante(EstadoRegistro.ACTIVO),
                    asignatura(EstadoAsignatura.ACTIVA), semestre(EstadoSemestre.INACTIVO), EstadoRegistro.INACTIVO);
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
}
