package co.edu.uco.sigra.reportes.service;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.reportes.dto.EstudianteRiesgoDTO;
import co.edu.uco.sigra.reportes.dto.RiesgoAcademicoReporteResponseDTO;
import co.edu.uco.sigra.reportes.dto.SeveridadRiesgo;
import co.edu.uco.sigra.reportes.exception.PermisoReporteInsuficienteException;
import co.edu.uco.sigra.reportes.exception.ReporteRecursoNoEncontradoException;
import co.edu.uco.sigra.reportes.service.impl.RiesgoAcademicoReporteServiceImpl;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Reporte R6: riesgo académico (RA no alcanzados)")
class RiesgoAcademicoReporteServiceImplTest {

    private final UUID semestreId = UUID.randomUUID();
    private final UUID programaId = UUID.randomUUID();
    private final UUID asignaturaId = UUID.randomUUID();
    private final UUID estudianteId = UUID.randomUUID();
    private final UUID profesorId = UUID.randomUUID();

    @Mock private AsignaturaRepository asignaturaRepository;
    @Mock private ProgramaAcademicoRepository programaRepository;
    @Mock private SemestreRepository semestreRepository;
    @Mock private EvaluacionService evaluacionService;
    @Mock private NivelLogroEstudianteRepository nivelLogroEstudianteRepository;

