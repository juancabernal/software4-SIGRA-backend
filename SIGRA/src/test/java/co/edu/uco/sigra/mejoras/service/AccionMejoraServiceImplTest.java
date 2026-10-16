package co.edu.uco.sigra.mejoras.service;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.repository.ConclusionRepository;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionCrearRequestDTO;
import co.edu.uco.sigra.evaluaciones.dto.EvaluacionResponseDTO;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionReglaNegocioException;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraFiltroDTO;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraResponseDTO;
import co.edu.uco.sigra.mejoras.dto.ConvertirEvaluacionRequestDTO;
import co.edu.uco.sigra.mejoras.dto.RegistrarAccionMejoraRequestDTO;
import co.edu.uco.sigra.mejoras.entity.AccionMejora;
import co.edu.uco.sigra.mejoras.exception.AccionNoConvertibleException;
import co.edu.uco.sigra.mejoras.exception.AccionYaConvertidaException;
import co.edu.uco.sigra.mejoras.exception.ConclusionNoVigenteException;
import co.edu.uco.sigra.mejoras.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.mejoras.mapper.AccionMejoraMapper;
import co.edu.uco.sigra.mejoras.repository.AccionMejoraRepository;
import co.edu.uco.sigra.mejoras.service.impl.AccionMejoraServiceImpl;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import jakarta.validation.Validation;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del servicio de acciones de mejora (RF-17) y su conversión en evaluación
 * (RF-18). Los repositorios y el servicio de evaluaciones se simulan con Mockito; el mapper es el real
 * generado por MapStruct y el validador es el real de Bean Validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de acciones de mejora (RF-17/RF-18)")
class AccionMejoraServiceImplTest {

    private static final UsuarioPrincipal ADMIN =
            new UsuarioPrincipal(UUID.randomUUID(), "admin@uco.net.co", RolUsuario.ADMINISTRADOR);

    private static final UUID ID_RA = UUID.randomUUID();
    private static final UUID ID_ASIGNATURA = UUID.randomUUID();
    private static final UUID ID_SEMESTRE = UUID.randomUUID();

    @Mock
    private AccionMejoraRepository accionMejoraRepository;
    @Mock
    private ConclusionRepository conclusionRepository;
    @Mock
    private AsignacionDocenteRepository asignacionDocenteRepository;
    @Mock
    private EvaluacionService evaluacionService;

    private final AccionMejoraMapper mapper = Mappers.getMapper(AccionMejoraMapper.class);

    private AccionMejoraServiceImpl service;
    private Asignatura asignatura;
    private ResultadoAprendizaje resultadoAprendizaje;
    private Semestre semestre;

    @BeforeEach
    void setUp() {
        service = new AccionMejoraServiceImpl(accionMejoraRepository, conclusionRepository,
                asignacionDocenteRepository, mapper, evaluacionService,
                Validation.buildDefaultValidatorFactory().getValidator());

        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setId(UUID.randomUUID());
        programa.setNombre("Ingeniería de Sistemas");
        asignatura = new Asignatura("ISW4", "Ingeniería de Software IV", programa);
        ReflectionTestUtils.setField(asignatura, "id", ID_ASIGNATURA);

        resultadoAprendizaje = new ResultadoAprendizaje();
        resultadoAprendizaje.setId(ID_RA);
        resultadoAprendizaje.setAsignatura(asignatura);
        resultadoAprendizaje.setCodigo("RA-01");
        resultadoAprendizaje.setDescripcion("Analiza los requisitos de un sistema");
        resultadoAprendizaje.setEstado(EstadoRegistro.ACTIVO);

        semestre = new Semestre();
        semestre.setId(ID_SEMESTRE);
        semestre.setCodigo("2025-2");
    }

    private Conclusion conclusionVigente() {
        return new Conclusion(resultadoAprendizaje, semestre, 60.0);
    }

    private static final LocalDate FECHA = LocalDate.now();

    private static ConvertirEvaluacionRequestDTO conversion(String porcentaje) {
        return new ConvertirEvaluacionRequestDTO(FECHA, new BigDecimal(porcentaje));
    }

