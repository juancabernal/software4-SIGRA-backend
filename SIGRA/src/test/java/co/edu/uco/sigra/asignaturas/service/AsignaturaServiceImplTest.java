package co.edu.uco.sigra.asignaturas.service;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaUpdateDTO;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.exception.CodigoAsignaturaDuplicadoException;
import co.edu.uco.sigra.asignaturas.exception.ProgramaInactivoException;
import co.edu.uco.sigra.asignaturas.exception.ProgramaNoEncontradoException;
import co.edu.uco.sigra.asignaturas.exception.RangoRaInvalidoException;
import co.edu.uco.sigra.asignaturas.exception.TransicionEstadoInvalidaException;
import co.edu.uco.sigra.asignaturas.mapper.AsignaturaMapper;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.asignaturas.service.impl.AsignaturaServiceImpl;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de asignaturas")
class AsignaturaServiceImplTest {

    private static final UUID ID_ASIGNATURA = UUID.randomUUID();
    private static final UUID ID_PROGRAMA = UUID.randomUUID();
    private static final String CODIGO = "ISW4";
    private static final String NOMBRE = "Ingeniería de Software IV";

    @Mock
    private AsignaturaRepository asignaturaRepository;

    @Mock
    private ProgramaAcademicoRepository programaAcademicoRepository;

    @Mock
    private ResultadoAprendizajeRepository resultadoAprendizajeRepository;

    private final AsignaturaMapper mapper = Mappers.getMapper(AsignaturaMapper.class);

    private AsignaturaServiceImpl service;
    private ProgramaAcademico programa;

    @BeforeEach
    void setUp() {
        service = new AsignaturaServiceImpl(asignaturaRepository, programaAcademicoRepository,
                resultadoAprendizajeRepository, mapper);
        programa = programa(EstadoRegistro.ACTIVO);
    }

    private static ProgramaAcademico programa(EstadoRegistro estado) {
        ProgramaAcademico p = new ProgramaAcademico();
        p.setId(ID_PROGRAMA);
        p.setCodigo("ISIS");
        p.setNombre("Ingeniería de Sistemas");
        p.setEstado(estado);
        return p;
    }

    private Asignatura asignatura(EstadoAsignatura estado) {
        Asignatura a = new Asignatura(CODIGO, NOMBRE, programa);
        ReflectionTestUtils.setField(a, "id", ID_ASIGNATURA);
        if (estado != EstadoAsignatura.BORRADOR) {
            a.activar(Asignatura.MIN_RA_ACTIVOS);
        }
        if (estado == EstadoAsignatura.INACTIVA) {
            a.inactivar();
        }
        return a;
    }

    private Asignatura existe(EstadoAsignatura estado) {
        Asignatura a = asignatura(estado);
        when(asignaturaRepository.findById(ID_ASIGNATURA)).thenReturn(Optional.of(a));
        return a;
    }

    private void noExiste() {
        when(asignaturaRepository.findById(ID_ASIGNATURA)).thenReturn(Optional.empty());
    }

    private void raActivos(long cantidad) {
        when(resultadoAprendizajeRepository.countByAsignatura_IdAndEstado(ID_ASIGNATURA, EstadoRegistro.ACTIVO))
                .thenReturn(cantidad);
    }

