package co.edu.uco.sigra.programas.service;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.facultades.entity.Facultad;
import co.edu.uco.sigra.facultades.exception.FacultadInactivaException;
import co.edu.uco.sigra.facultades.exception.FacultadNoEncontradaException;
import co.edu.uco.sigra.facultades.repository.FacultadRepository;
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
    private FacultadRepository facultadRepository;

    @Mock
    private AsignaturaRepository asignaturaRepository;

    private final ProgramaMapper mapper = Mappers.getMapper(ProgramaMapper.class);

    private ProgramaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProgramaServiceImpl(programaRepository, facultadRepository, mapper, asignaturaRepository);
    }

    private static Facultad facultad(EstadoRegistro estado) {
        Facultad f = new Facultad();
        f.setId(UUID.randomUUID());
        f.setNombre("Facultad de Ingeniería");
        f.setEstado(estado);
        return f;
    }

    private Facultad facultadActivaRegistrada() {
        Facultad f = facultad(EstadoRegistro.ACTIVO);
        when(facultadRepository.findById(f.getId())).thenReturn(Optional.of(f));
        return f;
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
    @DisplayName("Registrar crea el programa activo en su facultad, sin espacios sobrantes y con el código en mayúsculas")
    void registraProgramaActivo() {
        Facultad facultad = facultadActivaRegistrada();
        when(programaRepository.existsByNombreIgnoreCase("Ingeniería de Sistemas")).thenReturn(false);
        when(programaRepository.existsByCodigoIgnoreCase("ING-SIS")).thenReturn(false);
        when(programaRepository.save(any(ProgramaAcademico.class))).thenAnswer(inv -> {
            ProgramaAcademico guardado = inv.getArgument(0);
            guardado.setId(UUID.randomUUID());
            return guardado;
        });

        ProgramaResponseDTO resultado = service.registrar(
                new ProgramaRequestDTO("  Ingeniería de Sistemas ", " ing-sis ", facultad.getId()));

        ArgumentCaptor<ProgramaAcademico> captor = ArgumentCaptor.forClass(ProgramaAcademico.class);
        verify(programaRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Ingeniería de Sistemas");
        assertThat(captor.getValue().getCodigo()).isEqualTo("ING-SIS");
        assertThat(captor.getValue().getFacultad()).isSameAs(facultad);
        assertThat(resultado.estado()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(resultado.id()).isNotNull();
        assertThat(resultado.facultadId()).isEqualTo(facultad.getId());
        assertThat(resultado.facultadNombre()).isEqualTo("Facultad de Ingeniería");
    }

    @Test
    @DisplayName("Registrar rechaza una facultad que no existe")
    void registrarRechazaFacultadInexistente() {
        UUID facultadId = UUID.randomUUID();
        when(facultadRepository.findById(facultadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(new ProgramaRequestDTO("Medicina", "MED1", facultadId)))
                .isInstanceOf(FacultadNoEncontradaException.class);
        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registrar rechaza una facultad inactiva")
    void registrarRechazaFacultadInactiva() {
        Facultad inactiva = facultad(EstadoRegistro.INACTIVO);
        when(facultadRepository.findById(inactiva.getId())).thenReturn(Optional.of(inactiva));

        assertThatThrownBy(() -> service.registrar(new ProgramaRequestDTO("Medicina", "MED1", inactiva.getId())))
                .isInstanceOf(FacultadInactivaException.class);
        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registrar rechaza un nombre que ya existe, sin distinguir mayúsculas")
    void registrarRechazaNombreDuplicado() {
        Facultad facultad = facultadActivaRegistrada();
        when(programaRepository.existsByNombreIgnoreCase("derecho")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(new ProgramaRequestDTO("derecho", "DER1", facultad.getId())))
                .isInstanceOf(NombreProgramaDuplicadoException.class);
        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registrar rechaza un código que ya existe")
    void registrarRechazaCodigoDuplicado() {
        Facultad facultad = facultadActivaRegistrada();
        when(programaRepository.existsByNombreIgnoreCase("Medicina")).thenReturn(false);
        when(programaRepository.existsByCodigoIgnoreCase("MED")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(new ProgramaRequestDTO("Medicina", "MED", facultad.getId())))
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

        ProgramaResponseDTO resultado = service.actualizar(
                existente.getId(), new ProgramaUpdateDTO(" Derecho Civil "));

        assertThat(resultado.nombre()).isEqualTo("Derecho Civil");
        assertThat(resultado.codigo()).isEqualTo("DER");
    }

    @Test
    @DisplayName("Actualizar sin facultadId conserva la facultad actual y no la consulta")
    void actualizarSinFacultadConservaLaActual() {
        ProgramaAcademico existente = programa("DER", "Derecho", EstadoRegistro.ACTIVO);
        Facultad actual = facultad(EstadoRegistro.ACTIVO);
        existente.setFacultad(actual);
        when(programaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(programaRepository.existsByNombreIgnoreCaseAndIdNot("Derecho", existente.getId())).thenReturn(false);
        when(programaRepository.save(existente)).thenReturn(existente);

        ProgramaResponseDTO resultado = service.actualizar(existente.getId(), new ProgramaUpdateDTO("Derecho", null));

        assertThat(resultado.facultadId()).isEqualTo(actual.getId());
        verify(facultadRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Actualizar con otra facultad ACTIVA mueve el programa a esa facultad")
    void actualizarMueveAOtraFacultad() {
        ProgramaAcademico existente = programa("DER", "Derecho", EstadoRegistro.ACTIVO);
        existente.setFacultad(facultad(EstadoRegistro.ACTIVO));
        Facultad nueva = facultadActivaRegistrada();
        when(programaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(programaRepository.existsByNombreIgnoreCaseAndIdNot("Derecho", existente.getId())).thenReturn(false);
        when(programaRepository.save(existente)).thenReturn(existente);

        ProgramaResponseDTO resultado = service.actualizar(existente.getId(), new ProgramaUpdateDTO("Derecho", nueva.getId()));

        assertThat(existente.getFacultad()).isSameAs(nueva);
        assertThat(resultado.facultadId()).isEqualTo(nueva.getId());
        assertThat(resultado.codigo()).isEqualTo("DER");
    }

    @Test
    @DisplayName("Actualizar reenviando la facultad actual no la valida, aunque esté inactiva")
    void actualizarConLaMismaFacultadInactiva() {
        ProgramaAcademico existente = programa("DER", "Derecho", EstadoRegistro.ACTIVO);
        Facultad inactiva = facultad(EstadoRegistro.INACTIVO);
        existente.setFacultad(inactiva);
        when(programaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(programaRepository.existsByNombreIgnoreCaseAndIdNot("Derecho Civil", existente.getId())).thenReturn(false);
        when(programaRepository.save(existente)).thenReturn(existente);

        ProgramaResponseDTO resultado = service.actualizar(
                existente.getId(), new ProgramaUpdateDTO("Derecho Civil", inactiva.getId()));

        assertThat(resultado.nombre()).isEqualTo("Derecho Civil");
        assertThat(existente.getFacultad()).isSameAs(inactiva);
        verify(facultadRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Actualizar hacia una facultad inexistente lanza no encontrada y no guarda")
    void actualizarHaciaFacultadInexistente() {
        ProgramaAcademico existente = programa("DER", "Derecho", EstadoRegistro.ACTIVO);
        Facultad actual = facultad(EstadoRegistro.ACTIVO);
        existente.setFacultad(actual);
        UUID inexistente = UUID.randomUUID();
        when(programaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(programaRepository.existsByNombreIgnoreCaseAndIdNot("Derecho", existente.getId())).thenReturn(false);
        when(facultadRepository.findById(inexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(existente.getId(), new ProgramaUpdateDTO("Derecho", inexistente)))
                .isInstanceOf(FacultadNoEncontradaException.class);
        assertThat(existente.getFacultad()).isSameAs(actual);
        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar hacia una facultad inactiva lanza FacultadInactivaException y no guarda")
    void actualizarHaciaFacultadInactiva() {
        ProgramaAcademico existente = programa("DER", "Derecho", EstadoRegistro.ACTIVO);
        existente.setFacultad(facultad(EstadoRegistro.ACTIVO));
        Facultad inactiva = facultad(EstadoRegistro.INACTIVO);
        when(programaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(programaRepository.existsByNombreIgnoreCaseAndIdNot("Derecho", existente.getId())).thenReturn(false);
        when(facultadRepository.findById(inactiva.getId())).thenReturn(Optional.of(inactiva));

        assertThatThrownBy(() -> service.actualizar(existente.getId(), new ProgramaUpdateDTO("Derecho", inactiva.getId())))
                .isInstanceOf(FacultadInactivaException.class);
        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar rechaza el nombre de otro programa")
    void actualizarRechazaNombreDeOtroPrograma() {
        ProgramaAcademico existente = programa("DER", "Derecho", EstadoRegistro.ACTIVO);
        when(programaRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(programaRepository.existsByNombreIgnoreCaseAndIdNot("Medicina", existente.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(existente.getId(), new ProgramaUpdateDTO("Medicina")))
                .isInstanceOf(NombreProgramaDuplicadoException.class);
        verify(programaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Actualizar un programa inexistente lanza no encontrado")
    void actualizarInexistente() {
        UUID id = UUID.randomUUID();
        when(programaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(id, new ProgramaUpdateDTO("Nuevo")))
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
    @DisplayName("El filtro por facultad devuelve solo los programas de esa facultad")
    void filtroPorFacultad() {
        Facultad ingenieria = facultad(EstadoRegistro.ACTIVO);
        Facultad derechoYCiencias = facultad(EstadoRegistro.ACTIVO);
        ProgramaAcademico derecho = programa("DER", "Derecho", EstadoRegistro.ACTIVO);
        derecho.setFacultad(derechoYCiencias);
        ProgramaAcademico sistemas = programa("ISIS", "Ingeniería de Sistemas", EstadoRegistro.ACTIVO);
        sistemas.setFacultad(ingenieria);
        ProgramaAcademico civil = programa("ICIV", "Ingeniería Civil", EstadoRegistro.INACTIVO);
        civil.setFacultad(ingenieria);
        when(programaRepository.findAll(Sort.by("nombre"))).thenReturn(List.of(derecho, civil, sistemas));

        assertThat(service.consultar(null, null, ingenieria.getId(), null)).extracting(ProgramaResponseDTO::codigo)
                .containsExactly("ICIV", "ISIS");
        assertThat(service.consultar(null, null, ingenieria.getId(), EstadoRegistro.ACTIVO))
                .extracting(ProgramaResponseDTO::codigo).containsExactly("ISIS");
        assertThat(service.consultar(null, null, UUID.randomUUID(), null)).isEmpty();
        assertThat(service.consultar(null, null, null, null)).hasSize(3);
    }

    @Test
    @DisplayName("Un filtro en blanco se ignora y devuelve todos")
    void filtroEnBlancoSeIgnora() {
        when(programaRepository.findAll(Sort.by("nombre"))).thenReturn(List.of(
                programa("DER", "Derecho", EstadoRegistro.ACTIVO)));

        assertThat(service.consultar("   ", "  ", null)).hasSize(1);
    }
}
