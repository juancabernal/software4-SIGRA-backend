package co.edu.uco.sigra.profesores;

import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.profesores.dto.ProfesorRequestDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorResponseDTO;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.profesores.exception.CorreoDuplicadoException;
import co.edu.uco.sigra.profesores.mapper.ProfesorMapper;
import co.edu.uco.sigra.profesores.repository.ProfesorRepository;
import co.edu.uco.sigra.profesores.service.impl.ProfesorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// UNIT: lógica de registro y modificación de profesores con repositorios simulados.
@ExtendWith(MockitoExtension.class)
class ProfesorRegressionTest {

    @Mock
    private ProfesorRepository profesorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Mock
    private AsignacionDocenteRepository asignacionDocenteRepository;

    private final ProfesorMapper profesorMapper = Mappers.getMapper(ProfesorMapper.class);

    private ProfesorServiceImpl service;

    private TipoDocumento tipoDocumento;
    private UUID tipoDocId;

    @BeforeEach
    void setUp() {
        service = new ProfesorServiceImpl(
                profesorRepository,
                usuarioRepository,
                tipoDocumentoRepository,
                asignacionDocenteRepository,
                profesorMapper
        );

        tipoDocId = UUID.randomUUID();
        tipoDocumento = new TipoDocumento();
        tipoDocumento.setId(tipoDocId);
        tipoDocumento.setNombre("Cédula de Ciudadanía");
    }