    /** Respuesta del servicio de evaluaciones con los datos que recibió y el id generado. */
    private static EvaluacionResponseDTO evaluacionCreada(UUID idEvaluacion, EvaluacionCrearRequestDTO dto) {
        return new EvaluacionResponseDTO(idEvaluacion, ID_RA, UUID.randomUUID(), "2026-2", dto.nombre(),
                dto.fechaTentativa(), dto.porcentaje(), dto.descripcion(), "ACTIVA", false);
    }

    @Nested
    @DisplayName("registrarAccionMejora (RF-17)")
    class Registrar {

        @Test
        @DisplayName("Sobre un RA sin conclusión vigente (ya ATENDIDA) se rechaza")
        void sinConclusionVigenteSeRechaza() {
            UUID idConclusion = UUID.randomUUID();
            Conclusion conclusion = conclusionVigente();
            conclusion.marcarAtendida();
            when(conclusionRepository.findById(idConclusion)).thenReturn(Optional.of(conclusion));

            RegistrarAccionMejoraRequestDTO datos =
                    new RegistrarAccionMejoraRequestDTO("Capacitación", "Taller de refuerzo");

            assertThatThrownBy(() -> service.registrarAccionMejora(idConclusion, datos, ADMIN))
                    .isInstanceOf(ConclusionNoVigenteException.class);
            verify(accionMejoraRepository, never()).save(any());
        }

        @Test
        @DisplayName("Sobre una conclusión VIGENTE crea la acción y la conclusión queda ATENDIDA")
        void conConclusionVigenteCreaAccion() {
            UUID idConclusion = UUID.randomUUID();
            Conclusion conclusion = conclusionVigente();
            when(conclusionRepository.findById(idConclusion)).thenReturn(Optional.of(conclusion));
            when(accionMejoraRepository.save(any(AccionMejora.class))).thenAnswer(inv -> inv.getArgument(0));

            RegistrarAccionMejoraRequestDTO datos =
                    new RegistrarAccionMejoraRequestDTO("Capacitación", "Taller de refuerzo");

            AccionMejoraResponseDTO respuesta = service.registrarAccionMejora(idConclusion, datos, ADMIN);

            assertThat(respuesta.nombre()).isEqualTo("Capacitación");
            assertThat(respuesta.descripcion()).isEqualTo("Taller de refuerzo");
            assertThat(respuesta.convertida()).isFalse();
            assertThat(conclusion.getEstado()).isEqualTo(EstadoConclusion.ATENDIDA);
            verify(accionMejoraRepository).save(any(AccionMejora.class));
        }
    }

    @Nested
    @DisplayName("Alcance por rol")
    class Alcance {

