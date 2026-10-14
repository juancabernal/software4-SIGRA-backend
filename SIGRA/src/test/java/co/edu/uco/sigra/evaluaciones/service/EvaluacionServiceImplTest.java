package co.edu.uco.sigra.evaluaciones.service;

import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.evaluaciones.config.EvaluacionConfig;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionActualizarRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionCrearRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionResponseDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionesRaResumenDTO;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionNoEncontradaException;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionReglaNegocioException;
import co.edu.uco.sigra.evaluaciones.exception.NombreEvaluacionDuplicadoException;
import co.edu.uco.sigra.evaluaciones.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.evaluaciones.mapper.EvaluacionMapper;
import co.edu.uco.sigra.evaluaciones.repository.EvaluacionRepository;
import co.edu.uco.sigra.evaluaciones.service.impl.EvaluacionServiceImpl;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeNoEncontradoException;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del servicio de evaluaciones (RF-10 / RF-11).
 * Los repositorios y el puerto de calificaciones se simulan con Mockito; el mapper es el real generado por
 * MapStruct y el reloj está fijo en 2026-10-13 (zona America/Bogota) para que "el semestre activo" sea
 * determinista. Cada regla se prueba en su caso permitido y en su caso rechazado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de evaluaciones")
class EvaluacionServiceImplTest {

    private static final UUID ID_ASIGNATURA = UUID.randomUUID();
    private static final UUID ID_OTRA_ASIGNATURA = UUID.randomUUID();
    private static final UUID ID_RA = UUID.randomUUID();
    private static final UUID ID_EVALUACION = UUID.randomUUID();
    private static final UUID ID_PROFESOR = UUID.randomUUID();
    private static final UUID ID_SEMESTRE_ACTUAL = UUID.randomUUID();
    private static final UUID ID_SEMESTRE_PASADO = UUID.randomUUID();

    private static final LocalDate INICIO_ACTUAL = LocalDate.of(2026, 8, 3);
    private static final LocalDate FIN_ACTUAL = LocalDate.of(2026, 12, 12);

