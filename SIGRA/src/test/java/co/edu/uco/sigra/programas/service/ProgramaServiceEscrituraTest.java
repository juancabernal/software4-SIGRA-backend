package co.edu.uco.sigra.programas.service;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.programas.dto.ProgramaRequestDTO;
import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.dto.ProgramaUpdateDTO;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.exception.CodigoProgramaDuplicadoException;
import co.edu.uco.sigra.programas.exception.NombreProgramaDuplicadoException;
import co.edu.uco.sigra.programas.exception.ProgramaAcademicoNoEncontradoException;
import co.edu.uco.sigra.programas.exception.ProgramaConAsignaturasActivasException;
import co.edu.uco.sigra.programas.mapper.ProgramaMapper;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.programas.service.impl.ProgramaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de programas académicos: registro, modificación, inactivación y filtros")
class ProgramaServiceEscrituraTest {

    @Mock
    private ProgramaAcademicoRepository programaRepository;

    @Mock
    private AsignaturaRepository asignaturaRepository;

    private final ProgramaMapper mapper = Mappers.getMapper(ProgramaMapper.class);

    private ProgramaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProgramaServiceImpl(programaRepository, mapper, asignaturaRepository);
    }

    private static ProgramaAcademico programa(String codigo, String nombre, EstadoRegistro estado) {
        ProgramaAcademico p = new ProgramaAcademico();
        p.setId(UUID.randomUUID());
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setEstado(estado);
        return p;
    }

    @Test
    @DisplayName("Registrar crea el programa activo con el nombre y código sin espacios sobrantes")
    void registraProgramaActivo() {
        when(programaRepository.existsByNombreIgnoreCase("Ingeniería de Sistemas")).thenReturn(false);
        when(programaRepository.existsByCodigoIgnoreCase("ING-SIS")).thenReturn(false);
        when(programaRepository.save(any(ProgramaAcademico.class))).thenAnswer(inv -> {
            ProgramaAcademico guardado = inv.getArgument(0);
            guardado.setId(UUID.randomUUID());
            return guardado;
        });

        ProgramaResponseDTO resultado = service.registrar(
                new ProgramaRequestDTO("  Ingeniería de Sistemas ", " ING-SIS "));

        ArgumentCaptor<ProgramaAcademico> captor = ArgumentCaptor.forClass(ProgramaAcademico.class);
        verify(programaRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Ingeniería de Sistemas");
        assertThat(captor.getValue().getCodigo()).isEqualTo("ING-SIS");
        assertThat(resultado.estado()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(resultado.id()).isNotNull();
    }

    @Test
    @DisplayName("Registrar rechaza un nombre que ya existe, sin distinguir mayúsculas")
    void registrarRechazaNombreDuplicado() {
        when(programaRepository.existsByNombreIgnoreCase("derecho")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(new ProgramaRequestDTO("derecho", "DER1")))
                .isInstanceOf(NombreProgramaDuplicadoException.class);
        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registrar rechaza un código que ya existe")
    void registrarRechazaCodigoDuplicado() {
        when(programaRepository.existsByNombreIgnoreCase("Medicina")).thenReturn(false);
        when(programaRepository.existsByCodigoIgnoreCase("MED")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(new ProgramaRequestDTO("Medicina", "MED")))
                .isInstanceOf(CodigoProgramaDuplicadoException.class);
        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar cambia solo el nombre y conserva el código")
    void actualizaSoloNombre() {
        ProgramaAcademico existente = programa("DER", "Derecho", EstadoRegistro.ACTIVO);
        when(programaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(programaRepository.existsByNombreIgnoreCaseAndIdNot("Derecho Civil", existente.getId())).thenReturn(false);
        when(programaRepository.save(existente)).thenReturn(existente);

        ProgramaResponseDTO resultado = service.actualizarNombre(
                existente.getId(), new ProgramaUpdateDTO(" Derecho Civil "));

        assertThat(resultado.nombre()).isEqualTo("Derecho Civil");
        assertThat(resultado.codigo()).isEqualTo("DER");
    }

    @Test
    @DisplayName("Actualizar rechaza el nombre de otro programa")
    void actualizarRechazaNombreDeOtroPrograma() {
        ProgramaAcademico existente = programa("DER", "Derecho", EstadoRegistro.ACTIVO);
        when(programaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(programaRepository.existsByNombreIgnoreCaseAndIdNot("Medicina", existente.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.actualizarNombre(existente.getId(), new ProgramaUpdateDTO("Medicina")))
                .isInstanceOf(NombreProgramaDuplicadoException.class);
        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar un programa inexistente lanza no encontrado")
    void actualizarInexistente() {
        UUID id = UUID.randomUUID();
        when(programaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizarNombre(id, new ProgramaUpdateDTO("Nuevo")))
                .isInstanceOf(ProgramaAcademicoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Inactivar pasa el programa a INACTIVO sin borrarlo")
    void inactivaPrograma() {
        ProgramaAcademico existente = programa("MED", "Medicina", EstadoRegistro.ACTIVO);
        when(programaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(asignaturaRepository.existsByPrograma_IdAndEstado(existente.getId(), EstadoAsignatura.ACTIVA))
                .thenReturn(false);
        when(programaRepository.save(existente)).thenReturn(existente);

        ProgramaResponseDTO resultado = service.inactivar(existente.getId());

        assertThat(resultado.estado()).isEqualTo(EstadoRegistro.INACTIVO);
        verify(programaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Inactivar un programa que ya está inactivo no vuelve a validar ni a guardar")
    void inactivarIdempotente() {
        ProgramaAcademico existente = programa("MED", "Medicina", EstadoRegistro.INACTIVO);
        when(programaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

        ProgramaResponseDTO resultado = service.inactivar(existente.getId());

        assertThat(resultado.estado()).isEqualTo(EstadoRegistro.INACTIVO);
        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Inactivar rechaza el programa si tiene asignaturas activas")
    void inactivarConAsignaturasActivas() {
        ProgramaAcademico existente = programa("ISIS", "Ingeniería de Sistemas", EstadoRegistro.ACTIVO);
        when(programaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(asignaturaRepository.existsByPrograma_IdAndEstado(existente.getId(), EstadoAsignatura.ACTIVA))
                .thenReturn(true);

        assertThatThrownBy(() -> service.inactivar(existente.getId()))
                .isInstanceOf(ProgramaConAsignaturasActivasException.class);
        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Inactivar un programa inexistente lanza no encontrado")
    void inactivarInexistente() {
        UUID id = UUID.randomUUID();
        when(programaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inactivar(id))
                .isInstanceOf(ProgramaAcademicoNoEncontradoException.class);
    }

    @Test
    @DisplayName("Consultar por id devuelve el programa")
    void consultaPorId() {
        ProgramaAcademico existente = programa("DER", "Derecho", EstadoRegistro.ACTIVO);
        when(programaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

        assertThat(service.consultarPorId(existente.getId()).codigo()).isEqualTo("DER");
    }

    @Test
    @DisplayName("El filtro por nombre y código busca por contenido, sin distinguir mayúsculas")
    void filtroPorNombreYCodigoParcial() {
        ProgramaAcademico derecho = programa("DER", "Derecho", EstadoRegistro.ACTIVO);
        ProgramaAcademico ingenieria = programa("ISIS", "Ingeniería de Sistemas", EstadoRegistro.ACTIVO);
        when(programaRepository.findAll(Sort.by("nombre"))).thenReturn(List.of(derecho, ingenieria));

        assertThat(service.consultar("SISTEM", null, null)).extracting(ProgramaResponseDTO::codigo)
                .containsExactly("ISIS");
        assertThat(service.consultar(null, "de", null)).extracting(ProgramaResponseDTO::codigo)
                .containsExactly("DER");
    }

    @Test
    @DisplayName("Un filtro en blanco se ignora y devuelve todos")
    void filtroEnBlancoSeIgnora() {
        when(programaRepository.findAll(Sort.by("nombre"))).thenReturn(List.of(
                programa("DER", "Derecho", EstadoRegistro.ACTIVO)));

        assertThat(service.consultar("   ", "  ", null)).hasSize(1);
    }
}