    @Test
    void mapperMapeaAtributosHeredadosSinExponerPasswordHash() {
        Profesor profesor = new Profesor();
        UUID id = UUID.randomUUID();
        profesor.setId(id);
        profesor.setTipoDocumento(tipoDocumento);
        profesor.setNumeroDocumento("12345678");
        profesor.setNombreCompleto("Profesor Ejemplo");
        profesor.setCorreoInstitucional("profesor@uco.net.co");
        profesor.setPasswordHash("$2a$12$hashPrivadoOculto");
        profesor.setEstado(EstadoRegistro.ACTIVO);
        profesor.setIntentosFallidos(3);
        profesor.setFechaBloqueo(LocalDateTime.now());

        ProfesorResponseDTO dto = profesorMapper.toResponseDTO(profesor);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.tipoDocumentoNombre()).isEqualTo("Cédula de Ciudadanía");
        assertThat(dto.numeroDocumento()).isEqualTo("12345678");
        assertThat(dto.nombreCompleto()).isEqualTo("Profesor Ejemplo");
        assertThat(dto.correoInstitucional()).isEqualTo("profesor@uco.net.co");
        assertThat(dto.estado()).isEqualTo("ACTIVO");
    }

    @Test
    void registrarProfesorFuncionaCorrectamenteYConservaHerencia() {
        ProfesorRequestDTO request = new ProfesorRequestDTO(
                tipoDocId,
                "12345678",
                "Profesor Nuevo",
                "nuevo.profesor@uco.net.co"
        );

        when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumento(tipoDocId, "12345678")).thenReturn(false);
        when(usuarioRepository.existsByCorreoInstitucionalIgnoreCase("nuevo.profesor@uco.net.co")).thenReturn(false);
        when(tipoDocumentoRepository.findById(tipoDocId)).thenReturn(Optional.of(tipoDocumento));
        when(profesorRepository.save(any(Profesor.class))).thenAnswer(inv -> {
            Profesor p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        ProfesorResponseDTO response = service.registrarProfesor(request);

        assertThat(response).isNotNull();
        assertThat(response.numeroDocumento()).isEqualTo("12345678");
        assertThat(response.nombreCompleto()).isEqualTo("Profesor Nuevo");
        assertThat(response.correoInstitucional()).isEqualTo("nuevo.profesor@uco.net.co");
        assertThat(response.tipoDocumentoNombre()).isEqualTo("Cédula de Ciudadanía");
    }

    @Test
    void registrarConCorreoYaUsadoDevuelveConflictoYNoGuarda() {
        ProfesorRequestDTO request = new ProfesorRequestDTO(
                tipoDocId, "12345678", "Profesor Nuevo", "usado@uco.net.co");

        when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumento(tipoDocId, "12345678")).thenReturn(false);
        when(usuarioRepository.existsByCorreoInstitucionalIgnoreCase("usado@uco.net.co")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarProfesor(request))
                .isInstanceOf(CorreoDuplicadoException.class);

        verify(profesorRepository, never()).save(any());
    }

    @Test
    void registrarConMismoCorreoEnMayusculasSeNormalizaAntesDeConsultarYDevuelveConflicto() {
        ProfesorRequestDTO request = new ProfesorRequestDTO(
                tipoDocId, "12345678", "Profesor Nuevo", "PROFESOR.EXISTENTE@UCO.NET.CO");

        when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumento(tipoDocId, "12345678")).thenReturn(false);
        // Solo coincide si el servicio consulta la versión normalizada en minúsculas.
        when(usuarioRepository.existsByCorreoInstitucionalIgnoreCase("profesor.existente@uco.net.co")).thenReturn(true);

        assertThatThrownBy(() -> service.registrarProfesor(request))
                .isInstanceOf(CorreoDuplicadoException.class);

        verify(profesorRepository, never()).save(any());
    }

    @Test
    void registrarPersisteElCorreoNormalizadoEnMinusculas() {
        ProfesorRequestDTO request = new ProfesorRequestDTO(
                tipoDocId, "12345678", "Profesor Nuevo", "  Nuevo.Profesor@UCO.NET.CO");

        when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumento(tipoDocId, "12345678")).thenReturn(false);
        when(usuarioRepository.existsByCorreoInstitucionalIgnoreCase("nuevo.profesor@uco.net.co")).thenReturn(false);
        when(tipoDocumentoRepository.findById(tipoDocId)).thenReturn(Optional.of(tipoDocumento));
        when(profesorRepository.save(any(Profesor.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrarProfesor(request);

        ArgumentCaptor<Profesor> captor = ArgumentCaptor.forClass(Profesor.class);
        verify(profesorRepository).save(captor.capture());
        assertThat(captor.getValue().getCorreoInstitucional()).isEqualTo("nuevo.profesor@uco.net.co");
    }

    @Test
    void modificarProfesorNoSobrescribeCamposDeAutenticacion() {
        UUID profesorId = UUID.randomUUID();
        Profesor existente = new Profesor();
        existente.setId(profesorId);
        existente.setNumeroDocumento("12345678");
        existente.setNombreCompleto("Nombre Anterior");
        existente.setCorreoInstitucional("anterior@uco.net.co");
        existente.setTipoDocumento(tipoDocumento);
        existente.setPasswordHash("$2a$12$hashPreexistenteQueNoDebeCambiar");
        existente.setIntentosFallidos(4);
        LocalDateTime fechaBloqueoPrevia = LocalDateTime.now();
        existente.setFechaBloqueo(fechaBloqueoPrevia);

        ProfesorRequestDTO dtoModificacion = new ProfesorRequestDTO(
                tipoDocId,
                "12345678",
                "Nombre Modificado",
                "modificado@uco.net.co"
        );

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(existente));
        when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumentoAndIdNot(tipoDocId, "12345678", profesorId))
                .thenReturn(false);
        when(usuarioRepository.existsByCorreoInstitucionalIgnoreCaseAndIdNot("modificado@uco.net.co", profesorId))
                .thenReturn(false);
        when(tipoDocumentoRepository.findById(tipoDocId)).thenReturn(Optional.of(tipoDocumento));
        when(profesorRepository.save(any(Profesor.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfesorResponseDTO response = service.modificarProfesor(profesorId, dtoModificacion);

        assertThat(response.nombreCompleto()).isEqualTo("Nombre Modificado");
        assertThat(response.correoInstitucional()).isEqualTo("modificado@uco.net.co");

        // Verificamos que los campos de autenticación de Usuario quedaron intactos
        assertThat(existente.getPasswordHash()).isEqualTo("$2a$12$hashPreexistenteQueNoDebeCambiar");
        assertThat(existente.getIntentosFallidos()).isEqualTo(4);
        assertThat(existente.getFechaBloqueo()).isEqualTo(fechaBloqueoPrevia);
        assertThat(existente.getRol()).isEqualTo(RolUsuario.PROFESOR);
    }

    @Test
    void modificarProfesorManteniendoSuPropioCorreoEsPermitido() {
        UUID profesorId = UUID.randomUUID();
        Profesor existente = new Profesor();
        existente.setId(profesorId);
        existente.setNumeroDocumento("12345678");
        existente.setNombreCompleto("Nombre");
        existente.setCorreoInstitucional("mismo@uco.net.co");
        existente.setTipoDocumento(tipoDocumento);

        ProfesorRequestDTO dto = new ProfesorRequestDTO(
                tipoDocId, "12345678", "Nombre Nuevo", "MISMO@UCO.NET.CO");

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(existente));
        when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumentoAndIdNot(tipoDocId, "12345678", profesorId))
                .thenReturn(false);
        // El propio profesor se excluye de la comprobación de unicidad.
        when(usuarioRepository.existsByCorreoInstitucionalIgnoreCaseAndIdNot("mismo@uco.net.co", profesorId))
                .thenReturn(false);
        when(tipoDocumentoRepository.findById(tipoDocId)).thenReturn(Optional.of(tipoDocumento));
        when(profesorRepository.save(any(Profesor.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfesorResponseDTO response = service.modificarProfesor(profesorId, dto);

        assertThat(response.correoInstitucional()).isEqualTo("mismo@uco.net.co");
        verify(profesorRepository).save(existente);
    }

    @Test
    void modificarProfesorConCorreoDeOtroUsuarioDevuelveConflicto() {
        UUID profesorId = UUID.randomUUID();
        Profesor existente = new Profesor();
        existente.setId(profesorId);
        existente.setNumeroDocumento("12345678");
        existente.setTipoDocumento(tipoDocumento);

        ProfesorRequestDTO dto = new ProfesorRequestDTO(
                tipoDocId, "12345678", "Nombre", "ajeno@uco.net.co");

        when(profesorRepository.findById(profesorId)).thenReturn(Optional.of(existente));
        when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumentoAndIdNot(tipoDocId, "12345678", profesorId))
                .thenReturn(false);
        when(usuarioRepository.existsByCorreoInstitucionalIgnoreCaseAndIdNot("ajeno@uco.net.co", profesorId))
                .thenReturn(true);

        assertThatThrownBy(() -> service.modificarProfesor(profesorId, dto))
                .isInstanceOf(CorreoDuplicadoException.class);

        verify(profesorRepository, never()).save(any());
    }
}
