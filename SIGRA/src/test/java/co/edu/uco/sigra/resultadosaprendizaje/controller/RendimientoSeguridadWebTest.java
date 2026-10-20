package co.edu.uco.sigra.resultadosaprendizaje.controller;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.SecurityAuthorizationService;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.config.SecurityConfig;
import co.edu.uco.sigra.resultadosaprendizaje.dto.RendimientoAsignaturaResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.service.RendimientoResultadoAprendizajeService;
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

/**
 * Seguridad del endpoint de rendimiento de RA con la cadena de Spring Security real y el servicio simulado:
 * ADMINISTRADOR, o PROFESOR con asignación docente activa; anónimo 401; ESTUDIANTE 403.
 */
@WebMvcTest(controllers = RendimientoResultadoAprendizajeController.class)
@Import({SecurityConfig.class, CurrentUser.class})
@DisplayName("Seguridad del rendimiento de resultados de aprendizaje (cadena real)")
class RendimientoSeguridadWebTest {

    private static final UUID ID_ASIGNATURA = UUID.fromString("00000000-0000-4000-f100-000000000001");
    private static final String RUTA = "/api/v1/asignaturas/" + ID_ASIGNATURA + "/resultados-aprendizaje/rendimiento";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RendimientoResultadoAprendizajeService rendimientoService;

    @MockitoBean(name = "securityAuthorization")
    private SecurityAuthorizationService securityAuthorization;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Sin token devuelve 401 y no ejecuta el servicio")
    void anonimoEs401() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        verifyNoInteractions(rendimientoService);
    }

    @Test
    @DisplayName("El estudiante recibe 403 y no ejecuta el servicio")
    void estudianteEs403() throws Exception {
        configurarToken("token-estudiante", RolUsuario.ESTUDIANTE);

        mockMvc.perform(get(RUTA).header("Authorization", "Bearer token-estudiante"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        verifyNoInteractions(rendimientoService);
    }

    @Test
    @DisplayName("El administrador consulta el rendimiento")
    void administradorAccede() throws Exception {
        configurarToken("token-admin", RolUsuario.ADMINISTRADOR);
        respuestaVacia();

        mockMvc.perform(get(RUTA).header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asignaturaId").value(ID_ASIGNATURA.toString()));
    }

    @Test
    @DisplayName("El profesor con asignación docente activa consulta el rendimiento")
    void profesorAsignadoAccede() throws Exception {
        configurarToken("token-profesor", RolUsuario.PROFESOR);
        when(securityAuthorization.hasActiveAssignment(eq(ID_ASIGNATURA), any())).thenReturn(true);
        respuestaVacia();

        mockMvc.perform(get(RUTA).header("Authorization", "Bearer token-profesor"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("El profesor sin asignación docente activa recibe 403 y no ejecuta el servicio")
    void profesorSinAsignacionEs403() throws Exception {
        configurarToken("token-profesor", RolUsuario.PROFESOR);
        when(securityAuthorization.hasActiveAssignment(any(), any())).thenReturn(false);

        mockMvc.perform(get(RUTA).header("Authorization", "Bearer token-profesor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        verifyNoInteractions(rendimientoService);
    }

    private void respuestaVacia() {
        when(rendimientoService.consultarPorAsignatura(ID_ASIGNATURA))
                .thenReturn(new RendimientoAsignaturaResponseDTO(ID_ASIGNATURA, null, null, List.of()));
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