    private void saveDevuelveArgumento() {
        when(asignaturaRepository.save(any(Asignatura.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("Registrar")
    class Registrar {

        @Test
        @DisplayName("Con datos válidos queda en BORRADOR, con el código en mayúsculas y el nombre sin espacios sobrantes")
        void datosValidos() {
            when(asignaturaRepository.existsByCodigo(CODIGO)).thenReturn(false);
            when(programaAcademicoRepository.findById(ID_PROGRAMA)).thenReturn(Optional.of(programa));
            when(asignaturaRepository.saveAndFlush(any(Asignatura.class))).thenAnswer(inv -> inv.getArgument(0));

            AsignaturaResponseDTO respuesta = service.registrarAsignatura(
                    new AsignaturaRequestDTO("  isw4 ", "  " + NOMBRE + "  ", ID_PROGRAMA));

            assertThat(respuesta.estado()).isEqualTo(EstadoAsignatura.BORRADOR);
            assertThat(respuesta.codigo()).isEqualTo(CODIGO);
            assertThat(respuesta.nombre()).isEqualTo(NOMBRE);
            assertThat(respuesta.programaId()).isEqualTo(ID_PROGRAMA);
            assertThat(respuesta.programaNombre()).isEqualTo("Ingeniería de Sistemas");
            assertThat(respuesta.cantidadRa()).isZero();
        }

        @Test
        @DisplayName("Con un código ya existente lanza excepción y no guarda")
        void codigoDuplicado() {
            when(asignaturaRepository.existsByCodigo(CODIGO)).thenReturn(true);

            assertThatThrownBy(() -> service.registrarAsignatura(new AsignaturaRequestDTO(CODIGO, NOMBRE, ID_PROGRAMA)))
                    .isInstanceOf(CodigoAsignaturaDuplicadoException.class);
            verify(asignaturaRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Normaliza a mayúsculas antes de validar si el código ya existe")
        void codigoDuplicadoEnMinusculas() {
            when(asignaturaRepository.existsByCodigo(CODIGO)).thenReturn(true);

            assertThatThrownBy(() -> service.registrarAsignatura(new AsignaturaRequestDTO("isw4", NOMBRE, ID_PROGRAMA)))
                    .isInstanceOf(CodigoAsignaturaDuplicadoException.class);
            verify(asignaturaRepository).existsByCodigo(CODIGO);
            verify(asignaturaRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Con un programa inexistente lanza excepción y no guarda")
        void programaInexistente() {
            when(asignaturaRepository.existsByCodigo(CODIGO)).thenReturn(false);
            when(programaAcademicoRepository.findById(ID_PROGRAMA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registrarAsignatura(new AsignaturaRequestDTO(CODIGO, NOMBRE, ID_PROGRAMA)))
                    .isInstanceOf(ProgramaNoEncontradoException.class);
            verify(asignaturaRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Con un programa INACTIVO lanza excepción y no guarda")
        void programaInactivo() {
            when(asignaturaRepository.existsByCodigo(CODIGO)).thenReturn(false);
            when(programaAcademicoRepository.findById(ID_PROGRAMA))
                    .thenReturn(Optional.of(programa(EstadoRegistro.INACTIVO)));

            assertThatThrownBy(() -> service.registrarAsignatura(new AsignaturaRequestDTO(CODIGO, NOMBRE, ID_PROGRAMA)))
                    .isInstanceOf(ProgramaInactivoException.class);
            verify(asignaturaRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Modificar")
    class Modificar {

        @Test
        @DisplayName("Cambia solo el nombre y deja intactos el código y el programa")
        void cambiaSoloElNombre() {
            existe(EstadoAsignatura.BORRADOR);
            raActivos(3);
            saveDevuelveArgumento();

            AsignaturaResponseDTO respuesta = service.modificarAsignatura(ID_ASIGNATURA,
                    new AsignaturaUpdateDTO("  Arquitectura de Software "));

            assertThat(respuesta.nombre()).isEqualTo("Arquitectura de Software");
            assertThat(respuesta.codigo()).isEqualTo(CODIGO);
            assertThat(respuesta.programaId()).isEqualTo(ID_PROGRAMA);
            assertThat(respuesta.cantidadRa()).isEqualTo(3);
        }

        @Test
        @DisplayName("Con una asignatura inexistente lanza excepción")
        void inexistente() {
            noExiste();

            assertThatThrownBy(() -> service.modificarAsignatura(ID_ASIGNATURA, new AsignaturaUpdateDTO(NOMBRE)))
                    .isInstanceOf(AsignaturaNoEncontradaException.class);
            verify(asignaturaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Obtener")
    class Obtener {

        @Test
        @DisplayName("Devuelve los datos con la cantidad de RA activos")
        void devuelveDatos() {
            existe(EstadoAsignatura.ACTIVA);
            raActivos(6);

            AsignaturaResponseDTO respuesta = service.obtenerAsignatura(ID_ASIGNATURA);

            assertThat(respuesta.id()).isEqualTo(ID_ASIGNATURA);
            assertThat(respuesta.codigo()).isEqualTo(CODIGO);
            assertThat(respuesta.nombre()).isEqualTo(NOMBRE);
            assertThat(respuesta.programaNombre()).isEqualTo("Ingeniería de Sistemas");
            assertThat(respuesta.estado()).isEqualTo(EstadoAsignatura.ACTIVA);
            assertThat(respuesta.cantidadRa()).isEqualTo(6);
        }

        @Test
        @DisplayName("Con una asignatura inexistente lanza excepción")
        void inexistente() {
            noExiste();

            assertThatThrownBy(() -> service.obtenerAsignatura(ID_ASIGNATURA))
                    .isInstanceOf(AsignaturaNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("Activar")
    class Activar {

        @Test
        @DisplayName("Con 4 RA activos falla indicando el mínimo, sigue en BORRADOR y no guarda")
        void pocosRa() {
            Asignatura asignatura = existe(EstadoAsignatura.BORRADOR);
            raActivos(4);

            assertThatThrownBy(() -> service.activar(ID_ASIGNATURA))
                    .isInstanceOf(RangoRaInvalidoException.class)
                    .hasMessageContaining("al menos 5");
            assertThat(asignatura.getEstado()).isEqualTo(EstadoAsignatura.BORRADOR);
            verify(asignaturaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con 8 RA activos falla indicando el máximo, sigue en BORRADOR y no guarda")
        void demasiadosRa() {
            Asignatura asignatura = existe(EstadoAsignatura.BORRADOR);
            raActivos(8);

            assertThatThrownBy(() -> service.activar(ID_ASIGNATURA))
                    .isInstanceOf(RangoRaInvalidoException.class)
                    .hasMessageContaining("máximo 7");
            assertThat(asignatura.getEstado()).isEqualTo(EstadoAsignatura.BORRADOR);
            verify(asignaturaRepository, never()).save(any());
        }

        @ParameterizedTest(name = "Con {0} RA activos queda ACTIVA")
        @ValueSource(longs = {5, 6, 7})
        @DisplayName("Con un rango válido de RA activos queda ACTIVA")
        void rangoValido(long cantidad) {
            existe(EstadoAsignatura.BORRADOR);
            raActivos(cantidad);
            saveDevuelveArgumento();

            AsignaturaResponseDTO respuesta = service.activar(ID_ASIGNATURA);

            assertThat(respuesta.estado()).isEqualTo(EstadoAsignatura.ACTIVA);
            assertThat(respuesta.cantidadRa()).isEqualTo(cantidad);
        }

        @Test
        @DisplayName("Desde BORRADOR no restaura RA: solo cuenta los activos")
        void desdeBorradorNoRestaura() {
            existe(EstadoAsignatura.BORRADOR);
            raActivos(5);
            saveDevuelveArgumento();

            service.activar(ID_ASIGNATURA);

            verify(resultadoAprendizajeRepository, never()).restaurarInactivadosConAsignatura(any(), any());
        }

        @ParameterizedTest(name = "Reactivar con {0} RA restaurados queda ACTIVA")
        @ValueSource(longs = {5, 6, 7})
        @DisplayName("Desde INACTIVA restaura los RA inactivados con ella ANTES de contar y queda ACTIVA")
        void reactivarDesdeInactiva(long cantidad) {
            existe(EstadoAsignatura.INACTIVA);
            raActivos(cantidad);
            saveDevuelveArgumento();

            AsignaturaResponseDTO respuesta = service.activar(ID_ASIGNATURA);

            var orden = inOrder(resultadoAprendizajeRepository);
            orden.verify(resultadoAprendizajeRepository)
                    .restaurarInactivadosConAsignatura(ID_ASIGNATURA, EstadoRegistro.ACTIVO);
            orden.verify(resultadoAprendizajeRepository)
                    .countByAsignatura_IdAndEstado(ID_ASIGNATURA, EstadoRegistro.ACTIVO);
            assertThat(respuesta.estado()).isEqualTo(EstadoAsignatura.ACTIVA);
            assertThat(respuesta.cantidadRa()).isEqualTo(cantidad);
        }

        @Test
        @DisplayName("Reactivar con menos de 5 RA restaurados falla, sigue INACTIVA y no guarda")
        void reactivarConPocosRa() {
            Asignatura asignatura = existe(EstadoAsignatura.INACTIVA);
            raActivos(4);

            assertThatThrownBy(() -> service.activar(ID_ASIGNATURA))
                    .isInstanceOf(RangoRaInvalidoException.class)
                    .hasMessageContaining("al menos 5");
            assertThat(asignatura.getEstado()).isEqualTo(EstadoAsignatura.INACTIVA);
            verify(asignaturaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Reactivar con más de 7 RA restaurados falla, sigue INACTIVA y no guarda")
        void reactivarConDemasiadosRa() {
            Asignatura asignatura = existe(EstadoAsignatura.INACTIVA);
            raActivos(8);

            assertThatThrownBy(() -> service.activar(ID_ASIGNATURA))
                    .isInstanceOf(RangoRaInvalidoException.class)
                    .hasMessageContaining("máximo 7");
            assertThat(asignatura.getEstado()).isEqualTo(EstadoAsignatura.INACTIVA);
            verify(asignaturaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Una asignatura ya ACTIVA no se puede activar de nuevo")
        void yaActiva() {
            existe(EstadoAsignatura.ACTIVA);
            raActivos(5);

            assertThatThrownBy(() -> service.activar(ID_ASIGNATURA))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);
            verify(asignaturaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con una asignatura inexistente lanza excepción")
        void inexistente() {
            noExiste();

            assertThatThrownBy(() -> service.activar(ID_ASIGNATURA))
                    .isInstanceOf(AsignaturaNoEncontradaException.class);
        }
    }

    @Nested
    @DisplayName("Inactivar")
    class Inactivar {

        @Test
        @DisplayName("Desde ACTIVA queda INACTIVA, inactiva en cascada sus RA activos y no elimina nada")
        void desdeActiva() {
            existe(EstadoAsignatura.ACTIVA);
            raActivos(0);
            saveDevuelveArgumento();

            AsignaturaResponseDTO respuesta = service.inactivar(ID_ASIGNATURA);

            assertThat(respuesta.estado()).isEqualTo(EstadoAsignatura.INACTIVA);
            assertThat(respuesta.cantidadRa()).isZero();
            verify(resultadoAprendizajeRepository, times(1))
                    .inactivarActivosPorAsignatura(ID_ASIGNATURA, EstadoRegistro.INACTIVO, EstadoRegistro.ACTIVO);
            verify(asignaturaRepository, never()).delete(any());
            verify(asignaturaRepository, never()).deleteById(any());
            verify(resultadoAprendizajeRepository, never()).delete(any());
            verify(resultadoAprendizajeRepository, never()).deleteById(any());
            verifyNoInteractions(programaAcademicoRepository);
        }

        @Test
        @DisplayName("Desde BORRADOR lanza excepción, no guarda y no toca los RA")
        void desdeBorrador() {
            existe(EstadoAsignatura.BORRADOR);

            assertThatThrownBy(() -> service.inactivar(ID_ASIGNATURA))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);
            verify(asignaturaRepository, never()).save(any());
            verify(resultadoAprendizajeRepository, never()).inactivarActivosPorAsignatura(any(), any(), any());
        }

        @Test
        @DisplayName("Desde INACTIVA lanza excepción, no guarda y no toca los RA")
        void desdeInactiva() {
            existe(EstadoAsignatura.INACTIVA);

            assertThatThrownBy(() -> service.inactivar(ID_ASIGNATURA))
                    .isInstanceOf(TransicionEstadoInvalidaException.class);
            verify(asignaturaRepository, never()).save(any());
            verify(resultadoAprendizajeRepository, never()).inactivarActivosPorAsignatura(any(), any(), any());
        }

        @Test
        @DisplayName("Con una asignatura inexistente lanza excepción y no toca los RA")
        void inexistente() {
            noExiste();

            assertThatThrownBy(() -> service.inactivar(ID_ASIGNATURA))
                    .isInstanceOf(AsignaturaNoEncontradaException.class);
            verifyNoInteractions(resultadoAprendizajeRepository);
        }
    }
}
