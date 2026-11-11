package co.edu.uco.sigra.facultades.service;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.facultades.dto.FacultadRequestDTO;
import co.edu.uco.sigra.facultades.dto.FacultadResponseDTO;
import co.edu.uco.sigra.facultades.entity.Facultad;
import co.edu.uco.sigra.facultades.exception.FacultadConProgramasActivosException;
import co.edu.uco.sigra.facultades.exception.FacultadNoEncontradaException;
import co.edu.uco.sigra.facultades.exception.NombreFacultadDuplicadoException;
import co.edu.uco.sigra.facultades.mapper.FacultadMapper;
import co.edu.uco.sigra.facultades.repository.FacultadRepository;
import co.edu.uco.sigra.facultades.service.impl.FacultadServiceImpl;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de facultades")
class FacultadServiceImplTest {

    private static final Sort POR_NOMBRE = Sort.by("nombre");

    @Mock
    private FacultadRepository facultadRepository;

    @Mock
    private ProgramaAcademicoRepository programaRepository;

    private final FacultadMapper mapper = Mappers.getMapper(FacultadMapper.class);

    private FacultadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FacultadServiceImpl(facultadRepository, programaRepository, mapper);
    }

    private static Facultad facultad(String nombre, EstadoRegistro estado) {
        Facultad f = new Facultad();
        f.setId(UUID.randomUUID());
        f.setNombre(nombre);
        f.setEstado(estado);
        return f;
    }

    private Facultad registrada(String nombre, EstadoRegistro estado) {
        Facultad f = facultad(nombre, estado);
        when(facultadRepository.findById(f.getId())).thenReturn(Optional.of(f));
        return f;
    }

    private UUID inexistente() {
        UUID id = UUID.randomUUID();
        when(facultadRepository.findById(id)).thenReturn(Optional.empty());
        return id;
    }

    // ------------------------------------------------------------------ Registrar

    @Nested
    @DisplayName("Registrar")
    class Registrar {

        @Test
        @DisplayName("Con un nombre válido queda ACTIVA, con id y con el nombre sin espacios sobrantes")
        void registraActiva() {
            when(facultadRepository.existsByNombreIgnoreCase("Facultad de Ingeniería")).thenReturn(false);
            when(facultadRepository.save(any(Facultad.class))).thenAnswer(inv -> {
                Facultad guardada = inv.getArgument(0);
                guardada.setId(UUID.randomUUID());
                return guardada;
            });

            FacultadResponseDTO resultado = service.registrar(new FacultadRequestDTO("  Facultad de Ingeniería "));

            ArgumentCaptor<Facultad> captor = ArgumentCaptor.forClass(Facultad.class);
            verify(facultadRepository).save(captor.capture());
            assertThat(captor.getValue().getNombre()).isEqualTo("Facultad de Ingeniería");
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
            assertThat(resultado.id()).isNotNull();
            assertThat(resultado.nombre()).isEqualTo("Facultad de Ingeniería");
            assertThat(resultado.estado()).isEqualTo(EstadoRegistro.ACTIVO);
        }

        @Test
        @DisplayName("Valida la unicidad con el nombre ya recortado")
        void validaUnicidadConNombreRecortado() {
            when(facultadRepository.existsByNombreIgnoreCase("Facultad de Artes")).thenReturn(true);

            assertThatThrownBy(() -> service.registrar(new FacultadRequestDTO("   Facultad de Artes   ")))
                    .isInstanceOf(NombreFacultadDuplicadoException.class);
            verify(facultadRepository).existsByNombreIgnoreCase("Facultad de Artes");
        }

        @Test
        @DisplayName("Nombre ya usado (sin importar mayúsculas) lanza NombreFacultadDuplicadoException y no guarda")
        void nombreDuplicado() {
            when(facultadRepository.existsByNombreIgnoreCase("FACULTAD DE DERECHO")).thenReturn(true);

            assertThatThrownBy(() -> service.registrar(new FacultadRequestDTO("FACULTAD DE DERECHO")))
                    .isInstanceOf(NombreFacultadDuplicadoException.class)
                    .hasMessageContaining("FACULTAD DE DERECHO");
            verify(facultadRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ Consultar

    @Nested
    @DisplayName("Consultar")
    class Consultar {

        private Facultad derecho;
        private Facultad ingenieria;
        private Facultad medicina;

        @BeforeEach
        void catalogo() {
            derecho = facultad("Facultad de Derecho", EstadoRegistro.ACTIVO);
            ingenieria = facultad("Facultad de Ingeniería", EstadoRegistro.ACTIVO);
            medicina = facultad("Facultad de Medicina", EstadoRegistro.INACTIVO);
        }

        private void conCatalogo() {
            when(facultadRepository.findAll(POR_NOMBRE)).thenReturn(List.of(derecho, ingenieria, medicina));
        }

        @Test
        @DisplayName("Sin filtros pide las facultades ordenadas por nombre y conserva ese orden")
        void sinFiltrosOrdenadas() {
            conCatalogo();

            List<FacultadResponseDTO> resultado = service.consultar(null, null);

            verify(facultadRepository).findAll(POR_NOMBRE);
            assertThat(resultado).extracting(FacultadResponseDTO::nombre)
                    .containsExactly("Facultad de Derecho", "Facultad de Ingeniería", "Facultad de Medicina");
            assertThat(resultado.getFirst().id()).isEqualTo(derecho.getId());
        }

        @Test
        @DisplayName("Filtra por nombre parcial sin distinguir mayúsculas")
        void filtraPorNombreSinMayusculas() {
            conCatalogo();

            assertThat(service.consultar("DERECHO", null)).extracting(FacultadResponseDTO::nombre)
                    .containsExactly("Facultad de Derecho");
        }

        @Test
        @DisplayName("Filtra por nombre sin distinguir tildes: «ingenieria» encuentra «Ingeniería»")
        void filtraPorNombreSinTildes() {
            conCatalogo();

            assertThat(service.consultar("ingenieria", null)).extracting(FacultadResponseDTO::nombre)
                    .containsExactly("Facultad de Ingeniería");
        }

        @Test
        @DisplayName("Filtra por estado ACTIVO e INACTIVO")
        void filtraPorEstado() {
            conCatalogo();

            assertThat(service.consultar(null, EstadoRegistro.ACTIVO)).extracting(FacultadResponseDTO::nombre)
                    .containsExactly("Facultad de Derecho", "Facultad de Ingeniería");
            assertThat(service.consultar(null, EstadoRegistro.INACTIVO)).extracting(FacultadResponseDTO::nombre)
                    .containsExactly("Facultad de Medicina");
        }

        @Test
        @DisplayName("Combina nombre y estado")
        void combinaFiltros() {
            conCatalogo();

            assertThat(service.consultar("medicina", EstadoRegistro.ACTIVO)).isEmpty();
            assertThat(service.consultar("medicina", EstadoRegistro.INACTIVO)).hasSize(1);
        }

        @Test
        @DisplayName("Un filtro de nombre en blanco equivale a no filtrar")
        void filtroEnBlanco() {
            conCatalogo();

            assertThat(service.consultar("   ", null)).hasSize(3);
        }

        @Test
        @DisplayName("Sin coincidencias devuelve lista vacía")
        void sinCoincidencias() {
            conCatalogo();

            assertThat(service.consultar("arquitectura", null)).isEmpty();
        }

        @Test
        @DisplayName("Sin facultades registradas devuelve lista vacía")
        void sinFacultades() {
            when(facultadRepository.findAll(POR_NOMBRE)).thenReturn(List.of());

            assertThat(service.consultar(null, null)).isEmpty();
        }
    }

    // ------------------------------------------------------------------ Consultar por id

    @Nested
    @DisplayName("Consultar por id")
    class ConsultarPorId {

        @Test
        @DisplayName("Devuelve la facultad con sus datos")
        void existente() {
            Facultad existente = registrada("Facultad de Derecho", EstadoRegistro.INACTIVO);

            FacultadResponseDTO resultado = service.consultarPorId(existente.getId());

            assertThat(resultado.id()).isEqualTo(existente.getId());
            assertThat(resultado.nombre()).isEqualTo("Facultad de Derecho");
            assertThat(resultado.estado()).isEqualTo(EstadoRegistro.INACTIVO);
        }

        @Test
        @DisplayName("Facultad inexistente lanza FacultadNoEncontradaException con el id en el mensaje")
        void inexistenteLanza() {
            UUID id = inexistente();

            assertThatThrownBy(() -> service.consultarPorId(id))
                    .isInstanceOf(FacultadNoEncontradaException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    // ------------------------------------------------------------------ Modificar

    @Nested
    @DisplayName("Modificar nombre")
    class Modificar {

        @Test
        @DisplayName("Actualiza el nombre recortado y conserva id y estado")
        void actualizaNombre() {
            Facultad existente = registrada("Facultad de Derecho", EstadoRegistro.ACTIVO);
            when(facultadRepository.existsByNombreIgnoreCaseAndIdNot("Facultad de Ciencias Jurídicas", existente.getId()))
                    .thenReturn(false);
            when(facultadRepository.save(existente)).thenReturn(existente);

            FacultadResponseDTO resultado = service.actualizarNombre(
                    existente.getId(), new FacultadRequestDTO(" Facultad de Ciencias Jurídicas "));

            assertThat(resultado.id()).isEqualTo(existente.getId());
            assertThat(resultado.nombre()).isEqualTo("Facultad de Ciencias Jurídicas");
            assertThat(resultado.estado()).isEqualTo(EstadoRegistro.ACTIVO);
        }

        @Test
        @DisplayName("Permite cambiar solo mayúsculas del propio nombre (la unicidad excluye su id)")
        void permiteCambiarMayusculasDelPropioNombre() {
            Facultad existente = registrada("facultad de derecho", EstadoRegistro.ACTIVO);
            when(facultadRepository.existsByNombreIgnoreCaseAndIdNot("Facultad de Derecho", existente.getId()))
                    .thenReturn(false);
            when(facultadRepository.save(existente)).thenReturn(existente);

            assertThat(service.actualizarNombre(existente.getId(), new FacultadRequestDTO("Facultad de Derecho")).nombre())
                    .isEqualTo("Facultad de Derecho");
        }

        @Test
        @DisplayName("Nombre de otra facultad lanza NombreFacultadDuplicadoException y no guarda")
        void nombreDeOtra() {
            Facultad existente = registrada("Facultad de Derecho", EstadoRegistro.ACTIVO);
            when(facultadRepository.existsByNombreIgnoreCaseAndIdNot("Facultad de Medicina", existente.getId()))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.actualizarNombre(existente.getId(), new FacultadRequestDTO("Facultad de Medicina")))
                    .isInstanceOf(NombreFacultadDuplicadoException.class);
            verify(facultadRepository, never()).save(any());
            assertThat(existente.getNombre()).isEqualTo("Facultad de Derecho");
        }

        @Test
        @DisplayName("Facultad inexistente lanza FacultadNoEncontradaException y no valida el nombre")
        void inexistenteLanza() {
            UUID id = inexistente();

            assertThatThrownBy(() -> service.actualizarNombre(id, new FacultadRequestDTO("Nueva")))
                    .isInstanceOf(FacultadNoEncontradaException.class);
            verify(facultadRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
            verify(facultadRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ Inactivar

    @Nested
    @DisplayName("Inactivar")
    class Inactivar {

        @Test
        @DisplayName("Sin programas activos queda INACTIVA, se guarda y no se borra")
        void sinProgramasActivos() {
            Facultad existente = registrada("Facultad de Medicina", EstadoRegistro.ACTIVO);
            when(programaRepository.existsByFacultad_IdAndEstado(existente.getId(), EstadoRegistro.ACTIVO)).thenReturn(false);
            when(facultadRepository.save(existente)).thenReturn(existente);

            FacultadResponseDTO resultado = service.inactivar(existente.getId());

            assertThat(resultado.estado()).isEqualTo(EstadoRegistro.INACTIVO);
            verify(facultadRepository).save(existente);
            verify(facultadRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Con programas activos lanza FacultadConProgramasActivosException y sigue ACTIVA")
        void conProgramasActivos() {
            Facultad existente = registrada("Facultad de Ingeniería", EstadoRegistro.ACTIVO);
            when(programaRepository.existsByFacultad_IdAndEstado(existente.getId(), EstadoRegistro.ACTIVO)).thenReturn(true);

            assertThatThrownBy(() -> service.inactivar(existente.getId()))
                    .isInstanceOf(FacultadConProgramasActivosException.class)
                    .hasMessageContaining("Facultad de Ingeniería");
            assertThat(existente.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
            verify(facultadRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ya INACTIVA: es idempotente, no consulta programas ni guarda")
        void idempotente() {
            Facultad existente = registrada("Facultad de Medicina", EstadoRegistro.INACTIVO);

            FacultadResponseDTO resultado = service.inactivar(existente.getId());

            assertThat(resultado.estado()).isEqualTo(EstadoRegistro.INACTIVO);
            verifyNoInteractions(programaRepository);
            verify(facultadRepository, never()).save(any());
        }

        @Test
        @DisplayName("Facultad inexistente lanza FacultadNoEncontradaException")
        void inexistenteLanza() {
            UUID id = inexistente();

            assertThatThrownBy(() -> service.inactivar(id))
                    .isInstanceOf(FacultadNoEncontradaException.class);
            verifyNoInteractions(programaRepository);
        }
    }
}