        @Test
        @DisplayName("Un PROFESOR sin asignación docente a la asignatura del RA no puede registrar")
        void profesorSinAsignacionNoPuedeRegistrar() {
            UUID idConclusion = UUID.randomUUID();
            UUID profesorId = UUID.randomUUID();
            Conclusion conclusion = conclusionVigente();
            when(conclusionRepository.findById(idConclusion)).thenReturn(Optional.of(conclusion));
            when(asignacionDocenteRepository.findByProfesor_IdAndEstado(profesorId, EstadoRegistro.ACTIVO))
                    .thenReturn(List.of());

            RegistrarAccionMejoraRequestDTO datos =
                    new RegistrarAccionMejoraRequestDTO("Capacitación", "Taller de refuerzo");

            assertThatThrownBy(() -> service.registrarAccionMejora(idConclusion, datos,
                    new UsuarioPrincipal(profesorId, RolUsuario.PROFESOR)))
                    .isInstanceOf(PermisoInsuficienteException.class);
            verify(accionMejoraRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("convertirEnEvaluacion (RF-18)")
    class Convertir {

        private AccionMejora accionNoConvertida(Conclusion conclusionOrigen) {
            return new AccionMejora("Capacitación", "Taller de refuerzo", resultadoAprendizaje, conclusionOrigen);
        }

        @Test
        @DisplayName("Crea la evaluación con el servicio de evaluaciones heredando nombre y descripción")
        void creaEvaluacionHeredandoNombreYDescripcion() {
            UUID idAccion = UUID.randomUUID();
            UUID idEvaluacion = UUID.randomUUID();
            AccionMejora accion = accionNoConvertida(conclusionVigente());
            when(accionMejoraRepository.findById(idAccion)).thenReturn(Optional.of(accion));
            when(evaluacionService.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any()))
                    .thenAnswer(inv -> evaluacionCreada(idEvaluacion, inv.getArgument(1)));
            when(accionMejoraRepository.save(any(AccionMejora.class))).thenAnswer(inv -> inv.getArgument(0));

            AccionMejoraResponseDTO respuesta = service.convertirEnEvaluacion(idAccion, conversion("30"), ADMIN);

            assertThat(respuesta.convertida()).isTrue();
            assertThat(respuesta.evaluacionGeneradaId()).isEqualTo(idEvaluacion);

            ArgumentCaptor<EvaluacionCrearRequestDTO> captor = ArgumentCaptor.forClass(EvaluacionCrearRequestDTO.class);
            verify(evaluacionService).crear(eq(ID_RA), captor.capture(), any());
            assertThat(captor.getValue().nombre()).isEqualTo("Capacitación");
            assertThat(captor.getValue().descripcion()).isEqualTo("Taller de refuerzo");
            assertThat(captor.getValue().fechaTentativa()).isEqualTo(FECHA);
            assertThat(captor.getValue().porcentaje()).isEqualByComparingTo("30");
        }

        @Test
        @DisplayName("Si el servicio de evaluaciones rechaza el porcentaje (más del 100%), la acción no queda convertida")
        void porcentajeExcedidoNoMarcaLaAccion() {
            UUID idAccion = UUID.randomUUID();
            AccionMejora accion = accionNoConvertida(conclusionVigente());
            when(accionMejoraRepository.findById(idAccion)).thenReturn(Optional.of(accion));
            when(evaluacionService.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any()))
                    .thenThrow(EvaluacionReglaNegocioException.porcentajeExcedido(new BigDecimal("20")));

            assertThatThrownBy(() -> service.convertirEnEvaluacion(idAccion, conversion("25"), ADMIN))
                    .isInstanceOf(EvaluacionReglaNegocioException.class)
                    .hasMessageContaining("Porcentaje disponible: 20%");
            assertThat(accion.estaConvertida()).isFalse();
            verify(accionMejoraRepository, never()).save(any());
        }

        @Test
        @DisplayName("Una acción ya convertida se rechaza sin llamar al servicio de evaluaciones")
        void accionYaConvertidaSeRechaza() {
            UUID idAccion = UUID.randomUUID();
            AccionMejora accion = accionNoConvertida(conclusionVigente());
            accion.marcarConvertida(UUID.randomUUID());
            when(accionMejoraRepository.findById(idAccion)).thenReturn(Optional.of(accion));

            assertThatThrownBy(() -> service.convertirEnEvaluacion(idAccion, conversion("10"), ADMIN))
                    .isInstanceOf(AccionYaConvertidaException.class);
            verifyNoInteractions(evaluacionService);
        }

        @Test
        @DisplayName("Un nombre heredado de más de 30 caracteres no cumple RF-11: se rechaza sin crear la evaluación")
        void nombreHeredadoMuyLargoSeRechaza() {
            UUID idAccion = UUID.randomUUID();
            AccionMejora accion = new AccionMejora("Taller intensivo de refuerzo para el RA-01",
                    "Taller de refuerzo", resultadoAprendizaje, conclusionVigente());
            when(accionMejoraRepository.findById(idAccion)).thenReturn(Optional.of(accion));

            assertThatThrownBy(() -> service.convertirEnEvaluacion(idAccion, conversion("10"), ADMIN))
                    .isInstanceOf(AccionNoConvertibleException.class)
                    .hasMessageContaining("El nombre debe tener máximo 30 caracteres");
            verifyNoInteractions(evaluacionService);
            verify(accionMejoraRepository, never()).save(any());
        }

        @Test
        @DisplayName("Una descripción heredada con < o > no cumple RF-11: se rechaza sin crear la evaluación")
        void descripcionHeredadaConCaracteresNoPermitidosSeRechaza() {
            UUID idAccion = UUID.randomUUID();
            AccionMejora accion = new AccionMejora("Capacitación", "Taller <b>de refuerzo</b>",
                    resultadoAprendizaje, conclusionVigente());
            when(accionMejoraRepository.findById(idAccion)).thenReturn(Optional.of(accion));

            assertThatThrownBy(() -> service.convertirEnEvaluacion(idAccion, conversion("10"), ADMIN))
                    .isInstanceOf(AccionNoConvertibleException.class)
                    .hasMessageContaining("La descripción no puede contener los caracteres < o >");
            verifyNoInteractions(evaluacionService);
        }
    }

