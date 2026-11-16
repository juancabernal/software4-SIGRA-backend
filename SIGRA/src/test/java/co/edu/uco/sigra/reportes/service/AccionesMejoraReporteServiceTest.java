package co.edu.uco.sigra.reportes.service;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.mejoras.entity.AccionMejora;
import co.edu.uco.sigra.reportes.repository.AccionesMejoraReporteRepository;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.semestres.entity.Semestre;
import org.junit.jupiter.api.Test;

import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.reportes.exception.PermisoReporteInsuficienteException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AccionesMejoraReporteServiceTest {
    private final AccionesMejoraReporteRepository repository = mock(AccionesMejoraReporteRepository.class);
    private final AsignacionDocenteRepository asignaciones = mock(AsignacionDocenteRepository.class);
    private final UsuarioPrincipal admin = new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ADMINISTRADOR);
    private final AccionesMejoraReporteService service = new AccionesMejoraReporteService(repository, asignaciones);

    @Test
    void cuentaConversionesRedondeaYExponeElSemestreDeOrigen() {
        var ra = new ResultadoAprendizaje();
        ra.setId(UUID.randomUUID());
        ra.setCodigo("RA-01");
        ra.setAsignatura(new Asignatura("MAT-1", "Matemáticas", null));
        var semestre = mock(Semestre.class);
        var semestreId = UUID.randomUUID();
        when(semestre.getId()).thenReturn(semestreId);
        when(semestre.getCodigo()).thenReturn("2026-1");
        var conclusion = new Conclusion(ra, semestre, 60);
        var convertida = new AccionMejora("Refuerzo", "Práctica", ra, conclusion);
        var evaluacionId = UUID.randomUUID();
        convertida.marcarConvertida(evaluacionId);
        when(repository.consultar(null, null, null, null)).thenReturn(List.of(convertida,
                new AccionMejora("Taller", "Práctica", ra, conclusion),
                new AccionMejora("Tutoría", "Práctica", ra, conclusion)));

        var respuesta = service.consultar(null, null, null, null, admin);
        assertThat(respuesta.resumen().total()).isEqualTo(3);
        assertThat(respuesta.resumen().convertidas()).isEqualTo(1);
        assertThat(respuesta.resumen().sinConvertir()).isEqualTo(2);
        assertThat(respuesta.resumen().porcentajeConversion()).isEqualTo(33.33);
        assertThat(respuesta.filas().getFirst().semestreId()).isEqualTo(semestreId);
        assertThat(respuesta.filas().getFirst().evaluacionGeneradaId()).isEqualTo(evaluacionId);
    }

    @Test
    void conservaFiltroFalseYDevuelveCerosSinCoincidencias() {
        var semestre = UUID.randomUUID();
        var asignatura = UUID.randomUUID();
        var ra = UUID.randomUUID();
        when(repository.consultar(semestre, asignatura, ra, false)).thenReturn(List.of());
        var respuesta = service.consultar(semestre, asignatura, ra, false, admin);
        verify(repository).consultar(semestre, asignatura, ra, false);
        assertThat(respuesta.filas()).isEmpty();
        assertThat(respuesta.resumen().total()).isZero();
        assertThat(respuesta.resumen().porcentajeConversion()).isZero();
    }

    @Test
    void profesorSoloRecibeSusAccionesYSusConteosAunquePidaUnRaAjeno() {
        var profesor = new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.PROFESOR);
        var propia = mock(Asignatura.class);
        var ajena = mock(Asignatura.class);
        UUID propiaId = UUID.randomUUID(), ajenaId = UUID.randomUUID();
        when(propia.getId()).thenReturn(propiaId);
        when(ajena.getId()).thenReturn(ajenaId);
        var asignacion = mock(AsignacionDocente.class);
        when(asignacion.getAsignatura()).thenReturn(propia);
        when(asignaciones.findByProfesor_IdAndEstado(profesor.id(), EstadoRegistro.ACTIVO))
                .thenReturn(List.of(asignacion));
        var semestre = mock(Semestre.class);
        var raPropio = new ResultadoAprendizaje();
        raPropio.setId(UUID.randomUUID()); raPropio.setAsignatura(propia); raPropio.setCodigo("RA-01");
        var raAjeno = new ResultadoAprendizaje();
        raAjeno.setId(UUID.randomUUID()); raAjeno.setAsignatura(ajena); raAjeno.setCodigo("RA-02");
        var accionPropia = new AccionMejora("Propia", "Refuerzo", raPropio, new Conclusion(raPropio, semestre, 60));
        var accionAjena = new AccionMejora("Ajena", "Refuerzo", raAjeno, new Conclusion(raAjeno, semestre, 60));
        accionAjena.marcarConvertida(UUID.randomUUID());
        when(repository.consultar(null, null, null, null)).thenReturn(List.of(accionPropia, accionAjena));
        var respuesta = service.consultar(null, null, null, null, profesor);
        assertThat(respuesta.filas()).extracting(f -> f.nombre()).containsExactly("Propia");
        assertThat(respuesta.resumen().total()).isEqualTo(1);
        assertThat(respuesta.resumen().convertidas()).isZero();
        assertThatThrownBy(() -> service.consultar(null, ajenaId, null, null, profesor))
                .isInstanceOf(PermisoReporteInsuficienteException.class);
        when(repository.consultar(null, null, raAjeno.getId(), null)).thenReturn(List.of(accionAjena));
        assertThat(service.consultar(null, null, raAjeno.getId(), null, profesor).filas()).isEmpty();
    }

    @Test
    void profesorSinAsignacionesRecibeCerosYEstudianteEsRechazado() {
        var profesor = new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.PROFESOR);
        when(asignaciones.findByProfesor_IdAndEstado(profesor.id(), EstadoRegistro.ACTIVO)).thenReturn(List.of());
        assertThat(service.consultar(null, null, null, null, profesor).resumen().total()).isZero();
        verifyNoInteractions(repository);
        assertThatThrownBy(() -> service.consultar(null, null, null, null,
                new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ESTUDIANTE)))
                .isInstanceOf(PermisoReporteInsuficienteException.class);
        assertThatThrownBy(() -> service.consultar(null, null, null, null, null))
                .isInstanceOf(PermisoReporteInsuficienteException.class);
    }
}
