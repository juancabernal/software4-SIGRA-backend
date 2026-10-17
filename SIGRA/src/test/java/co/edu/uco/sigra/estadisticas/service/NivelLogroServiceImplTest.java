package co.edu.uco.sigra.estadisticas.service;

import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.dto.DistribucionNivelLogroDTO;
import co.edu.uco.sigra.estadisticas.dto.EstadisticasResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.GenerarConclusionResponseDTO;
import co.edu.uco.sigra.estadisticas.dto.RecalculoEstadisticasResponseDTO;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.estadisticas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.estadisticas.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
import co.edu.uco.sigra.estadisticas.service.impl.NivelLogroServiceImpl;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.repository.EstudianteRepository;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del nivel de logro y las estadísticas (RF-14 / RF-15). El repositorio de niveles se
 * simula con un mapa en memoria para poder comprobar lo que queda guardado tras cada recálculo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Servicio de nivel de logro y estadísticas (RF-14 / RF-15)")
class NivelLogroServiceImplTest {

    private static final UUID ID_RA = UUID.randomUUID();
    private static final UUID ID_ASIGNATURA = UUID.randomUUID();
    private static final UUID ID_SEMESTRE = UUID.randomUUID();
    private static final UUID ID_PROFESOR = UUID.randomUUID();
    private static final UsuarioPrincipal ADMIN =
            new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ADMINISTRADOR);

    @Mock private NivelLogroEstudianteRepository nivelLogroRepository;
    @Mock private FuenteCalificaciones fuenteCalificaciones;
    @Mock private ConclusionService conclusionService;
    @Mock private ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    @Mock private SemestreRepository semestreRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private MatriculaRepository matriculaRepository;
    @Mock private AsignacionDocenteRepository asignacionDocenteRepository;

    private NivelLogroServiceImpl service;
    private Asignatura asignatura;
    private ResultadoAprendizaje resultadoAprendizaje;
    private Semestre semestre;

    /** «Base de datos» de niveles: estudianteId → fila. */
    private final Map<UUID, NivelLogroEstudiante> guardados = new HashMap<>();
    private final List<Matricula> matriculasActivas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new NivelLogroServiceImpl(nivelLogroRepository, fuenteCalificaciones, conclusionService,
                resultadoAprendizajeRepository, semestreRepository, estudianteRepository, matriculaRepository,
                asignacionDocenteRepository);

        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setId(UUID.randomUUID());
        programa.setNombre("Ingeniería de Sistemas");
        asignatura = new Asignatura("ISW4", "Ingeniería de Software IV", programa);
        ReflectionTestUtils.setField(asignatura, "id", ID_ASIGNATURA);

        resultadoAprendizaje = new ResultadoAprendizaje();
        resultadoAprendizaje.setId(ID_RA);
        resultadoAprendizaje.setAsignatura(asignatura);
        resultadoAprendizaje.setCodigo("RA-01");

        semestre = new Semestre();
        semestre.setId(ID_SEMESTRE);
        semestre.setCodigo("2026-2");

        when(resultadoAprendizajeRepository.findById(ID_RA)).thenReturn(Optional.of(resultadoAprendizaje));
        when(semestreRepository.findById(ID_SEMESTRE)).thenReturn(Optional.of(semestre));
        when(estudianteRepository.getReferenceById(any())).thenAnswer(inv -> estudiante(inv.getArgument(0)));
        when(nivelLogroRepository.findByResultadoAprendizaje_IdAndSemestre_Id(ID_RA, ID_SEMESTRE))
                .thenAnswer(inv -> new ArrayList<>(guardados.values()));
        when(nivelLogroRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<NivelLogroEstudiante> filas = inv.getArgument(0);
            filas.forEach(f -> guardados.put(f.getEstudianteId(), f));
            return filas;
        });
        when(matriculaRepository.buscarPorAsignaturaYSemestre(ID_ASIGNATURA, ID_SEMESTRE, EstadoMatricula.ACTIVA))
                .thenReturn(matriculasActivas);
        when(conclusionService.generarConclusion(ID_RA, ID_SEMESTRE, ADMIN))
                .thenReturn(new GenerarConclusionResponseDTO(false, "motivo", null));
    }

    private static Estudiante estudiante(UUID id) {
        Estudiante e = new Estudiante();
        e.setId(id);
        return e;
    }

    private void matricular(UUID estudianteId) {
        Matricula m = new Matricula();
        m.setEstudiante(estudiante(estudianteId));
        matriculasActivas.add(m);
    }

    private void calificaciones(CalificacionPonderada... calificaciones) {
        when(fuenteCalificaciones.listarCalificaciones(ID_RA, ID_SEMESTRE)).thenReturn(List.of(calificaciones));
    }

    private static CalificacionPonderada nota(UUID estudianteId, double nota, double porcentaje) {
        return new CalificacionPonderada(estudianteId, nota, porcentaje);
    }

    private static long cantidad(EstadisticasResultadoAprendizajeResponseDTO e, NivelLogro nivel) {
        return e.distribucion().stream().filter(d -> d.nivel() == nivel).findFirst().orElseThrow().cantidad();
    }

    @Nested
    @DisplayName("Promedio ponderado")
    class PromedioPonderado {

        @Test
        @DisplayName("Pondera por el porcentaje de cada evaluación")
        void pondera() {
            UUID e = UUID.randomUUID();
            // (2.0·20 + 4.0·30 + 5.0·50) / 100 = 4.10 → ALTO
            calificaciones(nota(e, 2.0, 20), nota(e, 4.0, 30), nota(e, 5.0, 50));

            service.recalcular(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(guardados.get(e).getPromedioPonderado()).isEqualTo(4.10);
            assertThat(guardados.get(e).getNivelLogro()).isEqualTo(NivelLogro.ALTO);
            assertThat(guardados.get(e).getCantidadCalificaciones()).isEqualTo(3);
        }

        @Test
        @DisplayName("Normaliza cuando los porcentajes calificados no suman 100")
        void normaliza() {
            UUID e = UUID.randomUUID();
            // (3.0·10 + 3.0·10 + 4.5·20) / 40 = 3.75 → MEDIO (sin normalizar daría 1.5)
            calificaciones(nota(e, 3.0, 10), nota(e, 3.0, 10), nota(e, 4.5, 20));

            service.recalcular(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(guardados.get(e).getPromedioPonderado()).isEqualTo(3.75);
            assertThat(guardados.get(e).getNivelLogro()).isEqualTo(NivelLogro.MEDIO);
        }

        @Test
        @DisplayName("Redondea a 2 decimales antes de clasificar")
        void redondea() {
            UUID e = UUID.randomUUID();
            // (4.49 + 4.5 + 4.5) / 3 = 4.4966… → 4.50 → SOBRESALIENTE
            calificaciones(nota(e, 4.49, 10), nota(e, 4.5, 10), nota(e, 4.5, 10));

            service.recalcular(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(guardados.get(e).getPromedioPonderado()).isEqualTo(4.5);
            assertThat(guardados.get(e).getNivelLogro()).isEqualTo(NivelLogro.SOBRESALIENTE);
        }
    }

    @Nested
    @DisplayName("Umbral de 3 calificaciones")
    class Umbral {

        @Test
        @DisplayName("Con 2 calificaciones queda sin datos suficientes: sin promedio ni nivel")
        void dosCalificaciones() {
            UUID e = UUID.randomUUID();
            calificaciones(nota(e, 5.0, 50), nota(e, 5.0, 50));

            RecalculoEstadisticasResponseDTO r = service.recalcular(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(guardados.get(e).getCantidadCalificaciones()).isEqualTo(2);
            assertThat(guardados.get(e).getPromedioPonderado()).isNull();
            assertThat(guardados.get(e).getNivelLogro()).isNull();
            assertThat(r.estadisticas().evaluados()).isZero();
            assertThat(r.estadisticas().sinDatosSuficientes()).isEqualTo(1);
            assertThat(r.estadisticas().promedioNota()).isNull();
        }

        @Test
        @DisplayName("Con exactamente 3 calificaciones sí calcula el nivel")
        void tresCalificaciones() {
            UUID e = UUID.randomUUID();
            calificaciones(nota(e, 1.0, 10), nota(e, 2.0, 10), nota(e, 3.0, 10));

            service.recalcular(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(guardados.get(e).getNivelLogro()).isEqualTo(NivelLogro.BAJO);
        }

        @Test
        @DisplayName("Quien tenía nivel y se quedó sin calificaciones conserva su fila, en 0 y sin nivel")
        void sinCalificacionesConservaFila() {
            UUID e = UUID.randomUUID();
            calificaciones(nota(e, 4.0, 10), nota(e, 4.0, 10), nota(e, 4.0, 10));
            service.recalcular(ID_RA, ID_SEMESTRE, ADMIN);
            calificaciones();

            RecalculoEstadisticasResponseDTO r = service.recalcular(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(guardados.get(e).getCantidadCalificaciones()).isZero();
            assertThat(guardados.get(e).getNivelLogro()).isNull();
            assertThat(r.estadisticas().evaluados()).isZero();
            assertThat(r.estadisticas().sinDatosSuficientes()).isZero();
        }
    }

    @Nested
    @DisplayName("Estadísticas del RA")
    class Estadisticas {

        @Test
        @DisplayName("Distribución, promedios y conteos con evaluados, sin datos suficientes y sin evaluar")
        void estadisticasCompletas() {
            UUID bajo = UUID.randomUUID();
            UUID medio = UUID.randomUUID();
            UUID alto = UUID.randomUUID();
            UUID sobresaliente = UUID.randomUUID();
            UUID conDos = UUID.randomUUID();
            UUID sinNotas = UUID.randomUUID();
            List.of(bajo, medio, alto, sobresaliente, conDos, sinNotas).forEach(NivelLogroServiceImplTest.this::matricular);
            calificaciones(
                    nota(bajo, 2.0, 10), nota(bajo, 2.0, 10), nota(bajo, 2.0, 10),
                    nota(medio, 3.0, 10), nota(medio, 3.0, 10), nota(medio, 3.0, 10),
                    nota(alto, 4.0, 10), nota(alto, 4.0, 10), nota(alto, 4.0, 10),
                    nota(sobresaliente, 5.0, 10), nota(sobresaliente, 5.0, 10), nota(sobresaliente, 5.0, 10),
                    nota(conDos, 5.0, 10), nota(conDos, 5.0, 10));

            EstadisticasResultadoAprendizajeResponseDTO e = service.recalcular(ID_RA, ID_SEMESTRE, ADMIN).estadisticas();

            assertThat(e.totalEstudiantes()).isEqualTo(6);
            assertThat(e.evaluados()).isEqualTo(4);
            assertThat(e.sinDatosSuficientes()).isEqualTo(1);
            assertThat(e.sinEvaluar()).isEqualTo(1);
            assertThat(e.promedioNota()).isEqualTo(3.5);
            assertThat(e.promedioPorcentaje()).isEqualTo(70.0);
            assertThat(e.distribucion()).extracting(DistribucionNivelLogroDTO::nivel)
                    .containsExactly(NivelLogro.BAJO, NivelLogro.MEDIO, NivelLogro.ALTO, NivelLogro.SOBRESALIENTE);
            assertThat(e.distribucion()).extracting(DistribucionNivelLogroDTO::porcentaje)
                    .containsExactly(25.0, 25.0, 25.0, 25.0);
            assertThat(e.codigoResultadoAprendizaje()).isEqualTo("RA-01");
            assertThat(e.codigoSemestre()).isEqualTo("2026-2");
            assertThat(e.fechaCalculo()).isNotNull();
        }

        @Test
        @DisplayName("Sin calificaciones: los 4 niveles en 0 y todos los matriculados «Sin evaluar»")
        void sinCalificaciones() {
            matricular(UUID.randomUUID());
            matricular(UUID.randomUUID());

            EstadisticasResultadoAprendizajeResponseDTO e =
                    service.consultarEstadisticas(ID_RA, ID_SEMESTRE, ADMIN);

            assertThat(e.totalEstudiantes()).isEqualTo(2);
            assertThat(e.sinEvaluar()).isEqualTo(2);
            assertThat(e.evaluados()).isZero();
            assertThat(e.promedioNota()).isNull();
            assertThat(e.promedioPorcentaje()).isNull();
            assertThat(e.distribucion()).hasSize(4).allMatch(d -> d.cantidad() == 0 && d.porcentaje() == 0.0);
            assertThat(e.fechaCalculo()).isNull();
        }

        @Test
        @DisplayName("Un desvinculado con calificaciones sigue contando (no está en las matrículas activas)")
        void desvinculadoConCalificaciones() {
            UUID desvinculado = UUID.randomUUID();
            calificaciones(nota(desvinculado, 4.0, 10), nota(desvinculado, 4.0, 10), nota(desvinculado, 4.0, 10));

            EstadisticasResultadoAprendizajeResponseDTO e = service.recalcular(ID_RA, ID_SEMESTRE, ADMIN).estadisticas();

            assertThat(e.totalEstudiantes()).isEqualTo(1);
            assertThat(e.evaluados()).isEqualTo(1);
            assertThat(cantidad(e, NivelLogro.ALTO)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Integración con RF-16")
    class ConclusionRf16 {

        @Test
        @DisplayName("Al recalcular guarda los niveles y después llama a generarConclusion")
        void llamaGenerarConclusion() {
            UUID e = UUID.randomUUID();
            calificaciones(nota(e, 2.0, 10), nota(e, 2.0, 10), nota(e, 2.0, 10));
            GenerarConclusionResponseDTO respuestaConclusion = new GenerarConclusionResponseDTO(true, "generada", null);
            when(conclusionService.generarConclusion(ID_RA, ID_SEMESTRE, ADMIN)).thenReturn(respuestaConclusion);

            RecalculoEstadisticasResponseDTO r = service.recalcular(ID_RA, ID_SEMESTRE, ADMIN);

            InOrder orden = inOrder(nivelLogroRepository, conclusionService);
            orden.verify(nivelLogroRepository).saveAll(any());
            orden.verify(nivelLogroRepository).flush();
            orden.verify(conclusionService).generarConclusion(ID_RA, ID_SEMESTRE, ADMIN);
            assertThat(r.conclusion()).isSameAs(respuestaConclusion);
        }
    }

    @Nested
    @DisplayName("Validaciones y alcance por rol")
    class Validaciones {

        @Test
        @DisplayName("RA inexistente: 404 y no recalcula ni genera conclusión")
        void raInexistente() {
            UUID otro = UUID.randomUUID();
            when(resultadoAprendizajeRepository.findById(otro)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.recalcular(otro, ID_SEMESTRE, ADMIN))
                    .isInstanceOf(ResultadoAprendizajeNoEncontradoException.class);
            verify(conclusionService, never()).generarConclusion(any(), any(), any());
        }

        @Test
        @DisplayName("Semestre inexistente: 404")
        void semestreInexistente() {
            UUID otro = UUID.randomUUID();
            when(semestreRepository.findById(otro)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consultarEstadisticas(ID_RA, otro, ADMIN))
                    .isInstanceOf(SemestreNoEncontradoException.class);
        }

        @Test
        @DisplayName("PROFESOR asignado a la asignatura consulta las estadísticas")
        void profesorAsignado() {
            AsignacionDocente asignacion = new AsignacionDocente();
            asignacion.setAsignatura(asignatura);
            when(asignacionDocenteRepository.findByProfesor_IdAndEstado(ID_PROFESOR, EstadoRegistro.ACTIVO))
                    .thenReturn(List.of(asignacion));

            assertThat(service.consultarEstadisticas(ID_RA, ID_SEMESTRE,
                    new UsuarioPrincipal(ID_PROFESOR, RolUsuario.PROFESOR))).isNotNull();
        }

        @Test
        @DisplayName("PROFESOR sin asignación activa en la asignatura: 403 y no recalcula")
        void profesorNoAsignado() {
            when(asignacionDocenteRepository.findByProfesor_IdAndEstado(ID_PROFESOR, EstadoRegistro.ACTIVO))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> service.recalcular(ID_RA, ID_SEMESTRE,
                    new UsuarioPrincipal(ID_PROFESOR, RolUsuario.PROFESOR)))
                    .isInstanceOf(PermisoInsuficienteException.class);
            verify(nivelLogroRepository, never()).saveAll(any());
            verify(conclusionService, never()).generarConclusion(any(), any(), any());
        }

        @Test
        @DisplayName("Sin identidad: falla cerrado con 403 y no recalcula")
        void sinIdentidad() {
            assertThatThrownBy(() -> service.recalcular(ID_RA, ID_SEMESTRE, null))
                    .isInstanceOf(PermisoInsuficienteException.class);
            verify(nivelLogroRepository, never()).saveAll(any());
            verify(conclusionService, never()).generarConclusion(any(), any(), any());
        }

        @Test
        @DisplayName("ESTUDIANTE: 403")
        void estudiante() {
            assertThatThrownBy(() -> service.consultarEstadisticas(ID_RA, ID_SEMESTRE,
                    new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ESTUDIANTE)))
                    .isInstanceOf(PermisoInsuficienteException.class);
        }
    }

    @Test
    @DisplayName("Una calificación con nota fuera de 0–5 o porcentaje no positivo se rechaza")
    void calificacionInvalida() {
        UUID e = UUID.randomUUID();
        assertThatThrownBy(() -> nota(e, 5.5, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> nota(e, 4.0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