    private static final String NOMBRE = "Parcial 1";
    private static final String DESCRIPCION = "Primer parcial del corte";
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 10);

    private static final IdentidadActual PROFESOR = new IdentidadActual(ID_PROFESOR, RolUsuario.PROFESOR);

    @Mock
    private EvaluacionRepository evaluacionRepository;

    @Mock
    private ResultadoAprendizajeRepository resultadoAprendizajeRepository;

    @Mock
    private SemestreRepository semestreRepository;

    @Mock
    private AsignacionDocenteRepository asignacionDocenteRepository;

    @Mock
    private ConsultaCalificacionesEvaluacion consultaCalificaciones;

    private final EvaluacionMapper mapper = Mappers.getMapper(EvaluacionMapper.class);

    private EvaluacionServiceImpl service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-13T15:00:00Z"), EvaluacionConfig.ZONA_COLOMBIA);
        service = new EvaluacionServiceImpl(evaluacionRepository, mapper, resultadoAprendizajeRepository,
                semestreRepository, asignacionDocenteRepository, consultaCalificaciones, reloj);
    }

    // ------------------------------------------------------------------ datos de prueba

    private static Semestre semestreActual() {
        Semestre s = new Semestre();
        s.setId(ID_SEMESTRE_ACTUAL);
        s.setCodigo("2026-2");
        s.setFechaInicio(INICIO_ACTUAL);
        s.setFechaFin(FIN_ACTUAL);
        return s;
    }

    private static Semestre semestrePasado() {
        Semestre s = new Semestre();
        s.setId(ID_SEMESTRE_PASADO);
        s.setCodigo("2026-1");
        s.setFechaInicio(LocalDate.of(2026, 2, 2));
        s.setFechaFin(LocalDate.of(2026, 6, 13));
        return s;
    }

    private static Asignatura asignatura(UUID id, EstadoAsignatura estado) {
        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setCodigo("ISIS");
        programa.setNombre("Ingeniería de Sistemas");

        Asignatura a = new Asignatura("ISW4", "Ingeniería de Software IV", programa);
        ReflectionTestUtils.setField(a, "id", id);
        if (estado != EstadoAsignatura.BORRADOR) {
            a.activar(Asignatura.MIN_RA_ACTIVOS);
        }
        if (estado == EstadoAsignatura.INACTIVA) {
            a.inactivar();
        }
        return a;
    }

    private static ResultadoAprendizaje resultado(EstadoAsignatura estadoAsignatura, EstadoRegistro estadoRa) {
        ResultadoAprendizaje ra = new ResultadoAprendizaje();
        ra.setId(ID_RA);
        ra.setAsignatura(asignatura(ID_ASIGNATURA, estadoAsignatura));
        ra.setCodigo("RA-01");
        ra.setDescripcion("Analiza los requisitos");
        ra.setEstado(estadoRa);
        return ra;
    }

    private static Evaluacion evaluacion(ResultadoAprendizaje ra, Semestre semestre, EstadoEvaluacion estado,
                                         String porcentaje) {
        Evaluacion e = new Evaluacion();
        e.setId(ID_EVALUACION);
        e.setResultadoAprendizaje(ra);
        e.setSemestre(semestre);
        e.setNombre(NOMBRE);
        e.setFechaTentativa(FECHA);
        e.setPorcentaje(new BigDecimal(porcentaje));
        e.setDescripcion(DESCRIPCION);
        e.setEstado(estado);
        return e;
    }

    private static EvaluacionCrearRequestDTO crearDto(String porcentaje) {
        return new EvaluacionCrearRequestDTO(NOMBRE, FECHA, new BigDecimal(porcentaje), DESCRIPCION);
    }

    private static EvaluacionCrearRequestDTO crearDto(LocalDate fecha) {
        return new EvaluacionCrearRequestDTO(NOMBRE, fecha, new BigDecimal("30"), DESCRIPCION);
    }

    private static EvaluacionActualizarRequestDTO actualizarDto(String nombre, LocalDate fecha, String porcentaje,
                                                                String descripcion) {
        return new EvaluacionActualizarRequestDTO(nombre, fecha, new BigDecimal(porcentaje), descripcion);
    }

    /** Hay un único semestre activo (el que contiene hoy) y, opcionalmente, uno pasado. */
    private void hayEnCursoYPasado() {
        when(semestreRepository.findAllByOrderByFechaInicioDesc())
                .thenReturn(List.of(semestreActual(), semestrePasado()));
    }

    private void hoyNoHaySemestreEnCurso() {
        when(semestreRepository.findAllByOrderByFechaInicioDesc()).thenReturn(List.of(semestrePasado()));
    }

    private ResultadoAprendizaje raExiste(EstadoAsignatura estadoAsignatura, EstadoRegistro estadoRa) {
        ResultadoAprendizaje ra = resultado(estadoAsignatura, estadoRa);
        when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(ra));
        return ra;
    }

    private ResultadoAprendizaje raActivoExiste() {
        return raExiste(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO);
    }

    private void evaluacionExiste(Evaluacion e) {
        when(evaluacionRepository.findById(ID_EVALUACION)).thenReturn(Optional.of(e));
    }

    private void asignadoActivas(UUID semestreId, String suma) {
        when(evaluacionRepository.sumarPorcentajes(ID_RA, semestreId, EstadoEvaluacion.ACTIVA))
                .thenReturn(new BigDecimal(suma));
    }

    private void asignadoSinEsta(String suma) {
        when(evaluacionRepository.sumarPorcentajesExcluyendo(ID_RA, ID_SEMESTRE_ACTUAL, EstadoEvaluacion.ACTIVA,
                ID_EVALUACION)).thenReturn(new BigDecimal(suma));
    }

    private void profesorAsignadoAAsignatura(UUID asignaturaId) {
        AsignacionDocente asignacion = new AsignacionDocente();
        asignacion.setAsignatura(asignatura(asignaturaId, EstadoAsignatura.ACTIVA));
        asignacion.setEstado(EstadoRegistro.ACTIVO);
        when(asignacionDocenteRepository.findByProfesor_IdAndEstado(ID_PROFESOR, EstadoRegistro.ACTIVO))
                .thenReturn(List.of(asignacion));
    }

    private void guardarDevuelveArgumento() {
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static void assertRegla(Throwable error, EvaluacionReglaNegocioException esperada) {
        assertThat(error).isInstanceOf(EvaluacionReglaNegocioException.class).hasMessage(esperada.getMessage());
    }

    // ------------------------------------------------------------------ RF-10a / RF-11 crear

    @Nested
    @DisplayName("crear (RF-10a)")
    class Crear {

        @Test
        @DisplayName("Con datos válidos guarda la evaluación ACTIVA en el semestre activo, sin calificaciones")
        void datosValidos() {
            ResultadoAprendizaje ra = raActivoExiste();
            hayEnCursoYPasado();
            asignadoActivas(ID_SEMESTRE_ACTUAL, "0");
            guardarDevuelveArgumento();

            EvaluacionResponseDTO respuesta = service.crear(ID_RA, crearDto("30"), null);

            ArgumentCaptor<Evaluacion> guardada = ArgumentCaptor.forClass(Evaluacion.class);
            verify(evaluacionRepository).save(guardada.capture());
            Evaluacion e = guardada.getValue();
            assertThat(e.getResultadoAprendizaje()).isSameAs(ra);
            assertThat(e.getSemestre().getId()).isEqualTo(ID_SEMESTRE_ACTUAL);
            assertThat(e.getNombre()).isEqualTo(NOMBRE);
            assertThat(e.getFechaTentativa()).isEqualTo(FECHA);
            assertThat(e.getPorcentaje()).isEqualByComparingTo("30");
            assertThat(e.getDescripcion()).isEqualTo(DESCRIPCION);
            assertThat(e.getEstado()).isEqualTo(EstadoEvaluacion.ACTIVA);
            assertThat(e.isInactivadaConRa()).isFalse();

            assertThat(respuesta.estado()).isEqualTo("ACTIVA");
            assertThat(respuesta.semestreCodigo()).isEqualTo("2026-2");
            assertThat(respuesta.resultadoAprendizajeId()).isEqualTo(ID_RA);
            assertThat(respuesta.tieneCalificaciones()).isFalse();
        }

        @Test
        @DisplayName("Sin identidad (control por rol apagado) no consulta las asignaciones docentes")
        void sinIdentidadNoFiltra() {
            raActivoExiste();
            hayEnCursoYPasado();
            asignadoActivas(ID_SEMESTRE_ACTUAL, "0");
            guardarDevuelveArgumento();

            service.crear(ID_RA, crearDto("30"), null);

            verifyNoInteractions(asignacionDocenteRepository);
        }

        @Test
        @DisplayName("El profesor con asignación ACTIVA en la asignatura puede crear")
        void profesorAsignado() {
            raActivoExiste();
            profesorAsignadoAAsignatura(ID_ASIGNATURA);
            hayEnCursoYPasado();
            asignadoActivas(ID_SEMESTRE_ACTUAL, "0");
            guardarDevuelveArgumento();

            EvaluacionResponseDTO respuesta = service.crear(ID_RA, crearDto("30"), PROFESOR);

            assertThat(respuesta.estado()).isEqualTo("ACTIVA");
        }

        @Test
        @DisplayName("Un profesor que no tiene asignada la asignatura recibe 403 y no se guarda nada")
        void profesorNoAsignado() {
            raActivoExiste();
            profesorAsignadoAAsignatura(ID_OTRA_ASIGNATURA);

            assertThatThrownBy(() -> service.crear(ID_RA, crearDto("30"), PROFESOR))
                    .isInstanceOf(PermisoInsuficienteException.class);

            verify(evaluacionRepository, never()).save(any());
        }

        @ParameterizedTest
        @EnumSource(value = RolUsuario.class, names = {"ADMINISTRADOR", "ESTUDIANTE"})
        @DisplayName("Un rol distinto de PROFESOR recibe 403")
        void otroRol(RolUsuario rol) {
            raActivoExiste();

            assertThatThrownBy(() -> service.crear(ID_RA, crearDto("30"), new IdentidadActual(ID_PROFESOR, rol)))
                    .isInstanceOf(PermisoInsuficienteException.class);

            verifyNoInteractions(asignacionDocenteRepository);
            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Si el RA no existe lanza ResultadoAprendizajeNoEncontradoException")
        void raNoExiste() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.crear(ID_RA, crearDto("30"), null))
                    .isInstanceOf(ResultadoAprendizajeNoEncontradoException.class);

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con el RA inactivo se rechaza")
        void raInactivo() {
            raExiste(EstadoAsignatura.ACTIVA, EstadoRegistro.INACTIVO);

            assertThatThrownBy(() -> service.crear(ID_RA, crearDto("30"), null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.resultadoAprendizajeInactivo()));

            verify(evaluacionRepository, never()).save(any());
        }

        @ParameterizedTest
        @EnumSource(value = EstadoAsignatura.class, names = {"BORRADOR", "INACTIVA"})
        @DisplayName("Con la asignatura que no está ACTIVA se rechaza")
        void asignaturaNoActiva(EstadoAsignatura estado) {
            raExiste(estado, EstadoRegistro.ACTIVO);

            assertThatThrownBy(() -> service.crear(ID_RA, crearDto("30"), null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.asignaturaNoActiva()));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Si hoy no cae en ningún semestre se rechaza")
        void sinSemestreActivo() {
            raActivoExiste();
            hoyNoHaySemestreEnCurso();

            assertThatThrownBy(() -> service.crear(ID_RA, crearDto("30"), null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.sinSemestreActivo()));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con una fecha anterior al inicio del semestre se rechaza")
        void fechaAntesDelSemestre() {
            raActivoExiste();
            hayEnCursoYPasado();

            assertThatThrownBy(() -> service.crear(ID_RA, crearDto(INICIO_ACTUAL.minusDays(1)), null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException
                            .fechaFueraDelSemestre("2026-2", INICIO_ACTUAL, FIN_ACTUAL)));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con una fecha posterior al fin del semestre se rechaza")
        void fechaDespuesDelSemestre() {
            raActivoExiste();
            hayEnCursoYPasado();

            assertThatThrownBy(() -> service.crear(ID_RA, crearDto(FIN_ACTUAL.plusDays(1)), null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException
                            .fechaFueraDelSemestre("2026-2", INICIO_ACTUAL, FIN_ACTUAL)));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Las fechas de inicio y de fin del semestre son válidas (límites incluidos)")
        void fechasLimiteValidas() {
            raActivoExiste();
            hayEnCursoYPasado();
            asignadoActivas(ID_SEMESTRE_ACTUAL, "0");
            guardarDevuelveArgumento();

            assertThat(service.crear(ID_RA, crearDto(INICIO_ACTUAL), null).fechaTentativa()).isEqualTo(INICIO_ACTUAL);
            assertThat(service.crear(ID_RA, crearDto(FIN_ACTUAL), null).fechaTentativa()).isEqualTo(FIN_ACTUAL);
        }

        @Test
        @DisplayName("Con un nombre ya usado en el RA y semestre lanza NombreEvaluacionDuplicadoException")
        void nombreDuplicado() {
            raActivoExiste();
            hayEnCursoYPasado();
            when(evaluacionRepository.existsByResultadoAprendizaje_IdAndSemestre_IdAndNombreIgnoreCase(
                    ID_RA, ID_SEMESTRE_ACTUAL, NOMBRE)).thenReturn(true);

            assertThatThrownBy(() -> service.crear(ID_RA, crearDto("30"), null))
                    .isInstanceOf(NombreEvaluacionDuplicadoException.class);

            verify(evaluacionRepository, never()).save(any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"40", "100"})
        @DisplayName("Si lo asignado más el nuevo porcentaje llega justo a 100% se permite")
        void sumaExactaCien(String nuevo) {
            raActivoExiste();
            hayEnCursoYPasado();
            asignadoActivas(ID_SEMESTRE_ACTUAL, nuevo.equals("100") ? "0" : "60");
            guardarDevuelveArgumento();

            EvaluacionResponseDTO respuesta = service.crear(ID_RA, crearDto(nuevo), null);

            assertThat(respuesta.porcentaje()).isEqualByComparingTo(nuevo);
        }

        @Test
        @DisplayName("Si la suma superaría 100% se rechaza e informa el porcentaje disponible")
        void sumaExcedida() {
            raActivoExiste();
            hayEnCursoYPasado();
            asignadoActivas(ID_SEMESTRE_ACTUAL, "60");

            assertThatThrownBy(() -> service.crear(ID_RA, crearDto("40.01"), null))
                    .satisfies(e -> assertRegla(e,
                            EvaluacionReglaNegocioException.porcentajeExcedido(new BigDecimal("40"))))
                    .hasMessageContaining("Porcentaje disponible: 40%");

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con el RA ya al 100% cualquier porcentaje nuevo se rechaza (disponible 0%)")
        void raCompleto() {
            raActivoExiste();
            hayEnCursoYPasado();
            asignadoActivas(ID_SEMESTRE_ACTUAL, "100");

            assertThatThrownBy(() -> service.crear(ID_RA, crearDto("0.01"), null))
                    .hasMessageContaining("Porcentaje disponible: 0%");
        }

        @Test
        @DisplayName("La suma se valida solo con el semestre activo: lo asignado en otro semestre no cuenta")
        void sumaIndependientePorSemestre() {
            raActivoExiste();
            hayEnCursoYPasado();
            asignadoActivas(ID_SEMESTRE_ACTUAL, "0");
            guardarDevuelveArgumento();

            EvaluacionResponseDTO respuesta = service.crear(ID_RA, crearDto("100"), null);

            assertThat(respuesta.porcentaje()).isEqualByComparingTo("100");
            verify(evaluacionRepository, never()).sumarPorcentajes(eq(ID_RA), eq(ID_SEMESTRE_PASADO), any());
        }
    }

    // ------------------------------------------------------------------ RF-10b modificar

    @Nested
    @DisplayName("modificar (RF-10b)")
    class Modificar {

        private Evaluacion existente;

        @BeforeEach
        void preparar() {
            existente = evaluacion(resultado(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO), semestreActual(),
                    EstadoEvaluacion.ACTIVA, "30");
        }

        @Test
        @DisplayName("Sin calificaciones permite cambiar nombre, fecha, porcentaje y descripción")
        void sinCalificacionesTodoEditable() {
            evaluacionExiste(existente);
            hayEnCursoYPasado();
            asignadoSinEsta("50");
            guardarDevuelveArgumento();

            EvaluacionResponseDTO respuesta = service.modificar(ID_EVALUACION,
                    actualizarDto("Parcial final", LocalDate.of(2026, 12, 1), "50", "Nueva descripción"), null);

            assertThat(respuesta.nombre()).isEqualTo("Parcial final");
            assertThat(respuesta.fechaTentativa()).isEqualTo(LocalDate.of(2026, 12, 1));
            assertThat(respuesta.porcentaje()).isEqualByComparingTo("50");
            assertThat(respuesta.descripcion()).isEqualTo("Nueva descripción");
            assertThat(respuesta.estado()).isEqualTo("ACTIVA");
            verify(evaluacionRepository).save(existente);
        }

        @Test
        @DisplayName("Con calificaciones permite cambiar solo nombre y descripción")
        void conCalificacionesSoloNombreYDescripcion() {
            evaluacionExiste(existente);
            hayEnCursoYPasado();
            when(consultaCalificaciones.tieneCalificaciones(ID_EVALUACION)).thenReturn(true);
            guardarDevuelveArgumento();

            EvaluacionResponseDTO respuesta = service.modificar(ID_EVALUACION,
                    actualizarDto("Parcial uno", FECHA, "30", "Descripción corregida"), null);

            assertThat(respuesta.nombre()).isEqualTo("Parcial uno");
            assertThat(respuesta.descripcion()).isEqualTo("Descripción corregida");
            assertThat(respuesta.fechaTentativa()).isEqualTo(FECHA);
            assertThat(respuesta.porcentaje()).isEqualByComparingTo("30");
            assertThat(respuesta.tieneCalificaciones()).isTrue();
            verify(evaluacionRepository, never()).sumarPorcentajesExcluyendo(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Con calificaciones, enviar el mismo porcentaje con otra escala (30.00) no cuenta como cambio")
        void conCalificacionesMismoPorcentajeOtraEscala() {
            evaluacionExiste(existente);
            hayEnCursoYPasado();
            when(consultaCalificaciones.tieneCalificaciones(ID_EVALUACION)).thenReturn(true);
            guardarDevuelveArgumento();

            EvaluacionResponseDTO respuesta = service.modificar(ID_EVALUACION,
                    actualizarDto(NOMBRE, FECHA, "30.00", "Otra descripción"), null);

            assertThat(respuesta.descripcion()).isEqualTo("Otra descripción");
        }

        @Test
        @DisplayName("Con calificaciones, cambiar la fecha tentativa se rechaza")
        void conCalificacionesCambiaFecha() {
            evaluacionExiste(existente);
            hayEnCursoYPasado();
            when(consultaCalificaciones.tieneCalificaciones(ID_EVALUACION)).thenReturn(true);

            assertThatThrownBy(() -> service.modificar(ID_EVALUACION,
                    actualizarDto(NOMBRE, FECHA.plusDays(1), "30", DESCRIPCION), null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.camposBloqueadosPorCalificaciones()));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con calificaciones, cambiar el porcentaje se rechaza")
        void conCalificacionesCambiaPorcentaje() {
            evaluacionExiste(existente);
            hayEnCursoYPasado();
            when(consultaCalificaciones.tieneCalificaciones(ID_EVALUACION)).thenReturn(true);

            assertThatThrownBy(() -> service.modificar(ID_EVALUACION,
                    actualizarDto(NOMBRE, FECHA, "20", DESCRIPCION), null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.camposBloqueadosPorCalificaciones()));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Si el nuevo porcentaje haría superar 100% se rechaza e informa el disponible")
        void porcentajeExcedido() {
            evaluacionExiste(existente);
            hayEnCursoYPasado();
            asignadoSinEsta("70");

            assertThatThrownBy(() -> service.modificar(ID_EVALUACION,
                    actualizarDto(NOMBRE, FECHA, "40", DESCRIPCION), null))
                    .satisfies(e -> assertRegla(e,
                            EvaluacionReglaNegocioException.porcentajeExcedido(new BigDecimal("30"))));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("El porcentaje que completa exactamente 100% sin contar la propia evaluación se permite")
        void porcentajeExactoCien() {
            evaluacionExiste(existente);
            hayEnCursoYPasado();
            asignadoSinEsta("70");
            guardarDevuelveArgumento();

            existente.setPorcentaje(new BigDecimal("20"));

            EvaluacionResponseDTO respuesta = service.modificar(ID_EVALUACION,
                    actualizarDto(NOMBRE, FECHA, "30", DESCRIPCION), null);

            assertThat(respuesta.porcentaje()).isEqualByComparingTo("30");
        }

        @Test
        @DisplayName("Si el porcentaje no cambia no se vuelve a validar la suma")
        void porcentajeSinCambioNoValidaSuma() {
            evaluacionExiste(existente);
            hayEnCursoYPasado();
            guardarDevuelveArgumento();

            service.modificar(ID_EVALUACION, actualizarDto("Otro nombre", FECHA, "30", DESCRIPCION), null);

            verify(evaluacionRepository, never()).sumarPorcentajesExcluyendo(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Con un nombre que ya usa otra evaluación del RA y semestre lanza NombreEvaluacionDuplicadoException")
        void nombreDuplicado() {
            evaluacionExiste(existente);
            hayEnCursoYPasado();
            when(evaluacionRepository.existsByResultadoAprendizaje_IdAndSemestre_IdAndNombreIgnoreCaseAndIdNot(
                    ID_RA, ID_SEMESTRE_ACTUAL, "Quiz", ID_EVALUACION)).thenReturn(true);

            assertThatThrownBy(() -> service.modificar(ID_EVALUACION,
                    actualizarDto("Quiz", FECHA, "30", DESCRIPCION), null))
                    .isInstanceOf(NombreEvaluacionDuplicadoException.class);

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con una nueva fecha fuera del semestre se rechaza")
        void fechaFueraDelSemestre() {
            evaluacionExiste(existente);
            hayEnCursoYPasado();

            assertThatThrownBy(() -> service.modificar(ID_EVALUACION,
                    actualizarDto(NOMBRE, FIN_ACTUAL.plusDays(1), "30", DESCRIPCION), null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException
                            .fechaFueraDelSemestre("2026-2", INICIO_ACTUAL, FIN_ACTUAL)));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Una evaluación inactiva no se puede modificar")
        void inactiva() {
            existente.setEstado(EstadoEvaluacion.INACTIVA);
            evaluacionExiste(existente);
            hayEnCursoYPasado();

            assertThatThrownBy(() -> service.modificar(ID_EVALUACION,
                    actualizarDto(NOMBRE, FECHA, "30", DESCRIPCION), null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.modificarInactiva()));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Una evaluación de un semestre pasado es de solo consulta")
        void semestrePasadoEsSoloConsulta() {
            existente.setSemestre(semestrePasado());
            evaluacionExiste(existente);
            hayEnCursoYPasado();

            assertThatThrownBy(() -> service.modificar(ID_EVALUACION,
                    actualizarDto(NOMBRE, FECHA, "30", DESCRIPCION), null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.semestreNoActivo()));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Si la evaluación no existe lanza EvaluacionNoEncontradaException")
        void noExiste() {
            when(evaluacionRepository.findById(ID_EVALUACION)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.modificar(ID_EVALUACION,
                    actualizarDto(NOMBRE, FECHA, "30", DESCRIPCION), null))
                    .isInstanceOf(EvaluacionNoEncontradaException.class);
        }

        @Test
        @DisplayName("Un profesor sin la asignatura asignada recibe 403 y no se modifica nada")
        void profesorNoAsignado() {
            evaluacionExiste(existente);
            profesorAsignadoAAsignatura(ID_OTRA_ASIGNATURA);

            assertThatThrownBy(() -> service.modificar(ID_EVALUACION,
                    actualizarDto("Otro", FECHA, "30", DESCRIPCION), PROFESOR))
                    .isInstanceOf(PermisoInsuficienteException.class);

            assertThat(existente.getNombre()).isEqualTo(NOMBRE);
            verify(evaluacionRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ RF-10c inactivar / reactivar

    @Nested
    @DisplayName("inactivar (RF-10c)")
    class Inactivar {

        @Test
        @DisplayName("Pasa a INACTIVA sin tocar las calificaciones y deja la marca de cascada en falso")
        void inactivaEvaluacionActiva() {
            Evaluacion e = evaluacion(resultado(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO), semestreActual(),
                    EstadoEvaluacion.ACTIVA, "30");
            evaluacionExiste(e);
            hayEnCursoYPasado();
            when(consultaCalificaciones.tieneCalificaciones(ID_EVALUACION)).thenReturn(true);
            guardarDevuelveArgumento();

            EvaluacionResponseDTO respuesta = service.inactivar(ID_EVALUACION, null);

            assertThat(respuesta.estado()).isEqualTo("INACTIVA");
            assertThat(respuesta.tieneCalificaciones()).isTrue();
            assertThat(e.isInactivadaConRa()).isFalse();
            verify(evaluacionRepository).save(e);
        }

        @Test
        @DisplayName("Si ya está inactiva se rechaza")
        void yaInactiva() {
            evaluacionExiste(evaluacion(resultado(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO),
                    semestreActual(), EstadoEvaluacion.INACTIVA, "30"));
            hayEnCursoYPasado();

            assertThatThrownBy(() -> service.inactivar(ID_EVALUACION, null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.yaInactiva()));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Una evaluación de un semestre pasado no se puede inactivar")
        void semestrePasadoNoSeInactiva() {
            evaluacionExiste(evaluacion(resultado(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO),
                    semestrePasado(), EstadoEvaluacion.ACTIVA, "30"));
            hayEnCursoYPasado();

            assertThatThrownBy(() -> service.inactivar(ID_EVALUACION, null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.semestreNoActivo()));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Si no existe lanza EvaluacionNoEncontradaException")
        void noExiste() {
            when(evaluacionRepository.findById(ID_EVALUACION)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.inactivar(ID_EVALUACION, null))
                    .isInstanceOf(EvaluacionNoEncontradaException.class);
        }

        @Test
        @DisplayName("Un profesor sin la asignatura asignada recibe 403")
        void profesorNoAsignado() {
            evaluacionExiste(evaluacion(resultado(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO),
                    semestreActual(), EstadoEvaluacion.ACTIVA, "30"));
            profesorAsignadoAAsignatura(ID_OTRA_ASIGNATURA);

            assertThatThrownBy(() -> service.inactivar(ID_EVALUACION, PROFESOR))
                    .isInstanceOf(PermisoInsuficienteException.class);

            verify(evaluacionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("reactivar (RF-10c)")
    class Reactivar {

        private Evaluacion inactiva(EstadoAsignatura estadoAsignatura, EstadoRegistro estadoRa) {
            Evaluacion e = evaluacion(resultado(estadoAsignatura, estadoRa), semestreActual(),
                    EstadoEvaluacion.INACTIVA, "30");
            e.setInactivadaConRa(true);
            evaluacionExiste(e);
            return e;
        }

        @Test
        @DisplayName("Vuelve a ACTIVA y limpia la marca de cascada cuando la suma sigue siendo ≤ 100%")
        void reactiva() {
            Evaluacion e = inactiva(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO);
            hayEnCursoYPasado();
            asignadoActivas(ID_SEMESTRE_ACTUAL, "50");
            guardarDevuelveArgumento();

            EvaluacionResponseDTO respuesta = service.reactivar(ID_EVALUACION, null);

            assertThat(respuesta.estado()).isEqualTo("ACTIVA");
            assertThat(e.isInactivadaConRa()).isFalse();
            verify(evaluacionRepository).save(e);
        }

        @Test
        @DisplayName("Reactivar que completa exactamente 100% se permite")
        void reactivaHastaCien() {
            inactiva(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO);
            hayEnCursoYPasado();
            asignadoActivas(ID_SEMESTRE_ACTUAL, "70");
            guardarDevuelveArgumento();

            assertThat(service.reactivar(ID_EVALUACION, null).estado()).isEqualTo("ACTIVA");
        }

        @Test
        @DisplayName("Reactivar que haría superar 100% se rechaza e informa el disponible")
        void reactivarExcede() {
            inactiva(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO);
            hayEnCursoYPasado();
            asignadoActivas(ID_SEMESTRE_ACTUAL, "80");

            assertThatThrownBy(() -> service.reactivar(ID_EVALUACION, null))
                    .satisfies(e -> assertRegla(e,
                            EvaluacionReglaNegocioException.porcentajeExcedido(new BigDecimal("20"))));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Si ya está activa se rechaza")
        void yaActiva() {
            evaluacionExiste(evaluacion(resultado(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO),
                    semestreActual(), EstadoEvaluacion.ACTIVA, "30"));
            hayEnCursoYPasado();

            assertThatThrownBy(() -> service.reactivar(ID_EVALUACION, null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.yaActiva()));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con el RA inactivo no se puede reactivar")
        void raInactivo() {
            inactiva(EstadoAsignatura.ACTIVA, EstadoRegistro.INACTIVO);
            hayEnCursoYPasado();

            assertThatThrownBy(() -> service.reactivar(ID_EVALUACION, null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.resultadoAprendizajeInactivo()));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Con la asignatura inactiva no se puede reactivar")
        void asignaturaInactiva() {
            inactiva(EstadoAsignatura.INACTIVA, EstadoRegistro.ACTIVO);
            hayEnCursoYPasado();

            assertThatThrownBy(() -> service.reactivar(ID_EVALUACION, null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.asignaturaNoActiva()));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Una evaluación de un semestre pasado no se puede reactivar")
        void semestrePasadoNoSeReactiva() {
            Evaluacion e = inactiva(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO);
            e.setSemestre(semestrePasado());
            hayEnCursoYPasado();

            assertThatThrownBy(() -> service.reactivar(ID_EVALUACION, null))
                    .satisfies(ex -> assertRegla(ex, EvaluacionReglaNegocioException.semestreNoActivo()));

            verify(evaluacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Un profesor sin la asignatura asignada recibe 403")
        void profesorNoAsignado() {
            inactiva(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO);
            profesorAsignadoAAsignatura(ID_OTRA_ASIGNATURA);

            assertThatThrownBy(() -> service.reactivar(ID_EVALUACION, PROFESOR))
                    .isInstanceOf(PermisoInsuficienteException.class);

            verify(evaluacionRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------ Consultas

    @Nested
    @DisplayName("consultas (vista del profesor)")
    class Consultas {

        @Test
        @DisplayName("Sin semestreId usa el semestre activo y calcula asignado y disponible")
        void resumenSemestreActivo() {
            raActivoExiste();
            hayEnCursoYPasado();
            Evaluacion e = evaluacion(resultado(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO), semestreActual(),
                    EstadoEvaluacion.ACTIVA, "60");
            when(evaluacionRepository.findByResultadoAprendizaje_IdAndSemestre_IdOrderByFechaTentativaAscNombreAsc(
                    ID_RA, ID_SEMESTRE_ACTUAL)).thenReturn(List.of(e));
            asignadoActivas(ID_SEMESTRE_ACTUAL, "60");

            EvaluacionesRaResumenDTO resumen = service.consultarPorResultadoAprendizaje(ID_RA, null, null);

            assertThat(resumen.semestreId()).isEqualTo(ID_SEMESTRE_ACTUAL);
            assertThat(resumen.semestreCodigo()).isEqualTo("2026-2");
            assertThat(resumen.porcentajeAsignado()).isEqualByComparingTo("60");
            assertThat(resumen.porcentajeDisponible()).isEqualByComparingTo("40");
            assertThat(resumen.evaluaciones()).hasSize(1);
            assertThat(resumen.evaluaciones().get(0).nombre()).isEqualTo(NOMBRE);
        }

        @Test
        @DisplayName("Sin evaluaciones el asignado es 0% y el disponible 100%")
        void sinEvaluaciones() {
            raActivoExiste();
            hayEnCursoYPasado();
            when(evaluacionRepository.findByResultadoAprendizaje_IdAndSemestre_IdOrderByFechaTentativaAscNombreAsc(
                    ID_RA, ID_SEMESTRE_ACTUAL)).thenReturn(List.of());
            asignadoActivas(ID_SEMESTRE_ACTUAL, "0");

            EvaluacionesRaResumenDTO resumen = service.consultarPorResultadoAprendizaje(ID_RA, null, null);

            assertThat(resumen.evaluaciones()).isEmpty();
            assertThat(resumen.porcentajeAsignado()).isEqualByComparingTo("0");
            assertThat(resumen.porcentajeDisponible()).isEqualByComparingTo("100");
        }

        @Test
        @DisplayName("Con el RA al 100% el disponible es 0%")
        void disponibleCero() {
            raActivoExiste();
            hayEnCursoYPasado();
            when(evaluacionRepository.findByResultadoAprendizaje_IdAndSemestre_IdOrderByFechaTentativaAscNombreAsc(
                    ID_RA, ID_SEMESTRE_ACTUAL)).thenReturn(List.of());
            asignadoActivas(ID_SEMESTRE_ACTUAL, "100");

            assertThat(service.consultarPorResultadoAprendizaje(ID_RA, null, null).porcentajeDisponible())
                    .isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("Con semestreId de un semestre pasado se puede consultar (solo lectura)")
        void semestrePasadoSeConsulta() {
            raActivoExiste();
            when(semestreRepository.findById(ID_SEMESTRE_PASADO)).thenReturn(Optional.of(semestrePasado()));
            when(evaluacionRepository.findByResultadoAprendizaje_IdAndSemestre_IdOrderByFechaTentativaAscNombreAsc(
                    ID_RA, ID_SEMESTRE_PASADO)).thenReturn(List.of());
            asignadoActivas(ID_SEMESTRE_PASADO, "100");

            EvaluacionesRaResumenDTO resumen =
                    service.consultarPorResultadoAprendizaje(ID_RA, ID_SEMESTRE_PASADO, null);

            assertThat(resumen.semestreCodigo()).isEqualTo("2026-1");
            assertThat(resumen.porcentajeAsignado()).isEqualByComparingTo("100");
            verifyNoInteractions(consultaCalificaciones);
        }

        @Test
        @DisplayName("Con un semestreId inexistente lanza SemestreNoEncontradoException")
        void semestreNoExiste() {
            UUID desconocido = UUID.randomUUID();
            raActivoExiste();
            when(semestreRepository.findById(desconocido)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consultarPorResultadoAprendizaje(ID_RA, desconocido, null))
                    .isInstanceOf(SemestreNoEncontradoException.class);
        }

        @Test
        @DisplayName("Sin semestreId y sin semestre en curso se rechaza")
        void sinSemestreActivo() {
            raActivoExiste();
            hoyNoHaySemestreEnCurso();

            assertThatThrownBy(() -> service.consultarPorResultadoAprendizaje(ID_RA, null, null))
                    .satisfies(e -> assertRegla(e, EvaluacionReglaNegocioException.sinSemestreActivo()));
        }

        @Test
        @DisplayName("Si el RA no existe lanza ResultadoAprendizajeNoEncontradoException")
        void raNoExiste() {
            when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consultarPorResultadoAprendizaje(ID_RA, null, null))
                    .isInstanceOf(ResultadoAprendizajeNoEncontradoException.class);
        }

        @Test
        @DisplayName("Un profesor sin la asignatura asignada no ve las evaluaciones del RA (403)")
        void profesorNoAsignado() {
            raActivoExiste();
            profesorAsignadoAAsignatura(ID_OTRA_ASIGNATURA);

            assertThatThrownBy(() -> service.consultarPorResultadoAprendizaje(ID_RA, null, PROFESOR))
                    .isInstanceOf(PermisoInsuficienteException.class);
        }

        @Test
        @DisplayName("consultarPorId devuelve la evaluación con su indicador de calificaciones")
        void consultarPorId() {
            evaluacionExiste(evaluacion(resultado(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO),
                    semestreActual(), EstadoEvaluacion.ACTIVA, "30"));
            when(consultaCalificaciones.tieneCalificaciones(ID_EVALUACION)).thenReturn(true);

            EvaluacionResponseDTO respuesta = service.consultarPorId(ID_EVALUACION, null);

            assertThat(respuesta.id()).isEqualTo(ID_EVALUACION);
            assertThat(respuesta.tieneCalificaciones()).isTrue();
        }

        @Test
        @DisplayName("consultarPorId con id inexistente lanza EvaluacionNoEncontradaException")
        void consultarPorIdNoExiste() {
            when(evaluacionRepository.findById(ID_EVALUACION)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consultarPorId(ID_EVALUACION, null))
                    .isInstanceOf(EvaluacionNoEncontradaException.class);
        }

        @Test
        @DisplayName("consultarPorId de un profesor sin la asignatura asignada devuelve 403")
        void consultarPorIdProfesorNoAsignado() {
            evaluacionExiste(evaluacion(resultado(EstadoAsignatura.ACTIVA, EstadoRegistro.ACTIVO),
                    semestreActual(), EstadoEvaluacion.ACTIVA, "30"));
            profesorAsignadoAAsignatura(ID_OTRA_ASIGNATURA);

            assertThatThrownBy(() -> service.consultarPorId(ID_EVALUACION, PROFESOR))
                    .isInstanceOf(PermisoInsuficienteException.class);
        }

        @Test
        @DisplayName("consultarPorcentajeAsignado (para RF-18) suma solo las evaluaciones ACTIVAS del RA y semestre")
        void consultarPorcentajeAsignado() {
            asignadoActivas(ID_SEMESTRE_ACTUAL, "45.5");

            assertThat(service.consultarPorcentajeAsignado(ID_RA, ID_SEMESTRE_ACTUAL)).isEqualByComparingTo("45.5");
        }
    }

    // ------------------------------------------------------------------ Cascadas

    @Nested
    @DisplayName("cascadas del sistema")
    class Cascadas {

        @Test
        @DisplayName("Al inactivar un RA se inactivan sus evaluaciones activas")
        void inactivarPorResultado() {
            service.inactivarPorResultadoAprendizaje(ID_RA);

            verify(evaluacionRepository).inactivarActivasPorResultado(ID_RA, EstadoEvaluacion.INACTIVA,
                    EstadoEvaluacion.ACTIVA);
        }

        @Test
        @DisplayName("Al reactivar un RA se restauran las evaluaciones que se inactivaron con él")
        void restaurarPorResultado() {
            service.restaurarPorResultadoAprendizaje(ID_RA);

            verify(evaluacionRepository).restaurarInactivadasConResultado(ID_RA, EstadoEvaluacion.ACTIVA);
        }

        @Test
        @DisplayName("Al inactivar una asignatura se inactivan las evaluaciones activas de sus RA")
        void inactivarPorAsignatura() {
            service.inactivarPorAsignatura(ID_ASIGNATURA);

            verify(evaluacionRepository).inactivarActivasPorAsignatura(ID_ASIGNATURA, EstadoEvaluacion.INACTIVA,
                    EstadoEvaluacion.ACTIVA);
        }

        @Test
        @DisplayName("Al reactivar una asignatura se restauran solo las de RA que quedaron ACTIVOS")
        void restaurarPorAsignatura() {
            service.restaurarPorAsignatura(ID_ASIGNATURA);

            verify(evaluacionRepository).restaurarInactivadasPorAsignatura(ID_ASIGNATURA, EstadoEvaluacion.ACTIVA,
                    EstadoRegistro.ACTIVO);
        }
    }
}
