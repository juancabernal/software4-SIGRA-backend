package co.edu.uco.sigra.reportes.controller;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.config.SecurityConfig;
import co.edu.uco.sigra.reportes.dto.*;
import co.edu.uco.sigra.reportes.service.MatriculasProgramaSemestreReporteService;
import co.edu.uco.sigra.reportes.exception.ReporteRecursoNoEncontradoException;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReporteMatriculasProgramaSemestreController.class)
@Import({SecurityConfig.class, CurrentUser.class})
class ReporteMatriculasProgramaSemestreControllerTest {

    private static final String RUTA = "/api/v1/reportes/matriculas-programa-semestre";
    private static final UUID SEMESTRE = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID FACULTAD = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;
    @MockitoBean private MatriculasProgramaSemestreReporteService service;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UsuarioRepository usuarioRepository;

    @Test
    void sinTokenEs401() throws Exception {
        mockMvc.perform(get(RUTA)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @EnumSource(value = RolUsuario.class, names = {"PROFESOR", "ESTUDIANTE"})
    void profesorYEstudianteSon403(RolUsuario rol) throws Exception {
        String token = rol.name().toLowerCase() + "-token";
        configurarToken(token, rol);

        mockMvc.perform(get(RUTA).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void administradorRecibeTodosLosCamposDelContratoYFiltros() throws Exception {
        configurarToken("admin-token", RolUsuario.ADMINISTRADOR);
        var fila = new MatriculasProgramaSemestreReporteFilaDTO(SEMESTRE, "2026-1",
                LocalDate.of(2026, 1, 15), EstadoSemestre.ACTIVO, PROGRAMA, "P-1",
                "Programa Uno", "Facultad Uno", 7, 4, 3, 5, 2);
        var response = new MatriculasProgramaSemestreReporteResponseDTO(SEMESTRE, PROGRAMA, FACULTAD,
                new MatriculasProgramaSemestreReporteResumenDTO(7, 4, 3, 5, 1, 1), List.of(fila));
        when(service.consultar(SEMESTRE, PROGRAMA, FACULTAD)).thenReturn(response);

        mockMvc.perform(get(RUTA).param("semestreId", SEMESTRE.toString())
                        .param("programaId", PROGRAMA.toString())
                        .param("facultadId", FACULTAD.toString())
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semestreId").value(SEMESTRE.toString()))
                .andExpect(jsonPath("$.programaId").value(PROGRAMA.toString()))
                .andExpect(jsonPath("$.facultadId").value(FACULTAD.toString()))
                .andExpect(jsonPath("$.resumen.totalMatriculas").value(7))
                .andExpect(jsonPath("$.resumen.activas").value(4))
                .andExpect(jsonPath("$.resumen.desvinculadas").value(3))
                .andExpect(jsonPath("$.resumen.estudiantesDistintos").value(5))
                .andExpect(jsonPath("$.resumen.programas").value(1))
                .andExpect(jsonPath("$.resumen.semestres").value(1))
                .andExpect(jsonPath("$.filas[0].semestreId").value(SEMESTRE.toString()))
                .andExpect(jsonPath("$.filas[0].semestreCodigo").value("2026-1"))
                .andExpect(jsonPath("$.filas[0].semestreFechaInicio").value("2026-01-15"))
                .andExpect(jsonPath("$.filas[0].semestreEstado").value("ACTIVO"))
                .andExpect(jsonPath("$.filas[0].programaId").value(PROGRAMA.toString()))
                .andExpect(jsonPath("$.filas[0].programaCodigo").value("P-1"))
                .andExpect(jsonPath("$.filas[0].programaNombre").value("Programa Uno"))
                .andExpect(jsonPath("$.filas[0].facultadNombre").value("Facultad Uno"))
                .andExpect(jsonPath("$.filas[0].totalMatriculas").value(7))
                .andExpect(jsonPath("$.filas[0].activas").value(4))
                .andExpect(jsonPath("$.filas[0].desvinculadas").value(3))
                .andExpect(jsonPath("$.filas[0].estudiantes").value(5))
                .andExpect(jsonPath("$.filas[0].asignaturas").value(2));
        verify(service).consultar(SEMESTRE, PROGRAMA, FACULTAD);
    }

    @ParameterizedTest
    @ValueSource(strings = {"semestreId", "programaId", "facultadId"})
    void uuidInvalidoEs400(String parametro) throws Exception {
        configurarToken("admin-token", RolUsuario.ADMINISTRADOR);

        mockMvc.perform(get(RUTA).param(parametro, "no-es-uuid")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void recursoInexistenteEs404() throws Exception {
        configurarToken("admin-token", RolUsuario.ADMINISTRADOR);
        when(service.consultar(SEMESTRE, PROGRAMA, FACULTAD))
                .thenThrow(new ReporteRecursoNoEncontradoException("El programa académico indicado no existe."));

        mockMvc.perform(get(RUTA).param("semestreId", SEMESTRE.toString())
                        .param("programaId", PROGRAMA.toString())
                        .param("facultadId", FACULTAD.toString())
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void filtrosAusentesLleganComoNull() throws Exception {
        configurarToken("admin-token", RolUsuario.ADMINISTRADOR);
        when(service.consultar(null, null, null)).thenReturn(
                new MatriculasProgramaSemestreReporteResponseDTO(null, null, null,
                        new MatriculasProgramaSemestreReporteResumenDTO(0, 0, 0, 0, 0, 0), List.of()));

        mockMvc.perform(get(RUTA).header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
        verify(service).consultar(null, null, null);
    }

    private void configurarToken(String token, RolUsuario rol) {
        UUID id = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(jwtService.extraerClaims(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(id.toString());
        when(claims.get("rol", String.class)).thenReturn(rol.name());
        when(claims.get("correo", String.class)).thenReturn(rol.name().toLowerCase() + "@uco.net.co");
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(new UsuarioStub(id, rol)));
    }

    private static final class UsuarioStub extends Usuario {
        private final RolUsuario rol;
        private UsuarioStub(UUID id, RolUsuario rol) {
            this.rol = rol;
            setId(id);
            setCorreoInstitucional(rol.name().toLowerCase() + "@uco.net.co");
            setEstado(EstadoRegistro.ACTIVO);
        }
        @Override public RolUsuario getRol() { return rol; }
    }
}
