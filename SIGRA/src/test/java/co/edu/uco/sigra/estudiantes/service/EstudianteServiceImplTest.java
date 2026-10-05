package co.edu.uco.sigra.estudiantes.service;

import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.estudiantes.dto.EstudianteRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteResponseDTO;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.exception.CorreoEstudianteDuplicadoException;
import co.edu.uco.sigra.estudiantes.exception.DocumentoNoModificableException;
import co.edu.uco.sigra.estudiantes.exception.EstudianteNoEncontradoException;
import co.edu.uco.sigra.estudiantes.exception.EstudianteYaRegistradoException;
import co.edu.uco.sigra.estudiantes.exception.ReferenciaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.mapper.EstudianteMapper;
import co.edu.uco.sigra.estudiantes.repository.EstudianteRepository;
import co.edu.uco.sigra.estudiantes.service.impl.EstudianteServiceImpl;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de estudiantes")
class EstudianteServiceImplTest {

    private static final UUID ID_ESTUDIANTE = UUID.randomUUID();
    private static final UUID ID_TIPO_DOCUMENTO = UUID.randomUUID();
    private static final UUID ID_TIPO_DOCUMENTO_OTRO = UUID.randomUUID();
    private static final String NOMBRE_TIPO_DOCUMENTO = "Cédula de Ciudadanía";
    private static final String NOMBRE_TIPO_DOCUMENTO_OTRO = "Tarjeta de Identidad";
    private static final String NUMERO_DOCUMENTO = "123456789";
    private static final String NOMBRE_COMPLETO = "Juan Pérez";
    private static final String CORREO_NORMALIZADO = "juan.perez@uco.net.co";

    @Mock
    private EstudianteRepository estudianteRepository;

    @Mock
    private TipoDocumentoRepository tipoDocumentoRepository;

    private final EstudianteMapper mapper = Mappers.getMapper(EstudianteMapper.class);

