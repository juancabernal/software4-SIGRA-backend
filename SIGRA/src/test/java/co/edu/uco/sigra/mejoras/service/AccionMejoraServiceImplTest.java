package co.edu.uco.sigra.mejoras.service;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.repository.ConclusionRepository;
import co.edu.uco.sigra.mejoras.dto.AccionMejoraResponseDTO;
import co.edu.uco.sigra.mejoras.dto.ConvertirEvaluacionRequestDTO;
import co.edu.uco.sigra.mejoras.dto.RegistrarAccionMejoraRequestDTO;
import co.edu.uco.sigra.mejoras.entity.AccionMejora;
import co.edu.uco.sigra.mejoras.entity.RegistroEvaluacionPrueba;
import co.edu.uco.sigra.mejoras.exception.AccionYaConvertidaException;
import co.edu.uco.sigra.mejoras.exception.ConclusionNoVigenteException;
import co.edu.uco.sigra.mejoras.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.mejoras.exception.PorcentajeExcedeLimiteException;
import co.edu.uco.sigra.mejoras.mapper.AccionMejoraMapper;
import co.edu.uco.sigra.mejoras.repository.AccionMejoraRepository;
import co.edu.uco.sigra.mejoras.repository.RegistroEvaluacionPruebaRepository;
import co.edu.uco.sigra.mejoras.service.impl.AccionMejoraServiceImpl;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
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
 * Pruebas unitarias del servicio de acciones de mejora (RF-17) y su conversión en evaluación
 * (RF-18). Los repositorios se simulan con Mockito; el mapper es el real generado por MapStruct.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de acciones de mejora (RF-17/RF-18)")
class AccionMejoraServiceImplTest {

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
    private RegistroEvaluacionPruebaRepository registroEvaluacionPruebaRepository;

    private final AccionMejoraMapper mapper = Mappers.getMapper(AccionMejoraMapper.class);

    private AccionMejoraServiceImpl service;
    private Asignatura asignatura;
    private ResultadoAprendizaje resultadoAprendizaje;
    private Semestre semestre;

    @BeforeEach
    void setUp() {
        service = new AccionMejoraServiceImpl(accionMejoraRepository, conclusionRepository,
                asignacionDocenteRepository, mapper, registroEvaluacionPruebaRepository);

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

            assertThatThrownBy(() -> service.registrarAccionMejora(idConclusion, datos, null))
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

            AccionMejoraResponseDTO respuesta = service.registrarAccionMejora(idConclusion, datos, null);

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
                    new IdentidadActual(profesorId, RolUsuario.PROFESOR)))
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
        @DisplayName("Un porcentaje que hace superar el 100% se rechaza")
        void porcentajeExcedeLimiteSeRechaza() {
            UUID idAccion = UUID.randomUUID();
            AccionMejora accion = accionNoConvertida(conclusionVigente());
            when(accionMejoraRepository.findById(idAccion)).thenReturn(Optional.of(accion));
            when(registroEvaluacionPruebaRepository.sumarPorcentajes(ID_RA, ID_SEMESTRE)).thenReturn(80.0);

            ConvertirEvaluacionRequestDTO datos = new ConvertirEvaluacionRequestDTO(25.0);

            assertThatThrownBy(() -> service.convertirEnEvaluacion(idAccion, datos, null))
                    .isInstanceOf(PorcentajeExcedeLimiteException.class);
            verify(registroEvaluacionPruebaRepository, never()).save(any());
            verify(accionMejoraRepository, never()).save(any());
        }

        @Test
        @DisplayName("Si la suma queda exactamente en 100% no se rechaza")
        void enElLimiteExactoNoSeRechaza() {
            UUID idAccion = UUID.randomUUID();
            AccionMejora accion = accionNoConvertida(conclusionVigente());
            when(accionMejoraRepository.findById(idAccion)).thenReturn(Optional.of(accion));
            when(registroEvaluacionPruebaRepository.sumarPorcentajes(ID_RA, ID_SEMESTRE)).thenReturn(70.0);
            when(registroEvaluacionPruebaRepository.save(any(RegistroEvaluacionPrueba.class))).thenAnswer(inv -> {
                RegistroEvaluacionPrueba registro = inv.getArgument(0);
                ReflectionTestUtils.setField(registro, "id", UUID.randomUUID());
                return registro;
            });
            when(accionMejoraRepository.save(any(AccionMejora.class))).thenAnswer(inv -> inv.getArgument(0));

            ConvertirEvaluacionRequestDTO datos = new ConvertirEvaluacionRequestDTO(30.0);

            AccionMejoraResponseDTO respuesta = service.convertirEnEvaluacion(idAccion, datos, null);

            assertThat(respuesta.convertida()).isTrue();
        }

        @Test
        @DisplayName("Una acción ya convertida se rechaza sin consultar la suma de porcentajes")
        void accionYaConvertidaSeRechaza() {
            UUID idAccion = UUID.randomUUID();
            AccionMejora accion = accionNoConvertida(conclusionVigente());
            accion.marcarConvertida(UUID.randomUUID());
            when(accionMejoraRepository.findById(idAccion)).thenReturn(Optional.of(accion));

            ConvertirEvaluacionRequestDTO datos = new ConvertirEvaluacionRequestDTO(10.0);

            assertThatThrownBy(() -> service.convertirEnEvaluacion(idAccion, datos, null))
                    .isInstanceOf(AccionYaConvertidaException.class);
            verifyNoInteractions(registroEvaluacionPruebaRepository);
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
            service.registrarAccionMejora(idConclusion, datosRegistro, null);

            ArgumentCaptor<AccionMejora> captor = ArgumentCaptor.forClass(AccionMejora.class);
            verify(accionMejoraRepository).save(captor.capture());
            AccionMejora accionCreada = captor.getValue();
            ReflectionTestUtils.setField(accionCreada, "id", idAccion);

            assertThat(conclusion.getEstado()).isEqualTo(EstadoConclusion.ATENDIDA);

            when(accionMejoraRepository.findById(idAccion)).thenReturn(Optional.of(accionCreada));
            when(registroEvaluacionPruebaRepository.sumarPorcentajes(ID_RA, ID_SEMESTRE)).thenReturn(0.0);
            when(registroEvaluacionPruebaRepository.save(any(RegistroEvaluacionPrueba.class))).thenAnswer(inv -> {
                RegistroEvaluacionPrueba registro = inv.getArgument(0);
                ReflectionTestUtils.setField(registro, "id", idEvaluacion);
                return registro;
            });

            AccionMejoraResponseDTO respuestaConversion =
                    service.convertirEnEvaluacion(idAccion, new ConvertirEvaluacionRequestDTO(30.0), null);

            assertThat(respuestaConversion.convertida()).isTrue();
            assertThat(respuestaConversion.evaluacionGeneradaId()).isEqualTo(idEvaluacion);
            assertThat(accionCreada.estaConvertida()).isTrue();

            ArgumentCaptor<RegistroEvaluacionPrueba> evaluacionCaptor =
                    ArgumentCaptor.forClass(RegistroEvaluacionPrueba.class);
            verify(registroEvaluacionPruebaRepository).save(evaluacionCaptor.capture());
            assertThat(evaluacionCaptor.getValue().getDescripcion()).isEqualTo(datosRegistro.descripcion());
        }
    }
}
