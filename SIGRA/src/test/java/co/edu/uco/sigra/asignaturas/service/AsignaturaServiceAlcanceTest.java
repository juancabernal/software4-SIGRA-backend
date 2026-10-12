package co.edu.uco.sigra.asignaturas.service;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.asignaturas.mapper.AsignaturaMapper;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Alcance por rol del servicio de asignaturas")
class AsignaturaServiceAlcanceTest {

    private static final UUID PROFESOR = UUID.randomUUID();
    private static final AsignaturaFiltroDTO SIN_FILTROS = new AsignaturaFiltroDTO(null, null, null, null, null);

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
    private Asignatura suya;
    private Asignatura ajena;

    @BeforeEach
    void setUp() {
        service = new AsignaturaServiceImpl(asignaturaRepository, programaAcademicoRepository,
                resultadoAprendizajeRepository, asignacionDocenteRepository, mapper);
        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setId(UUID.randomUUID());
        programa.setNombre("Ingeniería de Sistemas");
        suya = asignatura("BRU10", "Bruno Activa", programa);
        ajena = asignatura("BRU05", "Bruno Cinco RA", programa);
    }

    private static Asignatura asignatura(String codigo, String nombre, ProgramaAcademico programa) {
        Asignatura a = new Asignatura(codigo, nombre, programa);
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        return a;
    }

    private void profesorTieneAsignada(Asignatura asignatura) {
        AsignacionDocente asignacion = new AsignacionDocente();
        asignacion.setAsignatura(asignatura);
        asignacion.setEstado(EstadoRegistro.ACTIVO);
        when(asignacionDocenteRepository.findByProfesor_IdAndEstado(PROFESOR, EstadoRegistro.ACTIVO))
                .thenReturn(List.of(asignacion));
    }

    private void catalogo() {
        when(asignaturaRepository.findAll()).thenReturn(List.of(suya, ajena));
        when(asignaturaRepository.contarRaPorAsignatura(EstadoRegistro.ACTIVO)).thenReturn(List.of());
    }

    @Test
    @DisplayName("Un PROFESOR solo ve las asignaturas donde tiene asignación docente ACTIVA")
    void profesorVeSoloLasSuyas() {
        catalogo();
        profesorTieneAsignada(suya);

        List<AsignaturaResponseDTO> resultado =
                service.consultar(SIN_FILTROS, new IdentidadActual(PROFESOR, RolUsuario.PROFESOR));

        assertThat(resultado).extracting(AsignaturaResponseDTO::codigo).containsExactly("BRU10");
    }

    @Test
    @DisplayName("Un ADMINISTRADOR ve todas y no se consultan asignaciones docentes")
    void administradorVeTodas() {
        catalogo();

        List<AsignaturaResponseDTO> resultado =
                service.consultar(SIN_FILTROS, new IdentidadActual(UUID.randomUUID(), RolUsuario.ADMINISTRADOR));

        assertThat(resultado).hasSize(2);
        verifyNoInteractions(asignacionDocenteRepository);
    }

    @Test
    @DisplayName("Sin identidad (control por rol apagado) se ve todo, como siempre")
    void sinIdentidad() {
        catalogo();

        assertThat(service.consultar(SIN_FILTROS)).hasSize(2);
        assertThat(service.consultar(SIN_FILTROS, null)).hasSize(2);
        verifyNoInteractions(asignacionDocenteRepository);
    }

    @Test
    @DisplayName("Un PROFESOR obtiene el detalle de una asignatura suya")
    void profesorObtieneLaSuya() {
        profesorTieneAsignada(suya);
        when(asignaturaRepository.findById(suya.getId())).thenReturn(Optional.of(suya));

        AsignaturaResponseDTO dto =
                service.obtenerAsignatura(suya.getId(), new IdentidadActual(PROFESOR, RolUsuario.PROFESOR));

        assertThat(dto.codigo()).isEqualTo("BRU10");
    }

    @Test
    @DisplayName("Un PROFESOR recibe 403 al pedir una asignatura ajena, sin consultarla")
    void profesorAjena() {
        profesorTieneAsignada(suya);

        assertThatThrownBy(() -> service.obtenerAsignatura(ajena.getId(),
                new IdentidadActual(PROFESOR, RolUsuario.PROFESOR)))
                .isInstanceOf(PermisoInsuficienteException.class);
        verify(asignaturaRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Un ESTUDIANTE no consulta el catálogo de asignaturas")
    void estudianteNoConsulta() {
        IdentidadActual estudiante = new IdentidadActual(UUID.randomUUID(), RolUsuario.ESTUDIANTE);

        assertThatThrownBy(() -> service.consultar(SIN_FILTROS, estudiante))
                .isInstanceOf(PermisoInsuficienteException.class);
        assertThatThrownBy(() -> service.obtenerAsignatura(suya.getId(), estudiante))
                .isInstanceOf(PermisoInsuficienteException.class);
        verifyNoInteractions(asignaturaRepository);
    }
}
