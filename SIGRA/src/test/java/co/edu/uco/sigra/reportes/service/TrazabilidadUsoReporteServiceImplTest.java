package co.edu.uco.sigra.reportes.service;

import co.edu.uco.sigra.auditoria.entity.AccionAuditoria;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import co.edu.uco.sigra.reportes.dto.ActividadRolDTO;
import co.edu.uco.sigra.reportes.dto.ConteoAccionDTO;
import co.edu.uco.sigra.reportes.exception.PermisoReporteInsuficienteException;
import co.edu.uco.sigra.reportes.exception.RangoReporteInvalidoException;
import co.edu.uco.sigra.reportes.exception.ReporteRecursoNoEncontradoException;
import co.edu.uco.sigra.reportes.repository.EstudiantesProfesorReporteRepository;
import co.edu.uco.sigra.reportes.repository.EstudiantesProfesorReporteRepository.EstudianteAsignatura;
import co.edu.uco.sigra.reportes.repository.TrazabilidadUsoReporteRepository;
import co.edu.uco.sigra.reportes.repository.TrazabilidadUsoReporteRepository.ActividadUsuario;
import co.edu.uco.sigra.reportes.repository.TrazabilidadUsoReporteRepository.ConteoAccion;
import co.edu.uco.sigra.reportes.repository.TrazabilidadUsoReporteRepository.ConteoRolAccion;
import co.edu.uco.sigra.reportes.repository.TrazabilidadUsoReporteRepository.ResumenRol;
import co.edu.uco.sigra.reportes.service.impl.TrazabilidadUsoReporteServiceImpl;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("R7 – Trazabilidad de uso por rol (servicio)")
class TrazabilidadUsoReporteServiceImplTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 15);

    private final TrazabilidadUsoReporteRepository repository = mock();
    private final EstudiantesProfesorReporteRepository estudiantesRepository = mock();
    private final SemestreRepository semestreRepository = mock();
    private final EvaluacionService evaluacionService = mock();
    private final Clock reloj = Clock.fixed(HOY.atStartOfDay(ZONA).plusHours(12).toInstant(), ZONA);
    private final TrazabilidadUsoReporteService service = new TrazabilidadUsoReporteServiceImpl(
            repository, estudiantesRepository, semestreRepository, evaluacionService, reloj);

    @Test
    @DisplayName("Agrupa por rol, incluye roles sin actividad y separa los accesos sin identificar")
    void agrupaPorRol() {
        LocalDateTime ultima = LocalDateTime.of(2026, 11, 14, 10, 0);
        when(repository.contarPorRolYAccion(any(), any())).thenReturn(List.of(
                conteo(RolUsuario.PROFESOR, AccionAuditoria.LOGIN_EXITOSO, 5),
                conteo(RolUsuario.PROFESOR, AccionAuditoria.CALIFICACION_MODIFICADA, 2),
                conteo(RolUsuario.ESTUDIANTE, AccionAuditoria.LOGIN_EXITOSO, 9),
                conteo(null, AccionAuditoria.LOGIN_FALLIDO, 3)));
        when(repository.resumirPorRol(any(), any())).thenReturn(List.of(
                resumen(RolUsuario.PROFESOR, 2, ultima),
                resumen(RolUsuario.ESTUDIANTE, 4, ultima),
                resumen(null, 0, ultima)));

        var reporte = service.consultarGeneral(null, null, null, null);

        assertThat(reporte.desde()).isEqualTo(HOY.minusDays(29));
        assertThat(reporte.hasta()).isEqualTo(HOY);
        assertThat(reporte.porRol()).extracting(ActividadRolDTO::rol)
                .containsExactly("ADMINISTRADOR", "PROFESOR", "ESTUDIANTE", "SIN_IDENTIFICAR");
        ActividadRolDTO admin = reporte.porRol().get(0);
        assertThat(admin.totalEventos()).isZero();
        assertThat(admin.acciones()).isEmpty();
        ActividadRolDTO profesor = reporte.porRol().get(1);
        assertThat(profesor.totalEventos()).isEqualTo(7);
        assertThat(profesor.usuariosDistintos()).isEqualTo(2);
        assertThat(profesor.ultimaActividad()).isEqualTo(ultima);
        assertThat(profesor.acciones()).extracting(ConteoAccionDTO::accion)
                .containsExactly(AccionAuditoria.LOGIN_EXITOSO, AccionAuditoria.CALIFICACION_MODIFICADA);
        assertThat(reporte.resumen().totalEventos()).isEqualTo(19);
        assertThat(reporte.resumen().usuariosActivos()).isEqualTo(6);
        assertThat(reporte.resumen().loginsExitosos()).isEqualTo(14);
        assertThat(reporte.resumen().loginsFallidos()).isEqualTo(3);
    }

    @Test
    @DisplayName("El filtro de rol deja solo ese tipo de usuario en el reporte")
    void filtraPorRol() {
        when(repository.contarPorRolYAccion(any(), any())).thenReturn(List.of(
                conteo(RolUsuario.ESTUDIANTE, AccionAuditoria.LOGIN_EXITOSO, 9),
                conteo(null, AccionAuditoria.LOGIN_FALLIDO, 3)));
        when(repository.resumirPorRol(any(), any())).thenReturn(List.of());

        var reporte = service.consultarGeneral(null, null, null, RolUsuario.ESTUDIANTE);

        assertThat(reporte.rol()).isEqualTo(RolUsuario.ESTUDIANTE);
        assertThat(reporte.porRol()).extracting(ActividadRolDTO::rol).containsExactly("ESTUDIANTE");
        assertThat(reporte.resumen().totalEventos()).isEqualTo(9);
        assertThat(reporte.resumen().loginsFallidos()).isZero();
    }

    @Test
    @DisplayName("Con semestre, el rango se acota a sus fechas")
    void semestreAcotaElRango() {
        Semestre semestre = semestre(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 15));
        when(semestreRepository.findById(semestre.getId())).thenReturn(Optional.of(semestre));
        when(repository.contarPorRolYAccion(any(), any())).thenReturn(List.of());
        when(repository.resumirPorRol(any(), any())).thenReturn(List.of());

        var completo = service.consultarGeneral(null, null, semestre.getId(), null);
        var acotado = service.consultarGeneral(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31),
                semestre.getId(), null);

        assertThat(completo.desde()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(completo.hasta()).isEqualTo(LocalDate.of(2026, 12, 15));
        assertThat(completo.semestreCodigo()).isEqualTo("2026-2");
        assertThat(acotado.desde()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(acotado.hasta()).isEqualTo(LocalDate.of(2026, 8, 31));
        verify(repository).contarPorRolYAccion(LocalDate.of(2026, 7, 1).atStartOfDay(),
                LocalDate.of(2026, 8, 31).atTime(23, 59, 59, 999_999_000));
    }

    @Test
    @DisplayName("Rechaza rangos invertidos, demasiado amplios o fuera del semestre")
    void rangosInvalidos() {
        Semestre semestre = semestre(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 15));
        when(semestreRepository.findById(semestre.getId())).thenReturn(Optional.of(semestre));

        assertThatThrownBy(() -> service.consultarGeneral(HOY, HOY.minusDays(1), null, null))
                .isInstanceOf(RangoReporteInvalidoException.class);
        assertThatThrownBy(() -> service.consultarGeneral(HOY.minusDays(400), HOY, null, null))
                .isInstanceOf(RangoReporteInvalidoException.class)
                .hasMessageContaining("366");
        assertThatThrownBy(() -> service.consultarGeneral(LocalDate.of(2027, 1, 1), null, semestre.getId(), null))
                .isInstanceOf(RangoReporteInvalidoException.class)
                .hasMessageContaining("fuera del semestre");
        verify(repository, never()).contarPorRolYAccion(any(), any());
    }

    @Test
    @DisplayName("Un semestre inexistente es 404")
    void semestreInexistente() {
        UUID id = UUID.randomUUID();
        when(semestreRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consultarGeneral(null, null, id, null))
                .isInstanceOf(ReporteRecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("El profesor ve su actividad y la de cada estudiante, agrupando sus asignaturas")
    void profesorVeSuActividadYLaDeSusEstudiantes() {
        UUID profesorId = UUID.randomUUID();
        UUID ana = UUID.randomUUID();
        UUID luis = UUID.randomUUID();
        LocalDateTime acceso = LocalDateTime.of(2026, 11, 10, 8, 30);
        Semestre semestre = semestre(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 15));
        when(evaluacionService.consultarSemestreActivo()).thenReturn(Optional.of(semestre));
        when(repository.contarPropiasPorAccion(eq(profesorId), any(), any())).thenReturn(List.of(
                conteoAccion(AccionAuditoria.LOGIN_EXITOSO, 4, acceso),
                conteoAccion(AccionAuditoria.CALIFICACION_MODIFICADA, 1, acceso.plusDays(1))));
        when(estudiantesRepository.listarEstudiantesDelProfesor(profesorId, semestre.getId(),
                EstadoMatricula.ACTIVA, EstadoRegistro.ACTIVO)).thenReturn(List.of(
                estudiante(ana, "Ana", "Gómez", "MAT-1"),
                estudiante(ana, "Ana", "Gómez", "FIS-1"),
                estudiante(luis, "Luis", "Pérez", "MAT-1")));
        when(repository.resumirPorUsuarios(eq(Set.of(ana, luis)), eq(AccionAuditoria.LOGIN_EXITOSO), any(), any()))
                .thenReturn(List.of(actividad(ana, 3, 3, acceso)));

        var reporte = service.consultarProfesor(null, null, null,
                new UsuarioPrincipal(profesorId, "profe@uco.net.co", RolUsuario.PROFESOR));

        assertThat(reporte.semestreId()).isEqualTo(semestre.getId());
        assertThat(reporte.propia().totalEventos()).isEqualTo(5);
        assertThat(reporte.propia().ultimaActividad()).isEqualTo(acceso.plusDays(1));
        assertThat(reporte.estudiantes()).hasSize(2);
        var primera = reporte.estudiantes().get(0);
        assertThat(primera.nombreCompleto()).isEqualTo("Ana Gómez Ruiz");
        assertThat(primera.asignaturas()).containsExactly("MAT-1", "FIS-1");
        assertThat(primera.ingresos()).isEqualTo(3);
        assertThat(primera.ultimaActividad()).isEqualTo(acceso);
        var segunda = reporte.estudiantes().get(1);
        assertThat(segunda.totalEventos()).isZero();
        assertThat(segunda.ultimaActividad()).isNull();
        assertThat(reporte.resumenEstudiantes().total()).isEqualTo(2);
        assertThat(reporte.resumenEstudiantes().conActividad()).isEqualTo(1);
        assertThat(reporte.resumenEstudiantes().sinActividad()).isEqualTo(1);
        assertThat(reporte.resumenEstudiantes().ingresos()).isEqualTo(3);
        assertThat(reporte.mensaje()).isNull();
    }

    @Test
    @DisplayName("Sin semestre activo, el profesor ve su actividad y un mensaje, sin estudiantes")
    void profesorSinSemestreActivo() {
        UUID profesorId = UUID.randomUUID();
        when(evaluacionService.consultarSemestreActivo()).thenReturn(Optional.empty());
        when(repository.contarPropiasPorAccion(eq(profesorId), any(), any())).thenReturn(List.of());

        var reporte = service.consultarProfesor(null, null, null,
                new UsuarioPrincipal(profesorId, "profe@uco.net.co", RolUsuario.PROFESOR));

        assertThat(reporte.estudiantes()).isEmpty();
        assertThat(reporte.mensaje()).contains("semestre activo");
        assertThat(reporte.hasta()).isEqualTo(HOY);
        verify(repository, never()).resumirPorUsuarios(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Solo el profesor puede consultar la vista del profesor")
    void vistaProfesorExigeRol() {
        assertThatThrownBy(() -> service.consultarProfesor(null, null, null,
                new UsuarioPrincipal(UUID.randomUUID(), "admin@uco.net.co", RolUsuario.ADMINISTRADOR)))
                .isInstanceOf(PermisoReporteInsuficienteException.class);
    }

    private static Semestre semestre(LocalDate inicio, LocalDate fin) {
        Semestre semestre = new Semestre();
        semestre.setId(UUID.randomUUID());
        semestre.setCodigo("2026-2");
        semestre.setFechaInicio(inicio);
        semestre.setFechaFin(fin);
        return semestre;
    }

    private static ConteoRolAccion conteo(RolUsuario rol, AccionAuditoria accion, long total) {
        return new ConteoRolAccion() {
            public RolUsuario getRol() { return rol; }
            public AccionAuditoria getAccion() { return accion; }
            public Long getTotal() { return total; }
        };
    }

    private static ResumenRol resumen(RolUsuario rol, long usuarios, LocalDateTime ultima) {
        return new ResumenRol() {
            public RolUsuario getRol() { return rol; }
            public Long getUsuarios() { return usuarios; }
            public LocalDateTime getUltimaActividad() { return ultima; }
        };
    }

    private static ConteoAccion conteoAccion(AccionAuditoria accion, long total, LocalDateTime ultima) {
        return new ConteoAccion() {
            public AccionAuditoria getAccion() { return accion; }
            public Long getTotal() { return total; }
            public LocalDateTime getUltimaActividad() { return ultima; }
        };
    }

    private static ActividadUsuario actividad(UUID id, long total, long ingresos, LocalDateTime ultima) {
        return new ActividadUsuario() {
            public UUID getUsuarioId() { return id; }
            public Long getTotal() { return total; }
            public Long getIngresos() { return ingresos; }
            public LocalDateTime getUltimaActividad() { return ultima; }
        };
    }

    private static EstudianteAsignatura estudiante(UUID id, String nombre, String apellido, String asignatura) {
        return new EstudianteAsignatura() {
            public UUID getEstudianteId() { return id; }
            public String getPrimerNombre() { return nombre; }
            public String getSegundoNombre() { return null; }
            public String getPrimerApellido() { return apellido; }
            public String getSegundoApellido() { return "Ruiz"; }
            public String getCorreoInstitucional() { return nombre.toLowerCase() + "@uco.net.co"; }
            public String getAsignaturaCodigo() { return asignatura; }
        };
    }
}
