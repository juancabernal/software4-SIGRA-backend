package co.edu.uco.sigra.asignaturas.service;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.MiAsignaturaDTO;
import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.mapper.AsignaturaMapper;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.asignaturas.repository.EvaluacionAbiertaRepository;
import co.edu.uco.sigra.asignaturas.service.impl.AsignaturaServiceImpl;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.facultades.entity.Facultad;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.resultadosaprendizaje.service.ResultadoAprendizajeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Incidencia 0000036 (segunda parte): la facultad en las respuestas de asignaturas y el filtro facultadId. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Asignaturas por facultad: campos de facultad y filtro facultadId")
class AsignaturaServiceFacultadTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID PROFESOR = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    @Mock private AsignaturaRepository asignaturaRepository;
    @Mock private ProgramaAcademicoRepository programaAcademicoRepository;
    @Mock private ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    @Mock private AsignacionDocenteRepository asignacionDocenteRepository;
    @Mock private ResultadoAprendizajeService resultadoAprendizajeService;
    @Mock private EvaluacionService evaluacionService;
    @Mock private EvaluacionAbiertaRepository evaluacionAbiertaRepository;

    private final AsignaturaMapper mapper = Mappers.getMapper(AsignaturaMapper.class);
    private AsignaturaServiceImpl service;

    private Facultad ingenieria;
    private Facultad sinAsignar;
    private ProgramaAcademico sistemas;
    private ProgramaAcademico derecho;
    private Asignatura programacion;
    private Asignatura bases;
    private Asignatura civil;

    @BeforeEach
    void setUp() {
        service = new AsignaturaServiceImpl(asignaturaRepository, programaAcademicoRepository,
                resultadoAprendizajeRepository, asignacionDocenteRepository, mapper,
                resultadoAprendizajeService, evaluacionService, evaluacionAbiertaRepository);
        ingenieria = facultad(UUID.randomUUID(), "Facultad de Ingeniería");
        sinAsignar = facultad(UUID.fromString("00000000-0000-4000-fa00-000000000001"), "Facultad sin asignar");
        sistemas = programa("Ingeniería de Sistemas", ingenieria);
        derecho = programa("Derecho", sinAsignar);
        programacion = asignatura("SIS101", "Programación", sistemas);
        bases = asignatura("SIS201", "Bases de Datos", sistemas);
        civil = asignatura("DER101", "Derecho Civil", derecho);
    }

    private static Facultad facultad(UUID id, String nombre) {
        Facultad f = new Facultad();
        f.setId(id);
        f.setNombre(nombre);
        f.setEstado(EstadoRegistro.ACTIVO);
        return f;
    }

    private static ProgramaAcademico programa(String nombre, Facultad facultad) {
        ProgramaAcademico p = new ProgramaAcademico();
        p.setId(UUID.randomUUID());
        p.setNombre(nombre);
        p.setFacultad(facultad);
        return p;
    }

    private static Asignatura asignatura(String codigo, String nombre, ProgramaAcademico programa) {
        Asignatura a = new Asignatura(codigo, nombre, programa);
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        return a;
    }

    private static UsuarioPrincipal admin() {
        return new UsuarioPrincipal(ADMIN, RolUsuario.ADMINISTRADOR);
    }

    private static UsuarioPrincipal profesor() {
        return new UsuarioPrincipal(PROFESOR, RolUsuario.PROFESOR);
    }

    private static AsignaturaFiltroDTO filtro(UUID programaId, UUID facultadId) {
        return new AsignaturaFiltroDTO(null, programaId, facultadId, null, null, null, null);
    }

    @Nested
    @DisplayName("Mapper")
    class Mapper {

        @Test
        @DisplayName("La respuesta trae la facultad del programa de la asignatura")
        void llenaLaFacultad() {
            AsignaturaResponseDTO dto = mapper.toResponseDTO(programacion, 5);

            assertThat(dto.facultadId()).isEqualTo(ingenieria.getId());
            assertThat(dto.facultadNombre()).isEqualTo("Facultad de Ingeniería");
            assertThat(dto.programaNombre()).isEqualTo("Ingeniería de Sistemas");
        }

        @Test
        @DisplayName("Un programa en «Facultad sin asignar» también la informa")
        void facultadSinAsignar() {
            AsignaturaResponseDTO dto = mapper.toResponseDTO(civil, 0);

            assertThat(dto.facultadId()).isEqualTo(UUID.fromString("00000000-0000-4000-fa00-000000000001"));
            assertThat(dto.facultadNombre()).isEqualTo("Facultad sin asignar");
        }
    }

    @Nested
    @DisplayName("GET /asignaturas con facultadId")
    class Consultar {

        @Test
        @DisplayName("Sin facultadId usa el catálogo completo con programa y facultad cargados")
        void sinFacultadUsaCatalogoCompleto() {
            when(asignaturaRepository.findAllConProgramaYFacultad()).thenReturn(List.of(programacion, civil));

            List<AsignaturaResponseDTO> resultado = service.consultar(filtro(null, null), admin());

            assertThat(resultado).extracting(AsignaturaResponseDTO::facultadNombre)
                    .containsExactly("Facultad sin asignar", "Facultad de Ingeniería");
            verify(asignaturaRepository, never()).findAllDeFacultad(any());
        }

        @Test
        @DisplayName("Con facultadId el filtro se resuelve en la consulta de la facultad")
        void conFacultadConsultaPorFacultad() {
            when(asignaturaRepository.findAllDeFacultad(ingenieria.getId())).thenReturn(List.of(programacion, bases));

            List<AsignaturaResponseDTO> resultado = service.consultar(filtro(null, ingenieria.getId()), admin());

            assertThat(resultado).extracting(AsignaturaResponseDTO::codigo).containsExactly("SIS201", "SIS101");
            assertThat(resultado).allMatch(a -> ingenieria.getId().equals(a.facultadId()));
            verify(asignaturaRepository, never()).findAllConProgramaYFacultad();
        }

        @Test
        @DisplayName("facultadId combinado con un programa de esa facultad acota por los dos")
        void combinaConProgramaDeLaMismaFacultad() {
            when(asignaturaRepository.findAllDeFacultad(ingenieria.getId())).thenReturn(List.of(programacion, bases));

            assertThat(service.consultar(filtro(sistemas.getId(), ingenieria.getId()), admin())).hasSize(2);
        }

        @Test
        @DisplayName("facultadId con un programa de otra facultad devuelve lista vacía, no error")
        void programaDeOtraFacultadDaVacio() {
            when(asignaturaRepository.findAllDeFacultad(ingenieria.getId())).thenReturn(List.of(programacion, bases));

            assertThat(service.consultar(filtro(derecho.getId(), ingenieria.getId()), admin())).isEmpty();
        }

        @Test
        @DisplayName("Una facultad sin asignaturas (o inexistente) devuelve lista vacía")
        void facultadSinDatos() {
            UUID otra = UUID.randomUUID();
            when(asignaturaRepository.findAllDeFacultad(otra)).thenReturn(List.of());

            assertThat(service.consultar(filtro(null, otra), admin())).isEmpty();
        }

        @Test
        @DisplayName("El PROFESOR con facultadId solo ve sus asignaturas de esa facultad")
        void profesorSoloLasSuyas() {
            AsignacionDocente asignacion = new AsignacionDocente();
            asignacion.setAsignatura(programacion);
            asignacion.setEstado(EstadoRegistro.ACTIVO);
            when(asignacionDocenteRepository.findByProfesor_IdAndEstado(PROFESOR, EstadoRegistro.ACTIVO))
                    .thenReturn(List.of(asignacion));
            when(asignaturaRepository.findAllDeFacultad(ingenieria.getId())).thenReturn(List.of(programacion, bases));

            List<AsignaturaResponseDTO> resultado = service.consultar(filtro(null, ingenieria.getId()), profesor());

            assertThat(resultado).extracting(AsignaturaResponseDTO::codigo).containsExactly("SIS101");
        }
    }

    @Nested
    @DisplayName("GET /asignaturas/mias con facultadId")
    class MisAsignaturas {

        @Test
        @DisplayName("Cada asignatura trae la facultad de su programa")
        void traeLaFacultad() {
            when(asignaturaRepository.findAsignadasAProfesor(PROFESOR, EstadoRegistro.ACTIVO))
                    .thenReturn(List.of(programacion, civil));

            List<MiAsignaturaDTO> resultado = service.misAsignaturas(profesor());

            assertThat(resultado).extracting(MiAsignaturaDTO::facultadNombre)
                    .containsExactly("Facultad sin asignar", "Facultad de Ingeniería");
            assertThat(resultado.get(1).facultadId()).isEqualTo(ingenieria.getId());
        }

        @Test
        @DisplayName("El PROFESOR con facultadId consulta solo sus asignaturas de esa facultad")
        void profesorFiltraEnLaConsulta() {
            when(asignaturaRepository.findAsignadasAProfesorEnFacultad(PROFESOR, EstadoRegistro.ACTIVO, ingenieria.getId()))
                    .thenReturn(List.of(programacion));

            List<MiAsignaturaDTO> resultado = service.misAsignaturas(profesor(), ingenieria.getId());

            assertThat(resultado).extracting(MiAsignaturaDTO::codigo).containsExactly("SIS101");
            verify(asignaturaRepository, never()).findAsignadasAProfesor(any(), any());
        }

        @Test
        @DisplayName("El ESTUDIANTE con facultadId consulta solo sus matrículas de esa facultad")
        void estudianteFiltraEnLaConsulta() {
            when(asignaturaRepository.findMatriculadasPorEstudianteEnFacultad(
                    ESTUDIANTE, EstadoMatricula.ACTIVA, sinAsignar.getId())).thenReturn(List.of(civil));

            List<MiAsignaturaDTO> resultado = service.misAsignaturas(
                    new UsuarioPrincipal(ESTUDIANTE, RolUsuario.ESTUDIANTE), sinAsignar.getId());

            assertThat(resultado).extracting(MiAsignaturaDTO::codigo).containsExactly("DER101");
            verify(asignaturaRepository, never()).findMatriculadasPorEstudiante(any(), any());
        }

        @Test
        @DisplayName("Una facultad donde no tiene asignaturas devuelve lista vacía")
        void sinCoincidencias() {
            UUID otra = UUID.randomUUID();
            when(asignaturaRepository.findAsignadasAProfesorEnFacultad(PROFESOR, EstadoRegistro.ACTIVO, otra))
                    .thenReturn(List.of());

            assertThat(service.misAsignaturas(profesor(), otra)).isEmpty();
        }
    }
}