    private RiesgoAcademicoReporteServiceImpl servicio;
    private final UsuarioPrincipal admin = new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ADMINISTRADOR);
    private Asignatura asignatura;
    private Estudiante estudiante;

    @BeforeEach
    void preparar() {
        servicio = new RiesgoAcademicoReporteServiceImpl(
                asignaturaRepository, programaRepository, semestreRepository, evaluacionService,
                nivelLogroEstudianteRepository);

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

        estudiante = mock(Estudiante.class);
        when(estudiante.getId()).thenReturn(estudianteId);
        when(estudiante.getNombreCompleto()).thenReturn("Mariana Salazar Duque");
        when(estudiante.getPrimerNombre()).thenReturn("Mariana");
        when(estudiante.getSegundoNombre()).thenReturn(null);
        when(estudiante.getPrimerApellido()).thenReturn("Salazar");
        when(estudiante.getSegundoApellido()).thenReturn("Duque");
        when(estudiante.getCorreoInstitucional()).thenReturn("mariana@uco.net.co");
        when(estudiante.getNumeroDocumento()).thenReturn("10458921");
    }

    private NivelLogroEstudiante fila(String codigoRa, NivelLogro nivel, double promedio) {
        ResultadoAprendizaje ra = mock(ResultadoAprendizaje.class);
        when(ra.getId()).thenReturn(UUID.randomUUID());
        when(ra.getCodigo()).thenReturn(codigoRa);
        when(ra.getDescripcion()).thenReturn("Desc " + codigoRa);
        when(ra.getAsignatura()).thenReturn(asignatura);

        NivelLogroEstudiante n = mock(NivelLogroEstudiante.class);
        when(n.getEstudiante()).thenReturn(estudiante);
        when(n.getResultadoAprendizaje()).thenReturn(ra);
        when(n.getNivelLogro()).thenReturn(nivel);
        when(n.getPromedioPonderado()).thenReturn(promedio);
        return n;
    }

    private void simularFilas(List<NivelLogroEstudiante> filas) {
        when(nivelLogroEstudianteRepository.buscarInsuficientesPorSemestreYAsignaturas(
                eq(semestreId), eq(List.of(NivelLogro.BAJO, NivelLogro.MEDIO)),
                eq(EstadoRegistro.ACTIVO), any()))
                .thenReturn(filas);
    }

    @Test
    @DisplayName("Agrupa RA insuficientes, calcula severidad y ordena por cantidad")
    void agrupaYOrdena() {
        simularFilas(List.of(
                fila("RA-02", NivelLogro.BAJO, 2.1),
                fila("RA-01", NivelLogro.MEDIO, 3.2),
                fila("RA-03", NivelLogro.BAJO, 1.5)));

        RiesgoAcademicoReporteResponseDTO reporte = servicio.consultar(null, null, null, null, admin);

        assertThat(reporte.semestreCodigo()).isEqualTo("2026-2");
        assertThat(reporte.totalEstudiantesEnRiesgo()).isEqualTo(1);
        assertThat(reporte.mensaje()).isNull();
        EstudianteRiesgoDTO fila = reporte.estudiantes().getFirst();
        assertThat(fila.cantidadRaNoAlcanzados()).isEqualTo(3);
        assertThat(fila.severidad()).isEqualTo(SeveridadRiesgo.ALTO_RIESGO);
        assertThat(fila.ras()).extracting(r -> r.codigo()).containsExactly("RA-01", "RA-02", "RA-03");
        assertThat(fila.nombreCompleto()).isEqualTo("Mariana Salazar Duque");
        assertThat(fila.numeroDocumento()).isEqualTo("10458921");
    }

    @Test
    @DisplayName("Umbral mínimo excluye filas con menos RA no alcanzados")
    void umbralMinimo() {
        simularFilas(List.of(fila("RA-01", NivelLogro.BAJO, 2.0)));

        RiesgoAcademicoReporteResponseDTO reporte = servicio.consultar(null, null, null, 2, admin);

        assertThat(reporte.estudiantes()).isEmpty();
        assertThat(reporte.mensaje()).isEqualTo("Sin datos disponibles");
        assertThat(reporte.totalEstudiantesEnRiesgo()).isZero();
    }

    @Test
    @DisplayName("Sin semestre activo: vacío con mensaje")
    void sinSemestreActivo() {
        when(evaluacionService.consultarSemestreActivo()).thenReturn(Optional.empty());

        RiesgoAcademicoReporteResponseDTO reporte = servicio.consultar(null, null, null, null, admin);

        assertThat(reporte.semestreId()).isNull();
        assertThat(reporte.estudiantes()).isEmpty();
        assertThat(reporte.mensaje()).isEqualTo("Sin datos disponibles");
    }

    @Test
    @DisplayName("Filtros inexistentes: 404 sin UUID en el mensaje")
    void filtrosInexistentes() {
        UUID otro = UUID.randomUUID();
        assertThatThrownBy(() -> servicio.consultar(otro, null, null, null, admin))
                .isInstanceOf(ReporteRecursoNoEncontradoException.class).hasMessageNotContaining(otro.toString());
        assertThatThrownBy(() -> servicio.consultar(null, otro, null, null, admin))
                .isInstanceOf(ReporteRecursoNoEncontradoException.class).hasMessageNotContaining(otro.toString());
        assertThatThrownBy(() -> servicio.consultar(null, null, otro, null, admin))
                .isInstanceOf(ReporteRecursoNoEncontradoException.class).hasMessageNotContaining(otro.toString());
    }

    @Test
    @DisplayName("Profesor con asignatura ajena: 403")
    void profesorAsignaturaAjena() {
        UsuarioPrincipal profesor = new UsuarioPrincipal(profesorId, RolUsuario.PROFESOR);
        when(asignaturaRepository.findAsignadasAProfesor(profesorId, EstadoRegistro.ACTIVO)).thenReturn(List.of());

        assertThatThrownBy(() -> servicio.consultar(null, null, asignaturaId, null, profesor))
                .isInstanceOf(PermisoReporteInsuficienteException.class);
    }

    @Test
    @DisplayName("Profesor solo ve riesgo de sus asignaturas")
    void profesorAlcance() {
        UsuarioPrincipal profesor = new UsuarioPrincipal(profesorId, RolUsuario.PROFESOR);
        when(asignaturaRepository.findAsignadasAProfesor(profesorId, EstadoRegistro.ACTIVO))
                .thenReturn(List.of(asignatura));
        simularFilas(List.of(fila("RA-01", NivelLogro.BAJO, 2.0)));

        RiesgoAcademicoReporteResponseDTO reporte = servicio.consultar(null, null, null, null, profesor);

        assertThat(reporte.totalEstudiantesEnRiesgo()).isEqualTo(1);
        assertThat(reporte.estudiantes().getFirst().severidad()).isEqualTo(SeveridadRiesgo.ATENCION);
    }

    @Test
    @DisplayName("Severidad: 1=ATENCION, 2=RIESGO")
    void severidadPorCantidad() {
        simularFilas(List.of(
                fila("RA-01", NivelLogro.BAJO, 2.0),
                fila("RA-02", NivelLogro.MEDIO, 3.1)));

        EstudianteRiesgoDTO fila = servicio.consultar(null, null, null, null, admin).estudiantes().getFirst();

        assertThat(fila.cantidadRaNoAlcanzados()).isEqualTo(2);
        assertThat(fila.severidad()).isEqualTo(SeveridadRiesgo.RIESGO);
    }
}
