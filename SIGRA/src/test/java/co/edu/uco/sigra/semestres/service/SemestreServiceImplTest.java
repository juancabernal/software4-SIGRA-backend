package co.edu.uco.sigra.semestres.service;

import co.edu.uco.sigra.semestres.config.SemestreConfig;
import co.edu.uco.sigra.semestres.dto.SemestreFechaFinRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreResponseDTO;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.exception.SemestreDuplicadoException;
import co.edu.uco.sigra.semestres.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.semestres.exception.SemestreReglaNegocioException;
import co.edu.uco.sigra.semestres.exception.SemestreSolapadoException;
import co.edu.uco.sigra.semestres.mapper.SemestreMapper;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import co.edu.uco.sigra.semestres.service.impl.SemestreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SemestreServiceImplTest {

    /** "Hoy" fijo para todas las pruebas: 2 de octubre de 2026 en Bogotá. */
    private static final LocalDate HOY = LocalDate.of(2026, 10, 2);
    private static final Clock RELOJ =
            Clock.fixed(Instant.parse("2026-10-02T17:00:00Z"), SemestreConfig.ZONA_COLOMBIA);

    private static final String COD_2026_1 = "2026-1";
    private static final String COD_2026_2 = "2026-2";
    private static final String COD_2027_1 = "2027-1";
    private static final String COD_INEXISTENTE = "2030-1";
    private static final String ACTIVO = EstadoSemestre.ACTIVO.name();
    private static final String INACTIVO = EstadoSemestre.INACTIVO.name();

    @Mock
    private SemestreRepository repository;

    private final SemestreMapper mapper = Mappers.getMapper(SemestreMapper.class);

    private SemestreServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SemestreServiceImpl(repository, mapper, RELOJ);
    }

    private void saveDevuelveArgumento() {
        when(repository.save(any(Semestre.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Semestre semestre(String codigo, LocalDate inicio, LocalDate fin, EstadoSemestre estado) {
        Semestre s = new Semestre();
        s.setId(UUID.randomUUID());
        s.setCodigo(codigo);
        s.setFechaInicio(inicio);
        s.setFechaFin(fin);
        s.setEstado(estado);
        return s;
    }

    @Nested
    class Crear {

        @Test
        void creaSemestreFuturoComoInactivo() {
            var dto = new SemestreRequestDTO(COD_2027_1, LocalDate.of(2027, 1, 20), LocalDate.of(2027, 6, 10));
            saveDevuelveArgumento();

            SemestreResponseDTO r = service.crear(dto);

            assertThat(r.codigo()).isEqualTo(COD_2027_1);
            assertThat(r.estado()).isEqualTo(INACTIVO);
            verify(repository).save(any(Semestre.class));
        }

        @Test
        void creaSemestreQueIniciaHoyComoActivo() {
            var dto = new SemestreRequestDTO(COD_2026_2, HOY, LocalDate.of(2026, 12, 15));
            saveDevuelveArgumento();

            assertThat(service.crear(dto).estado()).isEqualTo(ACTIVO);
        }

        @Test
        void rechazaFechaFinIgualAInicio() {
            var dto = new SemestreRequestDTO(COD_2027_1, LocalDate.of(2027, 1, 20), LocalDate.of(2027, 1, 20));

            assertThatThrownBy(() -> service.crear(dto)).isInstanceOf(SemestreReglaNegocioException.class);
            verifyNoInteractions(repository);
        }

        @Test
        void rechazaFechaFinAnteriorAInicio() {
            var dto = new SemestreRequestDTO(COD_2027_1, LocalDate.of(2027, 6, 1), LocalDate.of(2027, 1, 1));

            assertThatThrownBy(() -> service.crear(dto)).isInstanceOf(SemestreReglaNegocioException.class);
        }

        @Test
        void rechazaFechaInicioPasada() {
            var dto = new SemestreRequestDTO(COD_2026_2, HOY.minusDays(1), LocalDate.of(2026, 12, 15));

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(SemestreReglaNegocioException.class)
                    .hasMessageContaining("fecha de inicio");
        }

        @Test
        void rechazaAnioDeCodigoDistintoAlDeFechaInicio() {
            var dto = new SemestreRequestDTO(COD_2026_1, LocalDate.of(2027, 1, 20), LocalDate.of(2027, 6, 10));

            assertThatThrownBy(() -> service.crear(dto))
                    .isInstanceOf(SemestreReglaNegocioException.class)
                    .hasMessageContaining("año");
        }

        @Test
        void rechazaCodigoDuplicado() {
            var dto = new SemestreRequestDTO(COD_2027_1, LocalDate.of(2027, 1, 20), LocalDate.of(2027, 6, 10));
            when(repository.existsByCodigo(COD_2027_1)).thenReturn(true);

            assertThatThrownBy(() -> service.crear(dto)).isInstanceOf(SemestreDuplicadoException.class);
            verify(repository, never()).save(any());
        }

        @Test
        void rechazaSolapamientoConOtroSemestre() {
            var dto = new SemestreRequestDTO(COD_2027_1, LocalDate.of(2027, 1, 20), LocalDate.of(2027, 6, 10));
            when(repository.existsByCodigo(COD_2027_1)).thenReturn(false);
            when(repository.existeSolapamiento(dto.fechaInicio(), dto.fechaFin())).thenReturn(true);

            assertThatThrownBy(() -> service.crear(dto)).isInstanceOf(SemestreSolapadoException.class);
            verify(repository, never()).save(any());
        }
    }

    @Nested
    class Consultar {

        @Test
        void lanzaNoEncontradoSiNoExiste() {
            when(repository.findByCodigo(COD_INEXISTENTE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consultarPorCodigo(COD_INEXISTENTE))
                    .isInstanceOf(SemestreNoEncontradoException.class);
        }

        @Test
        void recalculaYPersisteEstadoSiCambio() {
            // Guardado como INACTIVO pero hoy está dentro del rango -> debe pasar a ACTIVO
            var s = semestre(COD_2026_2, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 12, 15), EstadoSemestre.INACTIVO);
            when(repository.findByCodigo(COD_2026_2)).thenReturn(Optional.of(s));
            saveDevuelveArgumento();

            assertThat(service.consultarPorCodigo(COD_2026_2).estado()).isEqualTo(ACTIVO);
            verify(repository).save(s);
        }

        @Test
        void noPersisteSiElEstadoNoCambio() {
            var s = semestre(COD_2026_1, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 10), EstadoSemestre.INACTIVO);
            when(repository.findByCodigo(COD_2026_1)).thenReturn(Optional.of(s));

            assertThat(service.consultarPorCodigo(COD_2026_1).estado()).isEqualTo(INACTIVO);
            verify(repository, never()).save(any());
        }

        @Test
        void listaTodosConEstadoCoherente() {
            var terminado = semestre(COD_2026_1, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 10), EstadoSemestre.ACTIVO);
            var enCurso = semestre(COD_2026_2, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 12, 15), EstadoSemestre.ACTIVO);
            when(repository.findAllByOrderByFechaInicioDesc()).thenReturn(List.of(enCurso, terminado));
            saveDevuelveArgumento();

            List<SemestreResponseDTO> r = service.listar();

            assertThat(r).extracting(SemestreResponseDTO::estado).containsExactly(ACTIVO, INACTIVO);
            verify(repository, times(1)).save(terminado);
        }
    }

    @Nested
    class Extender {

        private final Semestre enCurso =
                semestre(COD_2026_2, LocalDate.of(2026, 7, 20), LocalDate.of(2026, 12, 15), EstadoSemestre.ACTIVO);

        @Test
        void extiendeFechaFin() {
            var nueva = LocalDate.of(2026, 12, 20);
            when(repository.findByCodigo(COD_2026_2)).thenReturn(Optional.of(enCurso));
            when(repository.existeSolapamientoExcluyendo(enCurso.getFechaInicio(), nueva, enCurso.getId())).thenReturn(false);
            saveDevuelveArgumento();

            var r = service.extenderFechaFin(COD_2026_2, new SemestreFechaFinRequestDTO(nueva));

            assertThat(r.fechaFin()).isEqualTo(nueva);
            assertThat(r.estado()).isEqualTo(ACTIVO);
        }

        @Test
        void rechazaFechaIgualALaActual() {
            when(repository.findByCodigo(COD_2026_2)).thenReturn(Optional.of(enCurso));

            assertThatThrownBy(() -> service.extenderFechaFin(COD_2026_2,
                    new SemestreFechaFinRequestDTO(enCurso.getFechaFin())))
                    .isInstanceOf(SemestreReglaNegocioException.class);
        }

        @Test
        void rechazaAcortar() {
            when(repository.findByCodigo(COD_2026_2)).thenReturn(Optional.of(enCurso));

            assertThatThrownBy(() -> service.extenderFechaFin(COD_2026_2,
                    new SemestreFechaFinRequestDTO(LocalDate.of(2026, 11, 30))))
                    .isInstanceOf(SemestreReglaNegocioException.class);
            verify(repository, never()).save(any());
        }

        @Test
        void rechazaSemestreTerminado() {
            var terminado = semestre(COD_2026_1, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 10), EstadoSemestre.INACTIVO);
            when(repository.findByCodigo(COD_2026_1)).thenReturn(Optional.of(terminado));

            assertThatThrownBy(() -> service.extenderFechaFin(COD_2026_1,
                    new SemestreFechaFinRequestDTO(LocalDate.of(2026, 7, 1))))
                    .isInstanceOf(SemestreReglaNegocioException.class)
                    .hasMessageContaining("terminó");
        }

        @Test
        void permiteExtenderElUltimoDiaDelSemestre() {
            var terminaHoy = semestre(COD_2026_2, LocalDate.of(2026, 7, 20), HOY, EstadoSemestre.ACTIVO);
            var nueva = HOY.plusDays(5);
            when(repository.findByCodigo(COD_2026_2)).thenReturn(Optional.of(terminaHoy));
            when(repository.existeSolapamientoExcluyendo(terminaHoy.getFechaInicio(), nueva, terminaHoy.getId())).thenReturn(false);
            saveDevuelveArgumento();

            assertThat(service.extenderFechaFin(COD_2026_2, new SemestreFechaFinRequestDTO(nueva)).fechaFin())
                    .isEqualTo(nueva);
        }

        @Test
        void rechazaExtensionQueSolapa() {
            var nueva = LocalDate.of(2027, 2, 1);
            when(repository.findByCodigo(COD_2026_2)).thenReturn(Optional.of(enCurso));
            when(repository.existeSolapamientoExcluyendo(enCurso.getFechaInicio(), nueva, enCurso.getId())).thenReturn(true);

            assertThatThrownBy(() -> service.extenderFechaFin(COD_2026_2, new SemestreFechaFinRequestDTO(nueva)))
                    .isInstanceOf(SemestreSolapadoException.class);
            verify(repository, never()).save(any());
        }

        @Test
        void lanzaNoEncontradoSiNoExiste() {
            when(repository.findByCodigo(COD_INEXISTENTE)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.extenderFechaFin(COD_INEXISTENTE,
                    new SemestreFechaFinRequestDTO(LocalDate.of(2030, 7, 1))))
                    .isInstanceOf(SemestreNoEncontradoException.class);
        }
    }
}
