package co.edu.uco.sigra.resultadosaprendizaje.service;

import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.evaluaciones.repository.EvaluacionRepository;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.resultadosaprendizaje.dto.EstadoRendimiento;
import co.edu.uco.sigra.resultadosaprendizaje.dto.RendimientoAsignaturaResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.RendimientoResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.resultadosaprendizaje.service.impl.RendimientoResultadoAprendizajeServiceImpl;
import co.edu.uco.sigra.semestres.entity.Semestre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del rendimiento de los RA (RF-14 / RF-15). Los repositorios se simulan con Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de rendimiento de resultados de aprendizaje")
class RendimientoResultadoAprendizajeServiceImplTest {

    private static final UUID ID_ASIGNATURA = UUID.randomUUID();
    private static final UUID ID_SEMESTRE = UUID.randomUUID();
    private static final UUID ID_RA = UUID.randomUUID();

    @Mock
    private AsignaturaRepository asignaturaRepository;
    @Mock
    private ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    @Mock
    private EvaluacionService evaluacionService;
    @Mock
    private EvaluacionRepository evaluacionRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private NivelLogroEstudianteRepository nivelLogroEstudianteRepository;

    private RendimientoResultadoAprendizajeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RendimientoResultadoAprendizajeServiceImpl(asignaturaRepository,
                resultadoAprendizajeRepository, evaluacionService, evaluacionRepository, matriculaRepository,
                nivelLogroEstudianteRepository);
    }

    // ------------------------------------------------------------------ datos de prueba

    private static Semestre semestre() {
        Semestre s = new Semestre();
        s.setId(ID_SEMESTRE);
        s.setCodigo("2026-2");
        return s;
    }

    private static ResultadoAprendizaje ra() {
        ResultadoAprendizaje ra = new ResultadoAprendizaje();
        ra.setId(ID_RA);
        ra.setCodigo("RA1");
        return ra;
    }

    private static NivelLogroEstudiante nivel(int cantidad, Double promedio, NivelLogro nivel) {
        NivelLogroEstudiante n = mock(NivelLogroEstudiante.class);
        lenient().when(n.getCantidadCalificaciones()).thenReturn(cantidad);
        lenient().when(n.getPromedioPonderado()).thenReturn(promedio);
        lenient().when(n.getNivelLogro()).thenReturn(nivel);
        return n;
    }

    private static Evaluacion evaluacion(UUID id, String porcentaje) {
        Evaluacion e = new Evaluacion();
        e.setId(id);
        e.setPorcentaje(new BigDecimal(porcentaje));
        e.setEstado(EstadoEvaluacion.ACTIVA);
        return e;
    }

    private void escenarioBase(int matriculados) {
        when(asignaturaRepository.existsById(ID_ASIGNATURA)).thenReturn(true);
        when(evaluacionService.consultarSemestreActivo()).thenReturn(Optional.of(semestre()));
        when(matriculaRepository.buscarPorAsignaturaYSemestre(ID_ASIGNATURA, ID_SEMESTRE, EstadoMatricula.ACTIVA))
                .thenReturn(Collections.nCopies(matriculados, mock(Matricula.class)));
        when(resultadoAprendizajeRepository.findByAsignatura_IdAndEstadoOrderByCodigoAsc(ID_ASIGNATURA,
                EstadoRegistro.ACTIVO)).thenReturn(List.of(ra()));
    }

    private void niveles(NivelLogroEstudiante... filas) {
        when(nivelLogroEstudianteRepository.findByResultadoAprendizaje_IdAndSemestre_Id(ID_RA, ID_SEMESTRE))
                .thenReturn(List.of(filas));
    }

    private void porcentajes(String asignado, List<Evaluacion> activas, Object[]... conteos) {
        when(evaluacionRepository.sumarPorcentajes(ID_RA, ID_SEMESTRE, EstadoEvaluacion.ACTIVA))
                .thenReturn(new BigDecimal(asignado));
        lenient().when(evaluacionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(ID_RA, ID_SEMESTRE,
                EstadoEvaluacion.ACTIVA)).thenReturn(activas);
        lenient().when(evaluacionRepository.contarCalificacionesPorEvaluacion(ID_RA, ID_SEMESTRE,
                EstadoEvaluacion.ACTIVA, EstadoMatricula.ACTIVA)).thenReturn(List.of(conteos));
    }

    private RendimientoResultadoAprendizajeResponseDTO unico() {
        RendimientoAsignaturaResponseDTO r = service.consultarPorAsignatura(ID_ASIGNATURA);
        assertThat(r.resultados()).hasSize(1);
        return r.resultados().get(0);
    }

    // ------------------------------------------------------------------ pruebas

    @Test
    @DisplayName("Asignatura inexistente: AsignaturaNoEncontradaException")
    void asignaturaNoExiste() {
        when(asignaturaRepository.existsById(ID_ASIGNATURA)).thenReturn(false);

        assertThatThrownBy(() -> service.consultarPorAsignatura(ID_ASIGNATURA))
                .isInstanceOf(AsignaturaNoEncontradaException.class);
        verifyNoInteractions(evaluacionService, resultadoAprendizajeRepository);
    }

    @Test
    @DisplayName("Sin semestre activo: respuesta vacía con semestre nulo")
    void sinSemestreActivo() {
        when(asignaturaRepository.existsById(ID_ASIGNATURA)).thenReturn(true);
        when(evaluacionService.consultarSemestreActivo()).thenReturn(Optional.empty());

        RendimientoAsignaturaResponseDTO r = service.consultarPorAsignatura(ID_ASIGNATURA);

        assertThat(r.semestreId()).isNull();
        assertThat(r.semestreCodigo()).isNull();
        assertThat(r.resultados()).isEmpty();
        verifyNoInteractions(resultadoAprendizajeRepository);
    }

    @Test
    @DisplayName("Sin calificaciones: SIN_EVALUAR, sin nivel y sin promedio")
    void sinEvaluar() {
        escenarioBase(2);
        niveles();
        porcentajes("40", List.of(evaluacion(UUID.randomUUID(), "40")));

        RendimientoResultadoAprendizajeResponseDTO dto = unico();

        assertThat(dto.estado()).isEqualTo(EstadoRendimiento.SIN_EVALUAR);
        assertThat(dto.nivelLogro()).isNull();
        assertThat(dto.promedioNota()).isNull();
        assertThat(dto.porcentajeAsignado()).isEqualByComparingTo("40");
        assertThat(dto.porcentajeEvaluado()).isEqualByComparingTo("0");
        assertThat(dto.completo()).isFalse();
    }

    @Test
    @DisplayName("Hay notas pero nadie llega a 3: DATOS_INSUFICIENTES")
    void datosInsuficientes() {
        escenarioBase(2);
        niveles(nivel(2, null, null), nivel(1, null, null));
        porcentajes("40", List.of());

        RendimientoResultadoAprendizajeResponseDTO dto = unico();

        assertThat(dto.estado()).isEqualTo(EstadoRendimiento.DATOS_INSUFICIENTES);
        assertThat(dto.nivelLogro()).isNull();
    }

    @Test
    @DisplayName("Con nivel: promedia los promedios ponderados y clasifica (parcial)")
    void conNivelParcial() {
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        escenarioBase(2);
        // 4.5 y 4.0 -> 4.25 -> ALTO; el estudiante con 2 notas no cuenta
        niveles(nivel(3, 4.5, NivelLogro.SOBRESALIENTE), nivel(3, 4.0, NivelLogro.ALTO), nivel(2, null, null));
        porcentajes("100", List.of(evaluacion(e1, "60"), evaluacion(e2, "40")),
                new Object[]{e1, 2L}, new Object[]{e2, 1L});

        RendimientoResultadoAprendizajeResponseDTO dto = unico();

        assertThat(dto.estado()).isEqualTo(EstadoRendimiento.CON_NIVEL);
        assertThat(dto.promedioNota()).isEqualTo(4.25);
        assertThat(dto.nivelLogro()).isEqualTo(NivelLogro.ALTO);
        assertThat(dto.porcentajeAsignado()).isEqualByComparingTo("100");
        assertThat(dto.porcentajeEvaluado()).isEqualByComparingTo("60");
        assertThat(dto.completo()).isFalse();
    }

    @Test
    @DisplayName("Completo: asignado 100% y todas las evaluaciones calificadas a todos los matriculados")
    void conNivelCompleto() {
        UUID e1 = UUID.randomUUID();
        UUID e2 = UUID.randomUUID();
        escenarioBase(2);
        niveles(nivel(3, 3.2, NivelLogro.MEDIO), nivel(3, 3.4, NivelLogro.MEDIO));
        porcentajes("100", List.of(evaluacion(e1, "60"), evaluacion(e2, "40")),
                new Object[]{e1, 2L}, new Object[]{e2, 2L});

        RendimientoResultadoAprendizajeResponseDTO dto = unico();

        assertThat(dto.nivelLogro()).isEqualTo(NivelLogro.MEDIO);
        assertThat(dto.porcentajeEvaluado()).isEqualByComparingTo("100");
        assertThat(dto.completo()).isTrue();
    }

    @Test
    @DisplayName("Sin matriculados activos el porcentaje evaluado es 0 y no se consultan conteos")
    void sinMatriculados() {
        escenarioBase(0);
        niveles();
        when(evaluacionRepository.sumarPorcentajes(ID_RA, ID_SEMESTRE, EstadoEvaluacion.ACTIVA))
                .thenReturn(new BigDecimal("100"));

        RendimientoResultadoAprendizajeResponseDTO dto = unico();

        assertThat(dto.porcentajeEvaluado()).isEqualByComparingTo("0");
        assertThat(dto.completo()).isFalse();
    }
}
