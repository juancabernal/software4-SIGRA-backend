package co.edu.uco.sigra.reportes.controller;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.config.SecurityConfig;
import co.edu.uco.sigra.reportes.dto.RiesgoAcademicoReporteResponseDTO;
import co.edu.uco.sigra.reportes.exception.PermisoReporteInsuficienteException;
import co.edu.uco.sigra.reportes.service.RiesgoAcademicoReporteService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReporteRiesgoAcademicoController.class)
@Import({SecurityConfig.class, CurrentUser.class})
@DisplayName("Seguridad del reporte R6 de riesgo académico (cadena real)")
class ReporteRiesgoAcademicoSeguridadWebTest {

    private static final String RUTA = "/api/v1/reportes/riesgo-academico";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RiesgoAcademicoReporteService reporteService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Sin token devuelve 401 y no ejecuta el servicio")
    void anonimoEs401() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        verifyNoInteractions(reporteService);
    }

    @Test
    @DisplayName("El estudiante recibe 403 y no ejecuta el servicio")
    void estudianteEs403() throws Exception {
        configurarToken("token-estudiante", RolUsuario.ESTUDIANTE);

        mockMvc.perform(get(RUTA).header("Authorization", "Bearer token-estudiante"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        verifyNoInteractions(reporteService);
    }

    @Test
    @DisplayName("Administrador y profesor consultan el reporte")
    void administradorYProfesorAcceden() throws Exception {
        when(reporteService.consultar(any(), any(), any(), any(), any()))
                .thenReturn(new RiesgoAcademicoReporteResponseDTO(null, null, 0, List.of(), "Sin datos disponibles"));
        configurarToken("token-admin", RolUsuario.ADMINISTRADOR);
        configurarToken("token-profesor", RolUsuario.PROFESOR);

        mockMvc.perform(get(RUTA).header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Sin datos disponibles"));
        mockMvc.perform(get(RUTA).header("Authorization", "Bearer token-profesor"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("El profesor que pide una asignatura ajena recibe 403 desde el servicio")
    void profesorAsignaturaAjenaEs403() throws Exception {
        UUID asignaturaId = UUID.randomUUID();
        configurarToken("token-profesor", RolUsuario.PROFESOR);
        when(reporteService.consultar(any(), any(), eq(asignaturaId), any(), any()))
                .thenThrow(new PermisoReporteInsuficienteException());

        mockMvc.perform(get(RUTA).param("asignaturaId", asignaturaId.toString())
                        .header("Authorization", "Bearer token-profesor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Un identificador que no es UUID devuelve 400")
    void uuidInvalidoEs400() throws Exception {
        configurarToken("token-admin", RolUsuario.ADMINISTRADOR);

        mockMvc.perform(get(RUTA).param("programaId", "no-es-uuid")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reporteService);
    }

    private void configurarToken(String token, RolUsuario rol) {
        UUID id = UUID.randomUUID();
        String correo = rol.name().toLowerCase() + "@uco.net.co";
        Claims claims = mock(Claims.class);
        when(jwtService.extraerClaims(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(id.toString());
        when(claims.get("rol", String.class)).thenReturn(rol.name());
        when(claims.get("correo", String.class)).thenReturn(correo);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(new UsuarioStub(id, correo, rol)));
    }

    private static final class UsuarioStub extends Usuario {
        private final RolUsuario rol;

        private UsuarioStub(UUID id, String correo, RolUsuario rol) {
            this.rol = rol;
            setId(id);
            setCorreoInstitucional(correo);
            setEstado(EstadoRegistro.ACTIVO);
        }

        @Override
        public RolUsuario getRol() {
            return rol;
        }
    }
}
