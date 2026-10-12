package co.edu.uco.sigra.profesores.service;

import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.profesores.dto.AsignaturaProfesorResponseDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorRequestDTO;
import co.edu.uco.sigra.profesores.dto.ProfesorResponseDTO;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.profesores.exception.CampoNoModificableException;
import co.edu.uco.sigra.profesores.exception.CorreoDuplicadoException;
import co.edu.uco.sigra.profesores.exception.DocumentoDuplicadoException;
import co.edu.uco.sigra.profesores.exception.ProfesorConAsignacionesActivasException;
import co.edu.uco.sigra.profesores.exception.ProfesorNoEncontradoException;
import co.edu.uco.sigra.profesores.exception.TipoDocumentoNoEncontradoException;
import co.edu.uco.sigra.profesores.mapper.ProfesorMapper;
import co.edu.uco.sigra.profesores.repository.ProfesorRepository;
import co.edu.uco.sigra.profesores.service.impl.ProfesorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

/**
 * Pruebas unitarias de {@link ProfesorServiceImpl}: reglas de negocio con los repositorios
 * simulados (Mockito), sin Spring ni base de datos. El mapper es el real generado por MapStruct.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de profesores")
class ProfesorServiceImplTest {

    private static final UUID ID_PROFESOR = UUID.randomUUID();
    private static final UUID ID_OTRO_PROFESOR = UUID.randomUUID();
    private static final UUID ID_TIPO_DOCUMENTO = UUID.randomUUID();
    private static final String NOMBRE_TIPO_DOCUMENTO = "Cédula de Ciudadanía";
    private static final String NUMERO_DOCUMENTO = "123456789";
    private static final String NOMBRE_COMPLETO = "Profesor Uno";
    private static final String CORREO = "profesor.uno@uco.net.co";

    @Mock
    private ProfesorRepository profesorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Mock
    private AsignacionDocenteRepository asignacionDocenteRepository;

    private final ProfesorMapper mapper = Mappers.getMapper(ProfesorMapper.class);

    private ProfesorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProfesorServiceImpl(
                profesorRepository, usuarioRepository, tipoDocumentoRepository,
                asignacionDocenteRepository, mapper);
    }

    // ------------------------------------------------------------------ datos de apoyo

    private static TipoDocumento tipoDocumento() {
        TipoDocumento t = new TipoDocumento();
        t.setId(ID_TIPO_DOCUMENTO);
        t.setNombre(NOMBRE_TIPO_DOCUMENTO);
        return t;
    }

    private static Profesor profesor(UUID id, String numeroDocumento, String nombre, String correo) {
        Profesor p = new Profesor();
        ReflectionTestUtils.setField(p, "id", id);
        p.setTipoDocumento(tipoDocumento());
        p.setNumeroDocumento(numeroDocumento);
        p.setNombreCompleto(nombre);
        p.setCorreoInstitucional(correo);
        p.setEstado(EstadoRegistro.ACTIVO);
        return p;
    }

    private static Profesor profesorBase() {
        return profesor(ID_PROFESOR, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO);
    }

    private Profesor existe() {
        Profesor p = profesorBase();
        when(profesorRepository.findById(ID_PROFESOR)).thenReturn(Optional.of(p));
        return p;
    }

    private static Asignatura asignatura(UUID id, String codigo, String nombre) {
        Asignatura a = new Asignatura(codigo, nombre, null);
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    private static AsignacionDocente asignacion(Profesor p, Asignatura a, EstadoRegistro estado) {
        AsignacionDocente ad = new AsignacionDocente();
        ad.setId(UUID.randomUUID());
        ad.setProfesor(p);
        ad.setAsignatura(a);
        ad.setEstado(estado);
        return ad;
    }

    // ------------------------------------------------------------------ registrar

    @Nested
    @DisplayName("Registrar")
    class Registrar {

        @Test
        @DisplayName("Con datos válidos queda ACTIVO, con id, correo normalizado y tipo de documento resuelto")
        void datosValidos() {
            when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO))
                    .thenReturn(false);
            when(usuarioRepository.existsByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(false);
            when(tipoDocumentoRepository.findById(ID_TIPO_DOCUMENTO)).thenReturn(Optional.of(tipoDocumento()));
            when(profesorRepository.save(any(Profesor.class))).thenAnswer(inv -> {
                Profesor guardado = inv.getArgument(0);
                ReflectionTestUtils.setField(guardado, "id", ID_PROFESOR);
                return guardado;
            });

            ProfesorRequestDTO dto = new ProfesorRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, "  Profesor.Uno@UCO.NET.CO  ");

            ProfesorResponseDTO respuesta = service.registrarProfesor(dto);

            assertThat(respuesta.id()).isEqualTo(ID_PROFESOR);
            assertThat(respuesta.estado()).isEqualTo(EstadoRegistro.ACTIVO.name());
            assertThat(respuesta.tipoDocumentoNombre()).isEqualTo(NOMBRE_TIPO_DOCUMENTO);
            assertThat(respuesta.numeroDocumento()).isEqualTo(NUMERO_DOCUMENTO);
            assertThat(respuesta.nombreCompleto()).isEqualTo(NOMBRE_COMPLETO);
            assertThat(respuesta.correoInstitucional()).isEqualTo(CORREO);
            assertThat(respuesta.tieneAsignacionesActivas()).isFalse();

            ArgumentCaptor<Profesor> captor = ArgumentCaptor.forClass(Profesor.class);
            verify(profesorRepository).save(captor.capture());
            Profesor persistido = captor.getValue();
            assertThat(persistido.getCorreoInstitucional()).isEqualTo(CORREO);
            assertThat(persistido.getTipoDocumento().getId()).isEqualTo(ID_TIPO_DOCUMENTO);
            assertThat(persistido.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
        }

        @Test
        @DisplayName("Documento repetido para el mismo tipo lanza DocumentoDuplicadoException y no guarda")
        void documentoDuplicado() {
            when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO))
                    .thenReturn(true);

            ProfesorRequestDTO dto = new ProfesorRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO);

            assertThatThrownBy(() -> service.registrarProfesor(dto))
                    .isInstanceOf(DocumentoDuplicadoException.class)
                    .hasMessageContaining(NUMERO_DOCUMENTO);

            verify(profesorRepository, never()).save(any());
            verifyNoInteractions(usuarioRepository, tipoDocumentoRepository);
        }

        @Test
        @DisplayName("Correo ya usado (sin importar mayúsculas) lanza CorreoDuplicadoException y no guarda")
        void correoDuplicado() {
            when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO))
                    .thenReturn(false);
            when(usuarioRepository.existsByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(true);

            ProfesorRequestDTO dto = new ProfesorRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, "PROFESOR.UNO@UCO.NET.CO");

            assertThatThrownBy(() -> service.registrarProfesor(dto))
                    .isInstanceOf(CorreoDuplicadoException.class)
                    .hasMessageContaining(CORREO);

            verify(profesorRepository, never()).save(any());
            verifyNoInteractions(tipoDocumentoRepository);
        }

        @Test
        @DisplayName("Tipo de documento inexistente lanza TipoDocumentoNoEncontradoException y no guarda")
        void tipoDocumentoInexistente() {
            when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO))
                    .thenReturn(false);
            when(usuarioRepository.existsByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(false);
            when(tipoDocumentoRepository.findById(ID_TIPO_DOCUMENTO)).thenReturn(Optional.empty());

            ProfesorRequestDTO dto = new ProfesorRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO);

            assertThatThrownBy(() -> service.registrarProfesor(dto))
                    .isInstanceOf(TipoDocumentoNoEncontradoException.class);

            verify(profesorRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ consultar

    @Nested
    @DisplayName("Consultar")
    class Consultar {

        private Profesor uno;
        private Profesor dos;

        @BeforeEach
        void datos() {
            uno = profesor(ID_PROFESOR, "111111111", "Ana Gómez", "ana.gomez@uco.net.co");
            dos = profesor(ID_OTRO_PROFESOR, "222222222", "Luis Pérez", "luis.perez@uco.net.co");
        }

        @Test
        @DisplayName("Sin filtro devuelve todos e indica quién tiene asignaciones activas")
        void sinFiltro() {
            when(profesorRepository.findAll()).thenReturn(List.of(uno, dos));
            when(asignacionDocenteRepository.existsByProfesor_IdAndEstado(ID_PROFESOR, EstadoRegistro.ACTIVO))
                    .thenReturn(true);
            when(asignacionDocenteRepository.existsByProfesor_IdAndEstado(ID_OTRO_PROFESOR, EstadoRegistro.ACTIVO))
                    .thenReturn(false);

            List<ProfesorResponseDTO> resultado = service.consultar(null);

            assertThat(resultado).hasSize(2);
            assertThat(resultado.get(0).id()).isEqualTo(ID_PROFESOR);
            assertThat(resultado.get(0).tieneAsignacionesActivas()).isTrue();
            assertThat(resultado.get(1).id()).isEqualTo(ID_OTRO_PROFESOR);
            assertThat(resultado.get(1).tieneAsignacionesActivas()).isFalse();
            assertThat(resultado.get(0).tipoDocumentoNombre()).isEqualTo(NOMBRE_TIPO_DOCUMENTO);
        }

        @Test
        @DisplayName("Filtra por nombre sin distinguir mayúsculas")
        void filtroPorNombre() {
            when(profesorRepository.findAll()).thenReturn(List.of(uno, dos));
            when(asignacionDocenteRepository.existsByProfesor_IdAndEstado(ID_OTRO_PROFESOR, EstadoRegistro.ACTIVO))
                    .thenReturn(false);

            List<ProfesorResponseDTO> resultado = service.consultar("LUIS");

            assertThat(resultado).extracting(ProfesorResponseDTO::id).containsExactly(ID_OTRO_PROFESOR);
        }

        @Test
        @DisplayName("Filtra por número de documento")
        void filtroPorDocumento() {
            when(profesorRepository.findAll()).thenReturn(List.of(uno, dos));
            when(asignacionDocenteRepository.existsByProfesor_IdAndEstado(ID_PROFESOR, EstadoRegistro.ACTIVO))
                    .thenReturn(false);

            List<ProfesorResponseDTO> resultado = service.consultar("111111");

            assertThat(resultado).extracting(ProfesorResponseDTO::id).containsExactly(ID_PROFESOR);
        }

        @Test
        @DisplayName("Sin coincidencias devuelve lista vacía y no consulta asignaciones")
        void sinCoincidencias() {
            when(profesorRepository.findAll()).thenReturn(List.of(uno, dos));

            List<ProfesorResponseDTO> resultado = service.consultar("zzz-no-existe");

            assertThat(resultado).isEmpty();
            verifyNoInteractions(asignacionDocenteRepository);
        }
    }

    // ------------------------------------------------------------------ consultar asignaturas

    @Nested
    @DisplayName("Consultar asignaturas de un profesor")
    class ConsultarAsignaturas {

        private static final UUID ID_ASIGNATURA = UUID.randomUUID();

        @Test
        @DisplayName("Profesor inexistente lanza ProfesorNoEncontradoException")
        void profesorInexistente() {
            when(profesorRepository.existsById(ID_PROFESOR)).thenReturn(false);

            assertThatThrownBy(() -> service.consultarAsignaturas(ID_PROFESOR, null))
                    .isInstanceOf(ProfesorNoEncontradoException.class)
                    .hasMessageContaining(ID_PROFESOR.toString());

            verifyNoInteractions(asignacionDocenteRepository);
        }

        @Test
        @DisplayName("Sin estado devuelve todas las asignaciones con sus datos de la asignatura")
        void sinEstado() {
            Profesor p = profesorBase();
            AsignacionDocente asignacion = asignacion(p, asignatura(ID_ASIGNATURA, "MAT-101", "Cálculo"),
                    EstadoRegistro.ACTIVO);
            when(profesorRepository.existsById(ID_PROFESOR)).thenReturn(true);
            when(asignacionDocenteRepository.findByProfesor_Id(ID_PROFESOR)).thenReturn(List.of(asignacion));

            List<AsignaturaProfesorResponseDTO> resultado = service.consultarAsignaturas(ID_PROFESOR, null);

            assertThat(resultado).hasSize(1);
            AsignaturaProfesorResponseDTO dto = resultado.get(0);
            assertThat(dto.asignacionId()).isEqualTo(asignacion.getId());
            assertThat(dto.asignaturaId()).isEqualTo(ID_ASIGNATURA);
            assertThat(dto.codigoAsignatura()).isEqualTo("MAT-101");
            assertThat(dto.nombreAsignatura()).isEqualTo("Cálculo");
            assertThat(dto.estadoAsignatura()).isEqualTo(EstadoRegistro.ACTIVO);
            verify(asignacionDocenteRepository, never()).findByProfesor_IdAndEstado(any(), any());
        }

        @Test
        @DisplayName("Con estado consulta solo las asignaciones de ese estado")
        void conEstado() {
            Profesor p = profesorBase();
            AsignacionDocente inactiva = asignacion(p, asignatura(ID_ASIGNATURA, "FIS-201", "Física"),
                    EstadoRegistro.INACTIVO);
            when(profesorRepository.existsById(ID_PROFESOR)).thenReturn(true);
            when(asignacionDocenteRepository.findByProfesor_IdAndEstado(ID_PROFESOR, EstadoRegistro.INACTIVO))
                    .thenReturn(List.of(inactiva));

            List<AsignaturaProfesorResponseDTO> resultado =
                    service.consultarAsignaturas(ID_PROFESOR, EstadoRegistro.INACTIVO);

            assertThat(resultado).extracting(AsignaturaProfesorResponseDTO::codigoAsignatura)
                    .containsExactly("FIS-201");
            verify(asignacionDocenteRepository, never()).findByProfesor_Id(any());
        }

        @Test
        @DisplayName("Profesor sin asignaciones devuelve lista vacía")
        void sinAsignaciones() {
            when(profesorRepository.existsById(ID_PROFESOR)).thenReturn(true);
            when(asignacionDocenteRepository.findByProfesor_Id(ID_PROFESOR)).thenReturn(List.of());

            assertThat(service.consultarAsignaturas(ID_PROFESOR, null)).isEmpty();
        }
    }

    // ------------------------------------------------------------------ modificar

    @Nested
    @DisplayName("Modificar")
    class Modificar {

        @Test
        @DisplayName("Actualiza nombre y correo (normalizado) y conserva el documento")
        void datosValidos() {
            Profesor existente = existe();
            when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumentoAndIdNot(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, ID_PROFESOR)).thenReturn(false);
            when(usuarioRepository.existsByCorreoInstitucionalIgnoreCaseAndIdNot("nuevo@uco.net.co", ID_PROFESOR))
                    .thenReturn(false);
            when(tipoDocumentoRepository.findById(ID_TIPO_DOCUMENTO)).thenReturn(Optional.of(tipoDocumento()));
            when(profesorRepository.save(any(Profesor.class))).thenAnswer(inv -> inv.getArgument(0));

            ProfesorRequestDTO dto = new ProfesorRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, "Profesor Modificado", "  Nuevo@UCO.NET.CO ");

            ProfesorResponseDTO respuesta = service.modificarProfesor(ID_PROFESOR, dto);

            assertThat(respuesta.id()).isEqualTo(ID_PROFESOR);
            assertThat(respuesta.nombreCompleto()).isEqualTo("Profesor Modificado");
            assertThat(respuesta.correoInstitucional()).isEqualTo("nuevo@uco.net.co");
            assertThat(respuesta.numeroDocumento()).isEqualTo(NUMERO_DOCUMENTO);
            assertThat(existente.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
            verify(profesorRepository).save(existente);
        }

        @Test
        @DisplayName("Profesor inexistente lanza ProfesorNoEncontradoException")
        void profesorInexistente() {
            when(profesorRepository.findById(ID_PROFESOR)).thenReturn(Optional.empty());

            ProfesorRequestDTO dto = new ProfesorRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO);

            assertThatThrownBy(() -> service.modificarProfesor(ID_PROFESOR, dto))
                    .isInstanceOf(ProfesorNoEncontradoException.class);

            verify(profesorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cambiar el número de documento lanza CampoNoModificableException y no guarda")
        void documentoNoModificable() {
            Profesor existente = existe();

            ProfesorRequestDTO dto = new ProfesorRequestDTO(
                    ID_TIPO_DOCUMENTO, "999999999", NOMBRE_COMPLETO, CORREO);

            assertThatThrownBy(() -> service.modificarProfesor(ID_PROFESOR, dto))
                    .isInstanceOf(CampoNoModificableException.class)
                    .hasMessageContaining("numeroDocumento");

            assertThat(existente.getNumeroDocumento()).isEqualTo(NUMERO_DOCUMENTO);
            verify(profesorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Documento que ya tiene otro profesor lanza DocumentoDuplicadoException")
        void documentoDuplicado() {
            existe();
            when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumentoAndIdNot(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, ID_PROFESOR)).thenReturn(true);

            ProfesorRequestDTO dto = new ProfesorRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO);

            assertThatThrownBy(() -> service.modificarProfesor(ID_PROFESOR, dto))
                    .isInstanceOf(DocumentoDuplicadoException.class);

            verify(profesorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Correo que ya tiene otro usuario lanza CorreoDuplicadoException")
        void correoDuplicado() {
            existe();
            when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumentoAndIdNot(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, ID_PROFESOR)).thenReturn(false);
            when(usuarioRepository.existsByCorreoInstitucionalIgnoreCaseAndIdNot("otro@uco.net.co", ID_PROFESOR))
                    .thenReturn(true);

            ProfesorRequestDTO dto = new ProfesorRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, "OTRO@UCO.NET.CO");

            assertThatThrownBy(() -> service.modificarProfesor(ID_PROFESOR, dto))
                    .isInstanceOf(CorreoDuplicadoException.class)
                    .hasMessageContaining("otro@uco.net.co");

            verify(profesorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Tipo de documento inexistente lanza TipoDocumentoNoEncontradoException")
        void tipoDocumentoInexistente() {
            existe();
            when(profesorRepository.existsByTipoDocumento_IdAndNumeroDocumentoAndIdNot(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, ID_PROFESOR)).thenReturn(false);
            when(usuarioRepository.existsByCorreoInstitucionalIgnoreCaseAndIdNot(CORREO, ID_PROFESOR))
                    .thenReturn(false);
            when(tipoDocumentoRepository.findById(ID_TIPO_DOCUMENTO)).thenReturn(Optional.empty());

            ProfesorRequestDTO dto = new ProfesorRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO);

            assertThatThrownBy(() -> service.modificarProfesor(ID_PROFESOR, dto))
                    .isInstanceOf(TipoDocumentoNoEncontradoException.class);

            verify(profesorRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ inactivar

    @Nested
    @DisplayName("Inactivar")
    class Inactivar {

        @Test
        @DisplayName("Sin asignaciones activas queda INACTIVO y se guarda")
        void sinAsignacionesActivas() {
            Profesor existente = existe();
            when(asignacionDocenteRepository.existsByProfesor_IdAndEstado(ID_PROFESOR, EstadoRegistro.ACTIVO))
                    .thenReturn(false);

            service.inactivar(ID_PROFESOR);

            assertThat(existente.getEstado()).isEqualTo(EstadoRegistro.INACTIVO);
            verify(profesorRepository).save(existente);
        }

        @Test
        @DisplayName("Con asignaciones activas lanza ProfesorConAsignacionesActivasException y sigue ACTIVO")
        void conAsignacionesActivas() {
            Profesor existente = existe();
            when(asignacionDocenteRepository.existsByProfesor_IdAndEstado(ID_PROFESOR, EstadoRegistro.ACTIVO))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.inactivar(ID_PROFESOR))
                    .isInstanceOf(ProfesorConAsignacionesActivasException.class);

            assertThat(existente.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
            verify(profesorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Profesor inexistente lanza ProfesorNoEncontradoException")
        void profesorInexistente() {
            when(profesorRepository.findById(ID_PROFESOR)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.inactivar(ID_PROFESOR))
                    .isInstanceOf(ProfesorNoEncontradoException.class);

            verifyNoInteractions(asignacionDocenteRepository);
            verify(profesorRepository, never()).save(any());
        }
    }
}