package co.edu.uco.sigra.asignaturas.service;

import co.edu.uco.sigra.asignaturas.dto.MiAsignaturaDTO;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.asignaturas.mapper.AsignaturaMapper;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.asignaturas.repository.ConteoPorAsignatura;
import co.edu.uco.sigra.asignaturas.repository.ProfesorDeAsignatura;
import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.asignaturas.service.impl.AsignaturaServiceImpl;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mis asignaturas (Profesor y Estudiante)")
class AsignaturaServiceMisAsignaturasTest {

    private static final UUID USUARIO = UUID.randomUUID();

    @Mock
    private AsignaturaRepository asignaturaRepository;
    @Mock
    private ProgramaAcademicoRepository programaAcademicoRepository;
    @Mock
    private ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    @Mock
    private AsignacionDocenteRepository asignacionDocenteRepository;

    private final AsignaturaMapper mapper = Mappers.getMapper(AsignaturaMapper.class);
    private AsignaturaServiceImpl service;
    private Asignatura siete;
    private Asignatura activa;

    @BeforeEach
    void setUp() {
        service = new AsignaturaServiceImpl(asignaturaRepository, programaAcademicoRepository,
                resultadoAprendizajeRepository, asignacionDocenteRepository, mapper);
        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setId(UUID.randomUUID());
        programa.setNombre("Programa Bruno Activo");
        siete = asignatura("BRU07", "Bruno Siete RA", programa);
        activa = asignatura("BRU10", "bruno Activa", programa);
    }

    private static Asignatura asignatura(String codigo, String nombre, ProgramaAcademico programa) {
        Asignatura a = new Asignatura(codigo, nombre, programa);
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        return a;
    }

    private static ConteoPorAsignatura conteo(Asignatura asignatura, long cantidad) {
        return new ConteoPorAsignatura() {
            @Override
            public UUID getAsignaturaId() {
                return asignatura.getId();
            }

            @Override
            public Long getCantidad() {
                return cantidad;
            }
        };
    }

    private static ProfesorDeAsignatura profesor(Asignatura asignatura, String nombre) {
        return new ProfesorDeAsignatura() {
            @Override
            public UUID getAsignaturaId() {
                return asignatura.getId();
            }

            @Override
            public String getNombre() {
                return nombre;
            }
        };
    }

    private void conteosYProfesores() {
        when(asignaturaRepository.contarRaDe(anyCollection(), eq(EstadoRegistro.ACTIVO)))
                .thenReturn(List.of(conteo(siete, 7), conteo(activa, 5)));
        when(asignaturaRepository.contarMatriculasDe(anyCollection(), eq(EstadoRegistro.ACTIVO)))
                .thenReturn(List.of(conteo(activa, 1)));
        when(asignaturaRepository.profesoresDe(anyCollection(), eq(EstadoRegistro.ACTIVO)))
                .thenReturn(List.of(profesor(activa, "Profesor Bruno"), profesor(activa, "Ana Docente"),
                        profesor(siete, "Profesor Bruno")));
    }

    @Test
    @DisplayName("El PROFESOR recibe sus asignaturas ordenadas por nombre con RA, estudiantes y profesores")
    void profesor() {
        when(asignaturaRepository.findAsignadasAProfesor(USUARIO, EstadoRegistro.ACTIVO))
                .thenReturn(List.of(siete, activa));
        conteosYProfesores();

        List<MiAsignaturaDTO> resultado = service.misAsignaturas(new IdentidadActual(USUARIO, RolUsuario.PROFESOR));

        assertThat(resultado).extracting(MiAsignaturaDTO::codigo).containsExactly("BRU10", "BRU07");
        MiAsignaturaDTO primera = resultado.getFirst();
        assertThat(primera.cantidadRa()).isEqualTo(5);
        assertThat(primera.cantidadEstudiantes()).isEqualTo(1);
        assertThat(primera.profesores()).containsExactly("Ana Docente", "Profesor Bruno");
        assertThat(primera.programaNombre()).isEqualTo("Programa Bruno Activo");
        assertThat(primera.estado()).isEqualTo(EstadoAsignatura.BORRADOR);
        assertThat(resultado.get(1).cantidadEstudiantes()).isZero();
        verify(asignaturaRepository, never()).findMatriculadasPorEstudiante(any(), any());
    }

    @Test
    @DisplayName("El ESTUDIANTE recibe las asignaturas donde tiene matrícula ACTIVA")
    void estudiante() {
        when(asignaturaRepository.findMatriculadasPorEstudiante(USUARIO, EstadoRegistro.ACTIVO))
                .thenReturn(List.of(activa));
        conteosYProfesores();

        List<MiAsignaturaDTO> resultado = service.misAsignaturas(new IdentidadActual(USUARIO, RolUsuario.ESTUDIANTE));

        assertThat(resultado).extracting(MiAsignaturaDTO::codigo).containsExactly("BRU10");
        verify(asignaturaRepository, never()).findAsignadasAProfesor(any(), any());
    }

    @Test
    @DisplayName("Los datos se cargan con una consulta agrupada por dato, no una por asignatura (sin N+1)")
    void sinNmasUno() {
        when(asignaturaRepository.findAsignadasAProfesor(USUARIO, EstadoRegistro.ACTIVO))
                .thenReturn(List.of(siete, activa));
        conteosYProfesores();

        service.misAsignaturas(new IdentidadActual(USUARIO, RolUsuario.PROFESOR));

        verify(asignaturaRepository, times(1)).contarRaDe(anyCollection(), any());
        verify(asignaturaRepository, times(1)).contarMatriculasDe(anyCollection(), any());
        verify(asignaturaRepository, times(1)).profesoresDe(anyCollection(), any());
        verifyNoInteractions(resultadoAprendizajeRepository, asignacionDocenteRepository);
    }

    @Test
    @DisplayName("Sin asignaturas devuelve una lista vacía sin más consultas")
    void vacia() {
        when(asignaturaRepository.findAsignadasAProfesor(USUARIO, EstadoRegistro.ACTIVO)).thenReturn(List.of());

        assertThat(service.misAsignaturas(new IdentidadActual(USUARIO, RolUsuario.PROFESOR))).isEmpty();
        verify(asignaturaRepository, never()).contarRaDe(any(), any());
    }

    @Test
    @DisplayName("Un ADMINISTRADOR no tiene «mis asignaturas» (403)")
    void administrador() {
        assertThatThrownBy(() -> service.misAsignaturas(new IdentidadActual(USUARIO, RolUsuario.ADMINISTRADOR)))
                .isInstanceOf(PermisoInsuficienteException.class);
        verifyNoInteractions(asignaturaRepository);
    }
}
