package co.edu.uco.sigra.resultadosaprendizaje.service;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeActualizarRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeCrearRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.exception.CodigoResultadoAprendizajeDuplicadoException;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeNoEncontradoException;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeReglaNegocioException;
import co.edu.uco.sigra.resultadosaprendizaje.mapper.ResultadoAprendizajeMapper;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.resultadosaprendizaje.service.impl.ResultadoAprendizajeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de resultados de aprendizaje (RF-06).
 * Los repositorios se simulan con Mockito; el mapper es el real generado por MapStruct.
 * Cada regla de negocio se prueba en su caso permitido y en su caso rechazado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de resultados de aprendizaje")
class ResultadoAprendizajeServiceImplTest {

    private static final UUID ID_ASIGNATURA = UUID.randomUUID();
    private static final UUID ID_RA = UUID.randomUUID();
    private static final String CODIGO = "RA-01";
    private static final String DESCRIPCION = "Analiza los requisitos de un sistema";

    @Mock
    private ResultadoAprendizajeRepository resultadoAprendizajeRepository;

    @Mock
    private AsignaturaRepository asignaturaRepository;

    private final ResultadoAprendizajeMapper mapper = Mappers.getMapper(ResultadoAprendizajeMapper.class);

    private ResultadoAprendizajeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ResultadoAprendizajeServiceImpl(resultadoAprendizajeRepository, asignaturaRepository, mapper);
    }

    // ------------------------------------------------------------------ datos de prueba

    /** Asignatura en el estado pedido, usando sus propios métodos de dominio para llegar a él. */
    private static Asignatura asignatura(EstadoAsignatura estado) {
        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setCodigo("ISIS");
        programa.setNombre("Ingeniería de Sistemas");

        Asignatura a = new Asignatura("ISW4", "Ingeniería de Software IV", programa);
        ReflectionTestUtils.setField(a, "id", ID_ASIGNATURA);
        if (estado != EstadoAsignatura.BORRADOR) {
            a.activar(Asignatura.MIN_RA_ACTIVOS);
        }
        if (estado == EstadoAsignatura.INACTIVA) {
            a.inactivar();
        }
        return a;
    }

    private static ResultadoAprendizaje resultado(Asignatura asignatura, EstadoRegistro estado) {
        ResultadoAprendizaje ra = new ResultadoAprendizaje();
        ra.setId(ID_RA);
        ra.setAsignatura(asignatura);
        ra.setCodigo(CODIGO);
        ra.setDescripcion(DESCRIPCION);
        ra.setEstado(estado);
        return ra;
    }

    private void asignaturaExiste(Asignatura asignatura) {
        when(asignaturaRepository.findById(ID_ASIGNATURA)).thenReturn(Optional.of(asignatura));
    }

    private void asignaturaNoExiste() {
        when(asignaturaRepository.findById(ID_ASIGNATURA)).thenReturn(Optional.empty());
    }

    private ResultadoAprendizaje resultadoExiste(EstadoAsignatura estadoAsignatura, EstadoRegistro estadoRa) {
        ResultadoAprendizaje ra = resultado(asignatura(estadoAsignatura), estadoRa);
        when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(ra));
        return ra;
    }

    private void resultadoNoExiste() {
        when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.empty());
    }

    private void raActivos(long cantidad) {
        when(resultadoAprendizajeRepository.countByAsignatura_IdAndEstado(ID_ASIGNATURA, EstadoRegistro.ACTIVO))
                .thenReturn(cantidad);
    }

    private void codigoExiste(String codigo, boolean existe) {
        when(resultadoAprendizajeRepository.existsByAsignatura_IdAndCodigo(ID_ASIGNATURA, codigo)).thenReturn(existe);
    }

    private void saveDevuelveArgumento() {
        when(resultadoAprendizajeRepository.save(any(ResultadoAprendizaje.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------ RF-06a

    @Nested
    @DisplayName("Registrar (RF-06a)")
    class Registrar {

        @Test
        @DisplayName("Con datos válidos queda ACTIVO, asociado a la asignatura, con código en mayúsculas y textos sin espacios sobrantes")
        void datosValidos() {
            asignaturaExiste(asignatura(EstadoAsignatura.BORRADOR));
            codigoExiste(CODIGO, false);
            raActivos(0);
            saveDevuelveArgumento();

            ResultadoAprendizajeResponseDTO respuesta = service.registrar(ID_ASIGNATURA,
                    new ResultadoAprendizajeCrearRequestDTO("  ra-01 ", "  " + DESCRIPCION + "  "));

            assertThat(respuesta.codigo()).isEqualTo(CODIGO);
            assertThat(respuesta.descripcion()).isEqualTo(DESCRIPCION);
            assertThat(respuesta.estado()).isEqualTo(EstadoRegistro.ACTIVO.name());
            assertThat(respuesta.asignaturaId()).isEqualTo(ID_ASIGNATURA);

            ArgumentCaptor<ResultadoAprendizaje> guardado = ArgumentCaptor.forClass(ResultadoAprendizaje.class);
            verify(resultadoAprendizajeRepository).save(guardado.capture());
            assertThat(guardado.getValue().getAsignatura().getId()).isEqualTo(ID_ASIGNATURA);
            assertThat(guardado.getValue().getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
        }

        @Test
        @DisplayName("En una asignatura ACTIVA con 6 RA activos se permite registrar el séptimo")
        void asignaturaActivaConSeis() {
            asignaturaExiste(asignatura(EstadoAsignatura.ACTIVA));
            codigoExiste(CODIGO, false);
            raActivos(6);
            saveDevuelveArgumento();

            ResultadoAprendizajeResponseDTO respuesta = service.registrar(ID_ASIGNATURA,
                    new ResultadoAprendizajeCrearRequestDTO(CODIGO, DESCRIPCION));

            assertThat(respuesta.estado()).isEqualTo(EstadoRegistro.ACTIVO.name());
        }

        @Test
        @DisplayName("Normaliza el código antes de validar si ya existe")
        void normalizaAntesDeValidarDuplicado() {
            asignaturaExiste(asignatura(EstadoAsignatura.BORRADOR));
            codigoExiste(CODIGO, true);

            assertThatThrownBy(() -> service.registrar(ID_ASIGNATURA,
                    new ResultadoAprendizajeCrearRequestDTO(" ra-01 ", DESCRIPCION)))
                    .isInstanceOf(CodigoResultadoAprendizajeDuplicadoException.class)
                    .hasMessageContaining(CODIGO);
            verify(resultadoAprendizajeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con 7 RA activos falla indicando el máximo y no guarda")
        void maximoAlcanzado() {
            asignaturaExiste(asignatura(EstadoAsignatura.ACTIVA));
            codigoExiste(CODIGO, false);
            raActivos(Asignatura.MAX_RA_ACTIVOS);

            assertThatThrownBy(() -> service.registrar(ID_ASIGNATURA,
                    new ResultadoAprendizajeCrearRequestDTO(CODIGO, DESCRIPCION)))
                    .isInstanceOf(ResultadoAprendizajeReglaNegocioException.class)
                    .hasMessageContaining("máximo");
            verify(resultadoAprendizajeRepository, never()).save(any());
        }

        @Test
        @DisplayName("En una asignatura INACTIVA falla y no guarda")
        void asignaturaInactiva() {
            asignaturaExiste(asignatura(EstadoAsignatura.INACTIVA));

            assertThatThrownBy(() -> service.registrar(ID_ASIGNATURA,
                    new ResultadoAprendizajeCrearRequestDTO(CODIGO, DESCRIPCION)))
                    .isInstanceOf(ResultadoAprendizajeReglaNegocioException.class)
                    .hasMessageContaining("inactiva");
            verify(resultadoAprendizajeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con una asignatura inexistente falla y no guarda")
        void asignaturaInexistente() {
            asignaturaNoExiste();

            assertThatThrownBy(() -> service.registrar(ID_ASIGNATURA,
                    new ResultadoAprendizajeCrearRequestDTO(CODIGO, DESCRIPCION)))
                    .isInstanceOf(AsignaturaNoEncontradaException.class);
            verify(resultadoAprendizajeRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ RF-06b

    @Nested
    @DisplayName("Consultar (RF-06b)")
    class Consultar {

        @Test
        @DisplayName("Por asignatura sin filtro devuelve todos sus RA")
        void porAsignaturaSinFiltro() {
            Asignatura a = asignatura(EstadoAsignatura.ACTIVA);
            asignaturaExiste(a);
            when(resultadoAprendizajeRepository.findByAsignatura_IdOrderByCodigoAsc(ID_ASIGNATURA))
                    .thenReturn(List.of(resultado(a, EstadoRegistro.ACTIVO), resultado(a, EstadoRegistro.INACTIVO)));

            List<ResultadoAprendizajeResponseDTO> respuesta = service.consultarPorAsignatura(ID_ASIGNATURA, null);

            assertThat(respuesta).hasSize(2);
            verify(resultadoAprendizajeRepository, never()).findByAsignatura_IdAndEstadoOrderByCodigoAsc(any(), any());
        }

        @Test
        @DisplayName("Por asignatura con filtro de estado usa la consulta filtrada")
        void porAsignaturaConFiltro() {
            Asignatura a = asignatura(EstadoAsignatura.ACTIVA);
            asignaturaExiste(a);
            when(resultadoAprendizajeRepository.findByAsignatura_IdAndEstadoOrderByCodigoAsc(ID_ASIGNATURA, EstadoRegistro.ACTIVO))
                    .thenReturn(List.of(resultado(a, EstadoRegistro.ACTIVO)));

            List<ResultadoAprendizajeResponseDTO> respuesta =
                    service.consultarPorAsignatura(ID_ASIGNATURA, EstadoRegistro.ACTIVO);

            assertThat(respuesta).hasSize(1);
            assertThat(respuesta.get(0).estado()).isEqualTo(EstadoRegistro.ACTIVO.name());
        }

        @Test
        @DisplayName("Una asignatura sin RA devuelve una lista vacía (estado vacío de la HU-20)")
        void asignaturaSinRa() {
            asignaturaExiste(asignatura(EstadoAsignatura.BORRADOR));
            when(resultadoAprendizajeRepository.findByAsignatura_IdOrderByCodigoAsc(ID_ASIGNATURA)).thenReturn(List.of());

            assertThat(service.consultarPorAsignatura(ID_ASIGNATURA, null)).isEmpty();
        }

        @Test
        @DisplayName("Con una asignatura inexistente falla en lugar de devolver una lista vacía")
        void asignaturaInexistente() {
            asignaturaNoExiste();

            assertThatThrownBy(() -> service.consultarPorAsignatura(ID_ASIGNATURA, null))
                    .isInstanceOf(AsignaturaNoEncontradaException.class);
            verifyNoInteractions(resultadoAprendizajeRepository);
        }

        @Test
        @DisplayName("Todos sin filtro devuelve todos los RA ordenados por código")
        void todosSinFiltro() {
            Asignatura a = asignatura(EstadoAsignatura.ACTIVA);
            when(resultadoAprendizajeRepository.findAll(any(Sort.class)))
                    .thenReturn(List.of(resultado(a, EstadoRegistro.ACTIVO)));

            assertThat(service.consultarTodos(null)).hasSize(1);
            verify(resultadoAprendizajeRepository).findAll(Sort.by("codigo"));
        }

        @Test
        @DisplayName("Todos con filtro de estado usa la consulta filtrada")
        void todosConFiltro() {
            Asignatura a = asignatura(EstadoAsignatura.ACTIVA);
            when(resultadoAprendizajeRepository.findByEstadoOrderByCodigoAsc(EstadoRegistro.INACTIVO))
                    .thenReturn(List.of(resultado(a, EstadoRegistro.INACTIVO)));

            assertThat(service.consultarTodos(EstadoRegistro.INACTIVO)).hasSize(1);
        }

        @Test
        @DisplayName("Por id devuelve el RA")
        void porId() {
            resultadoExiste(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO);

            ResultadoAprendizajeResponseDTO respuesta = service.consultarPorId(ID_RA);

            assertThat(respuesta.id()).isEqualTo(ID_RA);
            assertThat(respuesta.codigo()).isEqualTo(CODIGO);
        }

        @Test
        @DisplayName("Por id inexistente falla")
        void porIdInexistente() {
            resultadoNoExiste();

            assertThatThrownBy(() -> service.consultarPorId(ID_RA))
                    .isInstanceOf(ResultadoAprendizajeNoEncontradoException.class);
        }
    }

    // ------------------------------------------------------------------ RF-06c: modificar

    @Nested
    @DisplayName("Actualizar descripción (RF-06c)")
    class ActualizarDescripcion {

        @Test
        @DisplayName("Con un RA activo cambia solo la descripción y deja intacto el código")
        void raActivo() {
            resultadoExiste(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO);
            saveDevuelveArgumento();

            ResultadoAprendizajeResponseDTO respuesta = service.actualizarDescripcion(ID_RA,
                    new ResultadoAprendizajeActualizarRequestDTO("  Nueva descripción  "));

            assertThat(respuesta.descripcion()).isEqualTo("Nueva descripción");
            assertThat(respuesta.codigo()).isEqualTo(CODIGO);
        }

        @Test
        @DisplayName("Con un RA inactivo falla y no guarda")
        void raInactivo() {
            resultadoExiste(EstadoAsignatura.ACTIVA, EstadoRegistro.INACTIVO);

            assertThatThrownBy(() -> service.actualizarDescripcion(ID_RA,
                    new ResultadoAprendizajeActualizarRequestDTO("Nueva descripción")))
                    .isInstanceOf(ResultadoAprendizajeReglaNegocioException.class)
                    .hasMessageContaining("inactivo");
            verify(resultadoAprendizajeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con un RA inexistente falla")
        void raInexistente() {
            resultadoNoExiste();

            assertThatThrownBy(() -> service.actualizarDescripcion(ID_RA,
                    new ResultadoAprendizajeActualizarRequestDTO("Nueva descripción")))
                    .isInstanceOf(ResultadoAprendizajeNoEncontradoException.class);
        }
    }

    // ------------------------------------------------------------------ RF-06c: inactivar

    @Nested
    @DisplayName("Inactivar (RF-06c)")
    class Inactivar {

        @ParameterizedTest(name = "Asignatura ACTIVA con {0} RA activos: se permite inactivar")
        @ValueSource(longs = {6, 7})
        @DisplayName("En una asignatura ACTIVA con más del mínimo queda INACTIVO")
        void activaSobreElMinimo(long activos) {
            resultadoExiste(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO);
            raActivos(activos);
            saveDevuelveArgumento();

            ResultadoAprendizajeResponseDTO respuesta = service.inactivar(ID_RA);

            assertThat(respuesta.estado()).isEqualTo(EstadoRegistro.INACTIVO.name());
        }

        @Test
        @DisplayName("En una asignatura ACTIVA con exactamente 5 RA activos falla indicando el mínimo y no cambia")
        void activaEnElMinimo() {
            ResultadoAprendizaje ra = resultadoExiste(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO);
            raActivos(Asignatura.MIN_RA_ACTIVOS);

            assertThatThrownBy(() -> service.inactivar(ID_RA))
                    .isInstanceOf(ResultadoAprendizajeReglaNegocioException.class)
                    .hasMessageContaining("mínimo " + Asignatura.MIN_RA_ACTIVOS);
            assertThat(ra.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
            verify(resultadoAprendizajeRepository, never()).save(any());
        }

        @Test
        @DisplayName("En una asignatura en BORRADOR el mínimo no aplica y ni siquiera se cuentan los RA")
        void borradorSinMinimo() {
            resultadoExiste(EstadoAsignatura.BORRADOR, EstadoRegistro.ACTIVO);
            saveDevuelveArgumento();

            ResultadoAprendizajeResponseDTO respuesta = service.inactivar(ID_RA);

            assertThat(respuesta.estado()).isEqualTo(EstadoRegistro.INACTIVO.name());
            verify(resultadoAprendizajeRepository, never()).countByAsignatura_IdAndEstado(any(), any());
        }

        @Test
        @DisplayName("Un RA ya inactivo falla y no guarda")
        void yaInactivo() {
            resultadoExiste(EstadoAsignatura.ACTIVA, EstadoRegistro.INACTIVO);

            assertThatThrownBy(() -> service.inactivar(ID_RA))
                    .isInstanceOf(ResultadoAprendizajeReglaNegocioException.class)
                    .hasMessageContaining("ya se encuentra inactivo");
            verify(resultadoAprendizajeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con un RA inexistente falla")
        void raInexistente() {
            resultadoNoExiste();

            assertThatThrownBy(() -> service.inactivar(ID_RA))
                    .isInstanceOf(ResultadoAprendizajeNoEncontradoException.class);
        }
    }

    // ------------------------------------------------------------------ reactivar

    @Nested
    @DisplayName("Reactivar")
    class Reactivar {

        @ParameterizedTest(name = "Asignatura {0} con 6 RA activos: se permite reactivar")
        @EnumSource(value = EstadoAsignatura.class, names = {"BORRADOR", "ACTIVA"})
        @DisplayName("Por debajo del máximo queda ACTIVO")
        void debajoDelMaximo(EstadoAsignatura estadoAsignatura) {
            resultadoExiste(estadoAsignatura, EstadoRegistro.INACTIVO);
            raActivos(6);
            saveDevuelveArgumento();

            ResultadoAprendizajeResponseDTO respuesta = service.reactivar(ID_RA);

            assertThat(respuesta.estado()).isEqualTo(EstadoRegistro.ACTIVO.name());
        }

        @Test
        @DisplayName("Con 7 RA activos falla indicando el máximo y no guarda")
        void maximoAlcanzado() {
            resultadoExiste(EstadoAsignatura.ACTIVA, EstadoRegistro.INACTIVO);
            raActivos(Asignatura.MAX_RA_ACTIVOS);

            assertThatThrownBy(() -> service.reactivar(ID_RA))
                    .isInstanceOf(ResultadoAprendizajeReglaNegocioException.class)
                    .hasMessageContaining("máximo");
            verify(resultadoAprendizajeRepository, never()).save(any());
        }

        @Test
        @DisplayName("En una asignatura INACTIVA falla y no guarda")
        void asignaturaInactiva() {
            resultadoExiste(EstadoAsignatura.INACTIVA, EstadoRegistro.INACTIVO);

            assertThatThrownBy(() -> service.reactivar(ID_RA))
                    .isInstanceOf(ResultadoAprendizajeReglaNegocioException.class)
                    .hasMessageContaining("inactiva");
            verify(resultadoAprendizajeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Un RA ya activo falla y no guarda")
        void yaActivo() {
            resultadoExiste(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO);

            assertThatThrownBy(() -> service.reactivar(ID_RA))
                    .isInstanceOf(ResultadoAprendizajeReglaNegocioException.class)
                    .hasMessageContaining("ya se encuentra activo");
            verify(resultadoAprendizajeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con un RA inexistente falla")
        void raInexistente() {
            resultadoNoExiste();

            assertThatThrownBy(() -> service.reactivar(ID_RA))
                    .isInstanceOf(ResultadoAprendizajeNoEncontradoException.class);
        }
    }
}
