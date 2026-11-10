package co.edu.uco.sigra.reportes.service;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.dto.DistribucionNivelLogroDTO;
import co.edu.uco.sigra.estadisticas.dto.EstadisticasResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.service.NivelLogroService;
import co.edu.uco.sigra.evaluaciones.repository.EvaluacionRepository;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.reportes.dto.RaRendimientoReporteDTO;
import co.edu.uco.sigra.reportes.dto.RendimientoRaReporteResponseDTO;
import co.edu.uco.sigra.reportes.exception.PermisoReporteInsuficienteException;
import co.edu.uco.sigra.reportes.exception.ReporteRecursoNoEncontradoException;
import co.edu.uco.sigra.reportes.service.impl.RendimientoRaReporteServiceImpl;
import co.edu.uco.sigra.resultadosaprendizaje.dto.EstadoRendimiento;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Reporte R1: rendimiento por RA y asignatura")
class RendimientoRaReporteServiceImplTest {

    private final UUID semestreId = UUID.randomUUID();
    private final UUID programaId = UUID.randomUUID();
    private final UUID asignaturaId = UUID.randomUUID();
    private final UUID raId = UUID.randomUUID();
    private final UUID profesorId = UUID.randomUUID();

    @Mock private AsignaturaRepository asignaturaRepository;
    @Mock private ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    @Mock private ProgramaAcademicoRepository programaRepository;
    @Mock private SemestreRepository semestreRepository;
    @Mock private EvaluacionService evaluacionService;
    @Mock private EvaluacionRepository evaluacionRepository;
    @Mock private NivelLogroService nivelLogroService;

