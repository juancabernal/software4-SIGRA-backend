package co.edu.uco.sigra.asignaturas.service;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.exception.FiltroInvalidoException;
import co.edu.uco.sigra.asignaturas.mapper.AsignaturaMapper;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.asignaturas.repository.ConteoRaPorAsignatura;
import co.edu.uco.sigra.asignaturas.service.impl.AsignaturaServiceImpl;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.shared.enums.EstadoRegistro;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Consulta de asignaturas con filtros")
class AsignaturaServiceConsultaTest {

    private static final UUID ID_SISTEMAS = UUID.randomUUID();
    private static final UUID ID_MATEMATICAS = UUID.randomUUID();

    @Mock
    private AsignaturaRepository asignaturaRepository;

    @Mock
    private ProgramaAcademicoRepository programaAcademicoRepository;

    @Mock
    private ResultadoAprendizajeRepository resultadoAprendizajeRepository;

    private final AsignaturaMapper mapper = Mappers.getMapper(AsignaturaMapper.class);

    private AsignaturaServiceImpl service;

    private Asignatura software;
    private Asignatura arquitectura;
    private Asignatura basesDeDatos;
    private Asignatura calculo;

    @BeforeEach
    void setUp() {
        service = new AsignaturaServiceImpl(asignaturaRepository, programaAcademicoRepository,
                resultadoAprendizajeRepository, mapper);

        ProgramaAcademico sistemas = programa(ID_SISTEMAS, "Ingeniería de Sistemas");
        ProgramaAcademico matematicas = programa(ID_MATEMATICAS, "Matemáticas");
        software = asignatura("ISW4", "Ingeniería de Software IV", sistemas, EstadoAsignatura.ACTIVA);
        arquitectura = asignatura("ARQ-1", "arquitectura de Software", sistemas, EstadoAsignatura.BORRADOR);
        basesDeDatos = asignatura("BD2", "Bases de Datos", matematicas, EstadoAsignatura.BORRADOR);
        calculo = asignatura("MAT1", "Cálculo", matematicas, EstadoAsignatura.INACTIVA);
    }

    private static ProgramaAcademico programa(UUID id, String nombre) {
        ProgramaAcademico p = new ProgramaAcademico();
        p.setId(id);
        p.setNombre(nombre);
        return p;
    }

    private static Asignatura asignatura(String codigo, String nombre, ProgramaAcademico programa,
                                         EstadoAsignatura estado) {
        Asignatura a = new Asignatura(codigo, nombre, programa);
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        if (estado != EstadoAsignatura.BORRADOR) {
            a.activar(Asignatura.MIN_RA_ACTIVOS);
        }
        if (estado == EstadoAsignatura.INACTIVA) {
            a.inactivar();
        }
        return a;
    }

