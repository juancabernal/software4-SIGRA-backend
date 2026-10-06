package co.edu.uco.sigra.programas.service;

import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.mapper.ProgramaMapper;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.programas.service.impl.ProgramaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de programas académicos")
class ProgramaServiceImplTest {

    private static final Sort POR_NOMBRE = Sort.by("nombre");

    @Mock
    private ProgramaAcademicoRepository programaRepository;

    @Mock
    private AsignaturaRepository asignaturaRepository;

    private final ProgramaMapper mapper = Mappers.getMapper(ProgramaMapper.class);

    private ProgramaServiceImpl service;

    private ProgramaAcademico derecho;
    private ProgramaAcademico ingenieria;
    private ProgramaAcademico medicina;

    @BeforeEach
    void setUp() {
        service = new ProgramaServiceImpl(programaRepository, mapper, asignaturaRepository);
        derecho = programa("DER", "Derecho", EstadoRegistro.ACTIVO);
        ingenieria = programa("ISIS", "Ingeniería de Sistemas", EstadoRegistro.ACTIVO);
        medicina = programa("MED", "Medicina", EstadoRegistro.INACTIVO);
    }

    private static ProgramaAcademico programa(String codigo, String nombre, EstadoRegistro estado) {
        ProgramaAcademico p = new ProgramaAcademico();
        p.setId(UUID.randomUUID());
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setEstado(estado);
        return p;
    }

    private void catalogoOrdenado() {
        when(programaRepository.findAll(POR_NOMBRE)).thenReturn(List.of(derecho, ingenieria, medicina));
    }

    @Test
    @DisplayName("Pide los programas ordenados por nombre y conserva ese orden")
    void ordenadosPorNombre() {
        catalogoOrdenado();

        List<ProgramaResponseDTO> resultado = service.consultar(null);

        verify(programaRepository).findAll(POR_NOMBRE);
        assertThat(resultado).extracting(ProgramaResponseDTO::nombre)
                .containsExactly("Derecho", "Ingeniería de Sistemas", "Medicina");
    }

    @Test
    @DisplayName("Sin filtro devuelve todos los programas con sus datos")
    void sinFiltro() {
        catalogoOrdenado();

        List<ProgramaResponseDTO> resultado = service.consultar(null);

        assertThat(resultado).hasSize(3);
        ProgramaResponseDTO primero = resultado.getFirst();
        assertThat(primero.id()).isEqualTo(derecho.getId());
        assertThat(primero.codigo()).isEqualTo("DER");
        assertThat(primero.nombre()).isEqualTo("Derecho");
        assertThat(primero.estado()).isEqualTo(EstadoRegistro.ACTIVO);
    }

    @Test
    @DisplayName("Con estado ACTIVO devuelve solo los programas activos")
    void filtroActivo() {
        catalogoOrdenado();

        assertThat(service.consultar(EstadoRegistro.ACTIVO)).extracting(ProgramaResponseDTO::codigo)
                .containsExactly("DER", "ISIS");
    }

    @Test
    @DisplayName("Con estado INACTIVO devuelve solo los programas inactivos")
    void filtroInactivo() {
        catalogoOrdenado();

        assertThat(service.consultar(EstadoRegistro.INACTIVO)).extracting(ProgramaResponseDTO::codigo)
                .containsExactly("MED");
    }

    @Test
    @DisplayName("Sin programas registrados devuelve una lista vacía")
    void listaVacia() {
        when(programaRepository.findAll(POR_NOMBRE)).thenReturn(List.of());

        assertThat(service.consultar(null)).isEmpty();
    }
}