    private RendimientoRaReporteServiceImpl servicio;
    private final UsuarioPrincipal admin = new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ADMINISTRADOR);
    private Asignatura asignatura;

    @BeforeEach
    void preparar() {
        servicio = new RendimientoRaReporteServiceImpl(asignaturaRepository, resultadoAprendizajeRepository,
                programaRepository, semestreRepository, evaluacionService, evaluacionRepository, nivelLogroService);

        Semestre semestre = mock(Semestre.class);
        when(semestre.getId()).thenReturn(semestreId);
        when(semestre.getCodigo()).thenReturn("2026-2");
        when(evaluacionService.consultarSemestreActivo()).thenReturn(Optional.of(semestre));
        when(semestreRepository.findById(semestreId)).thenReturn(Optional.of(semestre));

        ProgramaAcademico programa = mock(ProgramaAcademico.class);
        when(programa.getId()).thenReturn(programaId);
        when(programa.getNombre()).thenReturn("Ingeniería de Sistemas");
        asignatura = mock(Asignatura.class);
        when(asignatura.getId()).thenReturn(asignaturaId);
        when(asignatura.getCodigo()).thenReturn("SIS101");
        when(asignatura.getNombre()).thenReturn("Programación");
        when(asignatura.getPrograma()).thenReturn(programa);
        when(asignaturaRepository.existsById(asignaturaId)).thenReturn(true);
        when(programaRepository.existsById(programaId)).thenReturn(true);
        when(asignaturaRepository.findAll()).thenReturn(List.of(asignatura));

        ResultadoAprendizaje ra = mock(ResultadoAprendizaje.class);
        when(ra.getId()).thenReturn(raId);
        when(ra.getCodigo()).thenReturn("RA-01");
        when(ra.getDescripcion()).thenReturn("Descripción del RA");
        when(resultadoAprendizajeRepository.findByAsignatura_IdAndEstadoOrderByCodigoAsc(asignaturaId,
                EstadoRegistro.ACTIVO)).thenReturn(List.of(ra));
    }

    private void simular(Double promedio, long calificaciones, long evaluados, long sinDatos) {
        when(nivelLogroService.consultarEstadisticas(eq(raId), eq(semestreId), any()))
                .thenReturn(new EstadisticasResultadoAprendizajeResponseDTO(raId, "RA-01", asignaturaId, semestreId,
                        "2026-2", evaluados + sinDatos, evaluados, sinDatos, 0, promedio, null,
                        List.of(new DistribucionNivelLogroDTO(NivelLogro.BAJO, 0, 0.0)), null));
        List<Object[]> filas = calificaciones == 0 ? List.of() : List.<Object[]>of(new Object[]{UUID.randomUUID(), calificaciones});
        when(evaluacionRepository.contarCalificacionesPorEvaluacion(eq(raId), eq(semestreId), any(), any()))
                .thenReturn(filas);
    }

    private RaRendimientoReporteDTO unicoRa(RendimientoRaReporteResponseDTO reporte) {
        return reporte.asignaturas().get(0).resultados().get(0);
    }

    @ParameterizedTest(name = "promedio {0} → {1}")
    @CsvSource({"2.9, BAJO", "3.0, MEDIO", "3.9, MEDIO", "4.0, ALTO", "4.5, SOBRESALIENTE", "4.6, SOBRESALIENTE"})
    @DisplayName("Clasifica el nivel del RA en los límites de RF-14 según el catálogo vigente")
    void clasificaEnLosLimites(double promedio, NivelLogro esperado) {
        simular(promedio, 15, 5, 0);

        RaRendimientoReporteDTO ra = unicoRa(servicio.consultar(null, null, null, admin));

        assertThat(ra.estado()).isEqualTo(EstadoRendimiento.CON_NIVEL);
        assertThat(ra.nivelLogro()).isEqualTo(esperado);
        assertThat(ra.promedioNota()).isEqualTo(promedio);
        assertThat(ra.cantidadCalificaciones()).isEqualTo(15);
        assertThat(ra.mensaje()).isNull();
    }

    @Test
    @DisplayName("Cero calificaciones: «Sin evaluar»")
    void sinCalificaciones() {
        simular(null, 0, 0, 0);

        RaRendimientoReporteDTO ra = unicoRa(servicio.consultar(null, null, null, admin));

        assertThat(ra.estado()).isEqualTo(EstadoRendimiento.SIN_EVALUAR);
        assertThat(ra.nivelLogro()).isNull();
        assertThat(ra.mensaje()).isEqualTo("Sin evaluar");
    }

    @Test
    @DisplayName("1 o 2 calificaciones: «Sin datos suficientes para calcular»")
    void pocasCalificaciones() {
        simular(null, 2, 0, 1);

        RaRendimientoReporteDTO ra = unicoRa(servicio.consultar(null, null, null, admin));

        assertThat(ra.estado()).isEqualTo(EstadoRendimiento.DATOS_INSUFICIENTES);
        assertThat(ra.nivelLogro()).isNull();
        assertThat(ra.promedioNota()).isNull();
        assertThat(ra.mensaje()).isEqualTo("Sin datos suficientes para calcular");
    }

    @Test
    @DisplayName("Sin asignaturas con RA: «Sin datos disponibles»")
    void sinResultados() {
        when(resultadoAprendizajeRepository.findByAsignatura_IdAndEstadoOrderByCodigoAsc(asignaturaId,
                EstadoRegistro.ACTIVO)).thenReturn(List.of());

        RendimientoRaReporteResponseDTO reporte = servicio.consultar(null, null, null, admin);

        assertThat(reporte.asignaturas()).isEmpty();
        assertThat(reporte.mensaje()).isEqualTo("Sin datos disponibles");
    }

    @Test
    @DisplayName("Sin semestre activo: reporte vacío con «Sin datos disponibles»")
    void sinSemestreActivo() {
        when(evaluacionService.consultarSemestreActivo()).thenReturn(Optional.empty());

        RendimientoRaReporteResponseDTO reporte = servicio.consultar(null, null, null, admin);

        assertThat(reporte.semestreId()).isNull();
        assertThat(reporte.asignaturas()).isEmpty();
        assertThat(reporte.mensaje()).isEqualTo("Sin datos disponibles");
    }

    @Test
    @DisplayName("Semestre, programa o asignatura inexistentes: 404 sin identificadores en el mensaje")
    void filtrosInexistentes() {
        UUID otro = UUID.randomUUID();
        assertThatThrownBy(() -> servicio.consultar(otro, null, null, admin))
                .isInstanceOf(ReporteRecursoNoEncontradoException.class).hasMessageNotContaining(otro.toString());
        assertThatThrownBy(() -> servicio.consultar(null, otro, null, admin))
                .isInstanceOf(ReporteRecursoNoEncontradoException.class).hasMessageNotContaining(otro.toString());
        assertThatThrownBy(() -> servicio.consultar(null, null, otro, admin))
                .isInstanceOf(ReporteRecursoNoEncontradoException.class).hasMessageNotContaining(otro.toString());
    }

    @Test
    @DisplayName("Un filtro de programa distinto deja el reporte sin datos")
    void filtroProgramaSinCoincidencia() {
        UUID otroPrograma = UUID.randomUUID();
        when(programaRepository.existsById(otroPrograma)).thenReturn(true);
        simular(4.0, 15, 5, 0);

        RendimientoRaReporteResponseDTO reporte = servicio.consultar(null, otroPrograma, null, admin);

        assertThat(reporte.asignaturas()).isEmpty();
        assertThat(reporte.mensaje()).isEqualTo("Sin datos disponibles");
    }

    @Test
    @DisplayName("El estudiante y la identidad ausente reciben 403")
    void rolesNoPermitidos() {
        assertThatThrownBy(() -> servicio.consultar(null, null, null,
                new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ESTUDIANTE)))
                .isInstanceOf(PermisoReporteInsuficienteException.class);
        assertThatThrownBy(() -> servicio.consultar(null, null, null, null))
                .isInstanceOf(PermisoReporteInsuficienteException.class);
    }

    @Test
    @DisplayName("El profesor solo ve sus asignaturas y recibe 403 si pide una ajena")
    void profesorSoloSusAsignaturas() {
        UsuarioPrincipal profesor = new UsuarioPrincipal(profesorId, RolUsuario.PROFESOR);
        when(asignaturaRepository.findAsignadasAProfesor(profesorId, EstadoRegistro.ACTIVO)).thenReturn(List.of());

        assertThat(servicio.consultar(null, null, null, profesor).asignaturas()).isEmpty();
        assertThatThrownBy(() -> servicio.consultar(null, null, asignaturaId, profesor))
                .isInstanceOf(PermisoReporteInsuficienteException.class);
        verify(asignaturaRepository, never()).findAll();
    }

    @Test
    @DisplayName("El profesor con la asignatura asignada obtiene su reporte")
    void profesorConAsignacion() {
        UsuarioPrincipal profesor = new UsuarioPrincipal(profesorId, RolUsuario.PROFESOR);
        when(asignaturaRepository.findAsignadasAProfesor(profesorId, EstadoRegistro.ACTIVO))
                .thenReturn(List.of(asignatura));
        simular(3.5, 12, 4, 0);

        RendimientoRaReporteResponseDTO reporte = servicio.consultar(null, programaId, asignaturaId, profesor);

        assertThat(reporte.asignaturas()).hasSize(1);
        assertThat(unicoRa(reporte).nivelLogro()).isEqualTo(NivelLogro.MEDIO);
    }
}