    private static ConteoRaPorAsignatura conteo(Asignatura asignatura, long cantidad) {
        return new ConteoRaPorAsignatura() {
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

    /** Catálogo de prueba: Software (6 RA), Arquitectura (3 RA), Bases de Datos y Cálculo sin RA activos. */
    private void catalogoCompleto() {
        when(asignaturaRepository.findAll()).thenReturn(List.of(software, arquitectura, basesDeDatos, calculo));
        when(asignaturaRepository.contarRaPorAsignatura(EstadoRegistro.ACTIVO))
                .thenReturn(List.of(conteo(software, 6), conteo(arquitectura, 3)));
    }

    private List<String> codigos(List<AsignaturaResponseDTO> resultado) {
        return resultado.stream().map(AsignaturaResponseDTO::codigo).toList();
    }

    private static AsignaturaFiltroDTO sinFiltros() {
        return new AsignaturaFiltroDTO(null, null, null, null, null);
    }

    private static AsignaturaFiltroDTO porTexto(String texto) {
        return new AsignaturaFiltroDTO(texto, null, null, null, null);
    }

    @Test
    @DisplayName("Sin filtros devuelve todas con nombre, código, programa, estado y cantidad de RA")
    void sinFiltrosDevuelveTodas() {
        catalogoCompleto();

        List<AsignaturaResponseDTO> resultado = service.consultar(sinFiltros());

        assertThat(resultado).hasSize(4);
        AsignaturaResponseDTO dto = resultado.stream().filter(r -> r.codigo().equals("ISW4")).findFirst().orElseThrow();
        assertThat(dto.id()).isEqualTo(software.getId());
        assertThat(dto.nombre()).isEqualTo("Ingeniería de Software IV");
        assertThat(dto.programaId()).isEqualTo(ID_SISTEMAS);
        assertThat(dto.programaNombre()).isEqualTo("Ingeniería de Sistemas");
        assertThat(dto.estado()).isEqualTo(EstadoAsignatura.ACTIVA);
        assertThat(dto.cantidadRa()).isEqualTo(6);
    }

    @Test
    @DisplayName("Una asignatura sin RA activos aparece con cantidad 0")
    void asignaturaSinRaApareceConCero() {
        catalogoCompleto();

        List<AsignaturaResponseDTO> resultado = service.consultar(sinFiltros());

        assertThat(resultado).filteredOn(r -> r.codigo().equals("BD2"))
                .singleElement()
                .extracting(AsignaturaResponseDTO::cantidadRa)
                .isEqualTo(0L);
    }

    @Nested
    @DisplayName("Búsqueda por texto")
    class BusquedaPorTexto {

        @Test
        @DisplayName("Encuentra por coincidencia parcial en el nombre")
        void porNombre() {
            catalogoCompleto();

            assertThat(codigos(service.consultar(porTexto("datos")))).containsExactly("BD2");
        }

        @Test
        @DisplayName("Encuentra por coincidencia parcial en el código")
        void porCodigo() {
            catalogoCompleto();

            assertThat(codigos(service.consultar(porTexto("arq")))).containsExactly("ARQ-1");
        }

        @Test
        @DisplayName("No distingue tildes ni mayúsculas")
        void sinTildes() {
            catalogoCompleto();

            assertThat(codigos(service.consultar(porTexto("ingenieria")))).containsExactly("ISW4");
            assertThat(codigos(service.consultar(porTexto("CÁLCULO")))).containsExactly("MAT1");
        }

        @Test
        @DisplayName("Un texto en blanco se ignora")
        void textoEnBlanco() {
            catalogoCompleto();

            assertThat(service.consultar(porTexto("   "))).hasSize(4);
        }
    }

    @Test
    @DisplayName("Filtra por programa académico")
    void porPrograma() {
        catalogoCompleto();

        List<AsignaturaResponseDTO> resultado =
                service.consultar(new AsignaturaFiltroDTO(null, ID_MATEMATICAS, null, null, null));

        assertThat(codigos(resultado)).containsExactly("BD2", "MAT1");
    }

    @Test
    @DisplayName("Filtra por estado")
    void porEstado() {
        catalogoCompleto();

        List<AsignaturaResponseDTO> resultado =
                service.consultar(new AsignaturaFiltroDTO(null, null, EstadoAsignatura.BORRADOR, null, null));

        assertThat(codigos(resultado)).containsExactly("ARQ-1", "BD2");
    }

    @Test
    @DisplayName("Filtra por rango de RA activos con límites inclusivos")
    void porRangoDeRa() {
        catalogoCompleto();

        assertThat(codigos(service.consultar(new AsignaturaFiltroDTO(null, null, null, 3, 6))))
                .containsExactly("ARQ-1", "ISW4");
        assertThat(codigos(service.consultar(new AsignaturaFiltroDTO(null, null, null, 0, 0))))
                .containsExactly("BD2", "MAT1");
    }

    @Test
    @DisplayName("Combina todos los filtros con AND")
    void filtrosCombinados() {
        catalogoCompleto();

        List<AsignaturaResponseDTO> resultado = service.consultar(
                new AsignaturaFiltroDTO("software", ID_SISTEMAS, EstadoAsignatura.BORRADOR, 1, 5));

        assertThat(codigos(resultado)).containsExactly("ARQ-1");
    }

    @Test
    @DisplayName("Ordena por nombre sin distinguir mayúsculas")
    void ordenadoPorNombre() {
        catalogoCompleto();

        assertThat(service.consultar(sinFiltros())).extracting(AsignaturaResponseDTO::nombre)
                .containsExactly("arquitectura de Software", "Bases de Datos", "Cálculo", "Ingeniería de Software IV");
    }

    @Test
    @DisplayName("Sin asignaturas registradas devuelve una lista vacía")
    void sinAsignaturas() {
        when(asignaturaRepository.findAll()).thenReturn(List.of());
        when(asignaturaRepository.contarRaPorAsignatura(EstadoRegistro.ACTIVO)).thenReturn(List.of());

        assertThat(service.consultar(sinFiltros())).isEmpty();
    }

    @Nested
    @DisplayName("Rango de RA inválido")
    class RangoInvalido {

        @Test
        @DisplayName("Un mínimo mayor que el máximo lanza excepción sin consultar los repositorios")
        void minimoMayorQueMaximo() {
            assertThatThrownBy(() -> service.consultar(new AsignaturaFiltroDTO(null, null, null, 6, 5)))
                    .isInstanceOf(FiltroInvalidoException.class)
                    .hasMessage("La cantidad mínima de RA no puede ser mayor que la máxima.");
            verifyNoInteractions(asignaturaRepository, programaAcademicoRepository, resultadoAprendizajeRepository);
        }

        @Test
        @DisplayName("Un mínimo negativo lanza excepción sin consultar los repositorios")
        void minimoNegativo() {
            assertThatThrownBy(() -> service.consultar(new AsignaturaFiltroDTO(null, null, null, -1, null)))
                    .isInstanceOf(FiltroInvalidoException.class)
                    .hasMessage("La cantidad de RA no puede ser negativa.");
            verifyNoInteractions(asignaturaRepository, programaAcademicoRepository, resultadoAprendizajeRepository);
        }

        @Test
        @DisplayName("Un máximo negativo lanza excepción sin consultar los repositorios")
        void maximoNegativo() {
            assertThatThrownBy(() -> service.consultar(new AsignaturaFiltroDTO(null, null, null, null, -3)))
                    .isInstanceOf(FiltroInvalidoException.class)
                    .hasMessage("La cantidad de RA no puede ser negativa.");
            verifyNoInteractions(asignaturaRepository, programaAcademicoRepository, resultadoAprendizajeRepository);
        }
    }
}