    private EstudianteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EstudianteServiceImpl(estudianteRepository, tipoDocumentoRepository, mapper);
    }

    private static TipoDocumento tipoDocumento() {
        TipoDocumento t = new TipoDocumento();
        t.setId(ID_TIPO_DOCUMENTO);
        t.setNombre(NOMBRE_TIPO_DOCUMENTO);
        return t;
    }

    private static TipoDocumento tipoDocumentoOtro() {
        TipoDocumento t = new TipoDocumento();
        t.setId(ID_TIPO_DOCUMENTO_OTRO);
        t.setNombre(NOMBRE_TIPO_DOCUMENTO_OTRO);
        return t;
    }

    private static Estudiante estudiante(EstadoRegistro estado) {
        Estudiante e = new Estudiante();
        ReflectionTestUtils.setField(e, "id", ID_ESTUDIANTE);
        e.setTipoDocumento(tipoDocumento());
        e.setNumeroDocumento(NUMERO_DOCUMENTO);
        e.setNombreCompleto(NOMBRE_COMPLETO);
        e.setCorreoInstitucional(CORREO_NORMALIZADO);
        e.setEstado(estado);
        return e;
    }

    private Estudiante existe(EstadoRegistro estado) {
        Estudiante e = estudiante(estado);
        when(estudianteRepository.findById(ID_ESTUDIANTE)).thenReturn(Optional.of(e));
        return e;
    }

    private void noExiste() {
        when(estudianteRepository.findById(ID_ESTUDIANTE)).thenReturn(Optional.empty());
    }

    private void saveDevuelveArgumento() {
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void saveDevuelveArgumentoConId() {
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(inv -> {
            Estudiante guardado = inv.getArgument(0);
            ReflectionTestUtils.setField(guardado, "id", ID_ESTUDIANTE);
            return guardado;
        });
    }

    @Nested
    @DisplayName("Registrar")
    class Registrar {

        @Test
        @DisplayName("Con datos válidos queda ACTIVO, con id asignado, correo normalizado y tipo de documento resuelto")
        void datosValidos() {
            when(estudianteRepository.existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO))
                    .thenReturn(false);
            when(estudianteRepository.existsByCorreoInstitucional(CORREO_NORMALIZADO)).thenReturn(false);
            when(tipoDocumentoRepository.findById(ID_TIPO_DOCUMENTO)).thenReturn(Optional.of(tipoDocumento()));
            saveDevuelveArgumentoConId();

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, "  Juan.Perez@UCO.NET.CO  ");

            EstudianteResponseDTO respuesta = service.registrar(dto);

            assertThat(respuesta.id()).isEqualTo(ID_ESTUDIANTE);
            assertThat(respuesta.estado()).isEqualTo(EstadoRegistro.ACTIVO.name());
            assertThat(respuesta.tipoDocumentoNombre()).isEqualTo(NOMBRE_TIPO_DOCUMENTO);
            assertThat(respuesta.correoInstitucional()).isEqualTo(CORREO_NORMALIZADO);

            ArgumentCaptor<Estudiante> captor = ArgumentCaptor.forClass(Estudiante.class);
            verify(estudianteRepository).save(captor.capture());
            assertThat(captor.getValue().getTipoDocumento().getId()).isEqualTo(ID_TIPO_DOCUMENTO);
            assertThat(captor.getValue().getCorreoInstitucional()).isEqualTo(CORREO_NORMALIZADO);
        }

        @Test
        @DisplayName("Con el documento ya registrado lanza excepción sin consultar el correo ni el catálogo, y no guarda")
        void documentoDuplicado() {
            when(estudianteRepository.existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO))
                    .thenReturn(true);

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO_NORMALIZADO);

            assertThatThrownBy(() -> service.registrar(dto))
                    .isInstanceOf(EstudianteYaRegistradoException.class)
                    .hasMessageContaining(NUMERO_DOCUMENTO);

            verify(estudianteRepository, never()).save(any());
            verify(tipoDocumentoRepository, never()).findById(any());
            verify(estudianteRepository, never()).existsByCorreoInstitucional(any());
        }

        @Test
        @DisplayName("Con el correo ya registrado lanza excepción sin consultar el catálogo, y no guarda")
        void correoDuplicado() {
            when(estudianteRepository.existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO))
                    .thenReturn(false);
            when(estudianteRepository.existsByCorreoInstitucional(CORREO_NORMALIZADO)).thenReturn(true);

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO_NORMALIZADO);

            assertThatThrownBy(() -> service.registrar(dto))
                    .isInstanceOf(CorreoEstudianteDuplicadoException.class)
                    .hasMessageContaining(CORREO_NORMALIZADO);

            verify(tipoDocumentoRepository, never()).findById(any());
            verify(estudianteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con un tipo de documento inexistente lanza excepción y no guarda")
        void tipoDocumentoInexistente() {
            when(estudianteRepository.existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO))
                    .thenReturn(false);
            when(estudianteRepository.existsByCorreoInstitucional(CORREO_NORMALIZADO)).thenReturn(false);
            when(tipoDocumentoRepository.findById(ID_TIPO_DOCUMENTO)).thenReturn(Optional.empty());

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO_NORMALIZADO);

            assertThatThrownBy(() -> service.registrar(dto))
                    .isInstanceOf(ReferenciaNoEncontradaException.class);

            verify(estudianteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con documento y correo duplicados a la vez, se valida primero el documento")
        void documentoYCorreoDuplicados() {
            when(estudianteRepository.existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO))
                    .thenReturn(true);

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO_NORMALIZADO);

            assertThatThrownBy(() -> service.registrar(dto))
                    .isInstanceOf(EstudianteYaRegistradoException.class);

            verify(estudianteRepository, never()).existsByCorreoInstitucional(any());
        }

        @Test
        @DisplayName("Con correo duplicado y tipo de documento inexistente a la vez, no llega a resolver el catálogo")
        void correoDuplicadoYTipoDocumentoInexistente() {
            when(estudianteRepository.existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO))
                    .thenReturn(false);
            when(estudianteRepository.existsByCorreoInstitucional(CORREO_NORMALIZADO)).thenReturn(true);

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO_NORMALIZADO);

            assertThatThrownBy(() -> service.registrar(dto))
                    .isInstanceOf(CorreoEstudianteDuplicadoException.class);

            verify(tipoDocumentoRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Con el mismo número de documento pero un tipo de documento distinto, no hay duplicado")
        void mismoNumeroDocumentoTipoDocumentoDistinto() {
            when(estudianteRepository.existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO_OTRO, NUMERO_DOCUMENTO))
                    .thenReturn(false);
            when(estudianteRepository.existsByCorreoInstitucional(CORREO_NORMALIZADO)).thenReturn(false);
            when(tipoDocumentoRepository.findById(ID_TIPO_DOCUMENTO_OTRO)).thenReturn(Optional.of(tipoDocumentoOtro()));
            saveDevuelveArgumentoConId();

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO_OTRO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO_NORMALIZADO);

            EstudianteResponseDTO respuesta = service.registrar(dto);

            assertThat(respuesta.tipoDocumentoNombre()).isEqualTo(NOMBRE_TIPO_DOCUMENTO_OTRO);
            verify(estudianteRepository)
                    .existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO_OTRO, NUMERO_DOCUMENTO);
        }

        @Test
        @DisplayName("Normaliza el correo a minúsculas antes de validar si ya existe")
        void correoConDistintaCapitalizacionYaExistente() {
            when(estudianteRepository.existsByTipoDocumento_IdAndNumeroDocumento(ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO))
                    .thenReturn(false);
            when(estudianteRepository.existsByCorreoInstitucional(CORREO_NORMALIZADO)).thenReturn(true);

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, "JUAN.PEREZ@UCO.NET.CO");

            assertThatThrownBy(() -> service.registrar(dto))
                    .isInstanceOf(CorreoEstudianteDuplicadoException.class);

            verify(estudianteRepository).existsByCorreoInstitucional(CORREO_NORMALIZADO);
        }
    }

    @Nested
    @DisplayName("Consultar")
    class Consultar {

        @Test
        @DisplayName("Sin filtro, invoca findAll (no la consulta con criterio) y mapea los resultados")
        void sinFiltro() {
            when(estudianteRepository.findAll())
                    .thenReturn(List.of(estudiante(EstadoRegistro.ACTIVO)));

            List<EstudianteResponseDTO> resultado = service.consultar(null);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).id()).isEqualTo(ID_ESTUDIANTE);
            verify(estudianteRepository).findAll();
            verify(estudianteRepository, never()).buscarPorNombreODocumento(any());
        }

        @Test
        @DisplayName("Con filtro en blanco, lo trata igual que nulo e invoca findAll")
        void filtroEnBlanco() {
            when(estudianteRepository.findAll())
                    .thenReturn(List.of(estudiante(EstadoRegistro.ACTIVO)));

            List<EstudianteResponseDTO> resultado = service.consultar("   ");

            assertThat(resultado).hasSize(1);
            verify(estudianteRepository).findAll();
            verify(estudianteRepository, never()).buscarPorNombreODocumento(any());
        }

        @Test
        @DisplayName("Con filtro, pasa el string exacto sin transformarlo")
        void conFiltro() {
            when(estudianteRepository.buscarPorNombreODocumento("Pérez"))
                    .thenReturn(List.of(estudiante(EstadoRegistro.ACTIVO)));

            service.consultar("Pérez");

            verify(estudianteRepository).buscarPorNombreODocumento("Pérez");
        }

        @Test
        @DisplayName("Sin coincidencias devuelve lista vacía, sin lanzar excepción")
        void sinCoincidencias() {
            when(estudianteRepository.buscarPorNombreODocumento("inexistente")).thenReturn(List.of());

            List<EstudianteResponseDTO> resultado = service.consultar("inexistente");

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("Con resultado mezclado, activos e inactivos aparecen con su estado correcto")
        void resultadoMezclado() {
            when(estudianteRepository.findAll())
                    .thenReturn(List.of(estudiante(EstadoRegistro.ACTIVO), estudiante(EstadoRegistro.INACTIVO)));

            List<EstudianteResponseDTO> resultado = service.consultar(null);

            assertThat(resultado).hasSize(2);
            assertThat(resultado)
                    .extracting(EstudianteResponseDTO::estado)
                    .containsExactly(EstadoRegistro.ACTIVO.name(), EstadoRegistro.INACTIVO.name());
        }
    }

    @Nested
    @DisplayName("Consultar por id")
    class ConsultarPorId {

        @Test
        @DisplayName("Con un id existente devuelve el DTO correspondiente")
        void existente() {
            existe(EstadoRegistro.ACTIVO);

            EstudianteResponseDTO respuesta = service.consultarPorId(ID_ESTUDIANTE);

            assertThat(respuesta.id()).isEqualTo(ID_ESTUDIANTE);
            assertThat(respuesta.numeroDocumento()).isEqualTo(NUMERO_DOCUMENTO);
            assertThat(respuesta.estado()).isEqualTo(EstadoRegistro.ACTIVO.name());
        }

        @Test
        @DisplayName("Con un id inexistente lanza excepción")
        void inexistente() {
            noExiste();

            assertThatThrownBy(() -> service.consultarPorId(ID_ESTUDIANTE))
                    .isInstanceOf(EstudianteNoEncontradoException.class)
                    .hasMessageContaining(ID_ESTUDIANTE.toString());
        }
    }

    @Nested
    @DisplayName("Inactivar")
    class Inactivar {

        @Test
        @DisplayName("Con un estudiante existente queda INACTIVO")
        void existente() {
            existe(EstadoRegistro.ACTIVO);
            saveDevuelveArgumento();

            service.inactivar(ID_ESTUDIANTE);

            ArgumentCaptor<Estudiante> captor = ArgumentCaptor.forClass(Estudiante.class);
            verify(estudianteRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoRegistro.INACTIVO);
        }

        @Test
        @DisplayName("Con un estudiante inexistente lanza excepción y no guarda")
        void inexistente() {
            noExiste();

            assertThatThrownBy(() -> service.inactivar(ID_ESTUDIANTE))
                    .isInstanceOf(EstudianteNoEncontradoException.class);

            verify(estudianteRepository, never()).save(any());
        }

        @Test
        @DisplayName("No invoca ningún método de borrado")
        void noBorraNada() {
            existe(EstadoRegistro.ACTIVO);
            saveDevuelveArgumento();

            service.inactivar(ID_ESTUDIANTE);

            verify(estudianteRepository, never()).delete(any());
            verify(estudianteRepository, never()).deleteById(any());
            verify(estudianteRepository, never()).deleteAll();
        }
    }

    @Nested
    @DisplayName("Modificar")
    class Modificar {

        @Test
        @DisplayName("Actualiza nombre y correo, deja el documento intacto, sin tocar el catálogo ni la unicidad de documento")
        void actualizaNombreYCorreoMismoDocumento() {
            existe(EstadoRegistro.ACTIVO);
            when(estudianteRepository.existsByCorreoInstitucionalAndIdNot("nuevo.correo@uco.net.co", ID_ESTUDIANTE))
                    .thenReturn(false);
            saveDevuelveArgumento();

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, "Juan Carlos Pérez", "  Nuevo.Correo@UCO.NET.CO  ");

            EstudianteResponseDTO respuesta = service.modificar(ID_ESTUDIANTE, dto);

            assertThat(respuesta.nombreCompleto()).isEqualTo("Juan Carlos Pérez");
            assertThat(respuesta.correoInstitucional()).isEqualTo("nuevo.correo@uco.net.co");
            assertThat(respuesta.numeroDocumento()).isEqualTo(NUMERO_DOCUMENTO);

            verify(tipoDocumentoRepository, never()).findById(any());
            verify(estudianteRepository, never()).existsByTipoDocumento_IdAndNumeroDocumento(any(), any());
        }

        @Test
        @DisplayName("Intentar cambiar el número de documento lanza excepción y no guarda")
        void cambioNumeroDocumento() {
            existe(EstadoRegistro.ACTIVO);

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO, "987654321", NOMBRE_COMPLETO, CORREO_NORMALIZADO);

            assertThatThrownBy(() -> service.modificar(ID_ESTUDIANTE, dto))
                    .isInstanceOf(DocumentoNoModificableException.class);

            verify(estudianteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Intentar cambiar el tipo de documento lanza excepción y no guarda")
        void cambioTipoDocumento() {
            existe(EstadoRegistro.ACTIVO);

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO_OTRO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO_NORMALIZADO);

            assertThatThrownBy(() -> service.modificar(ID_ESTUDIANTE, dto))
                    .isInstanceOf(DocumentoNoModificableException.class);

            verify(estudianteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cambiar tipo y número de documento a la vez lanza exactamente una excepción, sin NPE")
        void cambioTipoYNumeroDocumento() {
            existe(EstadoRegistro.ACTIVO);

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO_OTRO, "987654321", NOMBRE_COMPLETO, CORREO_NORMALIZADO);

            assertThatThrownBy(() -> service.modificar(ID_ESTUDIANTE, dto))
                    .isInstanceOf(DocumentoNoModificableException.class);

            verify(estudianteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con el correo ya usado por otro estudiante lanza excepción y no guarda")
        void correoYaUsadoPorOtro() {
            existe(EstadoRegistro.ACTIVO);
            when(estudianteRepository.existsByCorreoInstitucionalAndIdNot("otro@uco.net.co", ID_ESTUDIANTE))
                    .thenReturn(true);

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, "otro@uco.net.co");

            assertThatThrownBy(() -> service.modificar(ID_ESTUDIANTE, dto))
                    .isInstanceOf(CorreoEstudianteDuplicadoException.class);

            verify(estudianteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con el correo igual al propio (aunque con otra capitalización) no se considera duplicado")
        void correoIgualAlPropio() {
            existe(EstadoRegistro.ACTIVO);
            saveDevuelveArgumento();

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, "Juan.Perez@UCO.NET.CO");

            EstudianteResponseDTO respuesta = service.modificar(ID_ESTUDIANTE, dto);

            assertThat(respuesta.correoInstitucional()).isEqualTo(CORREO_NORMALIZADO);
            verify(estudianteRepository, never()).existsByCorreoInstitucionalAndIdNot(any(), any());
        }

        @Test
        @DisplayName("Con un id inexistente lanza excepción sin validar correo ni guardar")
        void idInexistente() {
            noExiste();

            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    ID_TIPO_DOCUMENTO, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO_NORMALIZADO);

            assertThatThrownBy(() -> service.modificar(ID_ESTUDIANTE, dto))
                    .isInstanceOf(EstudianteNoEncontradoException.class);

            verify(estudianteRepository, never()).existsByCorreoInstitucionalAndIdNot(any(), any());
            verify(estudianteRepository, never()).save(any());
        }
    }
}