    @Nested
    @DisplayName("consultarAcciones: filtro por semestre")
    class ConsultarAccionesFiltroSemestre {

        @Test
        @DisplayName("Con semestreId filtra solo las acciones cuya conclusión origen es de ese semestre")
        void filtraPorSemestreDeLaConclusionOrigen() {
            Semestre otroSemestre = new Semestre();
            otroSemestre.setId(UUID.randomUUID());
            otroSemestre.setCodigo("2025-1");

            AccionMejora deSemestreBuscado =
                    new AccionMejora("Capacitación", "Taller de refuerzo", resultadoAprendizaje,
                            new Conclusion(resultadoAprendizaje, semestre, 60.0));
            AccionMejora deOtroSemestre =
                    new AccionMejora("Otra acción", "Otro taller", resultadoAprendizaje,
                            new Conclusion(resultadoAprendizaje, otroSemestre, 60.0));
            when(accionMejoraRepository.findAll()).thenReturn(List.of(deSemestreBuscado, deOtroSemestre));

            AccionMejoraFiltroDTO filtro = new AccionMejoraFiltroDTO(null, null, ID_SEMESTRE, null);

            List<AccionMejoraResponseDTO> resultado = service.consultarAcciones(filtro, ADMIN);

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).semestreId()).isEqualTo(ID_SEMESTRE);
            assertThat(resultado.get(0).semestreCodigo()).isEqualTo("2025-2");
        }
    }

    @Nested
    @DisplayName("Flujo completo RF-17 + RF-18")
    class FlujoCompleto {

        @Test
        @DisplayName("Conclusión vigente -> registrar acción (conclusión ATENDIDA) -> convertir "
                + "(hereda descripción, queda convertida con evaluacionGeneradaId)")
        void registrarYLuegoConvertir() {
            UUID idConclusion = UUID.randomUUID();
            UUID idAccion = UUID.randomUUID();
            UUID idEvaluacion = UUID.randomUUID();
            Conclusion conclusion = conclusionVigente();
            when(conclusionRepository.findById(idConclusion)).thenReturn(Optional.of(conclusion));
            when(accionMejoraRepository.save(any(AccionMejora.class))).thenAnswer(inv -> inv.getArgument(0));

            RegistrarAccionMejoraRequestDTO datosRegistro =
                    new RegistrarAccionMejoraRequestDTO("Capacitación", "Taller de refuerzo");
            service.registrarAccionMejora(idConclusion, datosRegistro, ADMIN);

            ArgumentCaptor<AccionMejora> captor = ArgumentCaptor.forClass(AccionMejora.class);
            verify(accionMejoraRepository).save(captor.capture());
            AccionMejora accionCreada = captor.getValue();
            ReflectionTestUtils.setField(accionCreada, "id", idAccion);

            assertThat(conclusion.getEstado()).isEqualTo(EstadoConclusion.ATENDIDA);

            when(accionMejoraRepository.findById(idAccion)).thenReturn(Optional.of(accionCreada));
            when(evaluacionService.crear(eq(ID_RA), any(EvaluacionCrearRequestDTO.class), any()))
                    .thenAnswer(inv -> evaluacionCreada(idEvaluacion, inv.getArgument(1)));

            AccionMejoraResponseDTO respuestaConversion =
                    service.convertirEnEvaluacion(idAccion, conversion("30"), ADMIN);

            assertThat(respuestaConversion.convertida()).isTrue();
            assertThat(respuestaConversion.evaluacionGeneradaId()).isEqualTo(idEvaluacion);
            assertThat(accionCreada.estaConvertida()).isTrue();

            ArgumentCaptor<EvaluacionCrearRequestDTO> evaluacionCaptor =
                    ArgumentCaptor.forClass(EvaluacionCrearRequestDTO.class);
            verify(evaluacionService).crear(eq(ID_RA), evaluacionCaptor.capture(), any());
            assertThat(evaluacionCaptor.getValue().nombre()).isEqualTo(datosRegistro.nombre());
            assertThat(evaluacionCaptor.getValue().descripcion()).isEqualTo(datosRegistro.descripcion());
        }
    }
}
