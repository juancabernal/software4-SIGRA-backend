package co.edu.uco.sigra.auth.controller;

import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.config.SecurityConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MockMvc con la cadena real: ausencia, rol incorrecto y rol permitido. */
@WebMvcTest(controllers = co.edu.uco.sigra.asignaturas.controller.AsignaturaController.class)
@Import({SecurityConfig.class, CurrentUser.class})
class SecurityFilterMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AsignaturaService asignaturaService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void rutaPrivadaSinTokenEs401() throws Exception {
        mockMvc.perform(get("/api/v1/asignaturas"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        verifyNoInteractions(asignaturaService);
    }

    @Test
    void rolEstudianteNoPermitidoEs403() throws Exception {
        configurarToken("estudiante-token", RolUsuario.ESTUDIANTE, "estudiante@uco.net.co");

        mockMvc.perform(get("/api/v1/asignaturas")
                        .header("Origin", "http://localhost:4200")
                        .header("Authorization", "Bearer estudiante-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.matchesPattern(
                        "[0-9a-f-]{36}")))
                .andExpect(header().string("Access-Control-Expose-Headers",
                        org.hamcrest.Matchers.containsString("X-Request-Id")));
        verifyNoInteractions(asignaturaService);
    }

    @Test
    void rolAdministradorPermitidoEjecutaLaOperacion() throws Exception {
        configurarToken("admin-token", RolUsuario.ADMINISTRADOR, "admin@uco.net.co");
        when(asignaturaService.consultar(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/asignaturas").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        verify(asignaturaService).consultar(any(), any());
    }

    @Test
    void error401DesdeOrigenPermitidoExponeLaCorrelacionAlFrontend() throws Exception {
        mockMvc.perform(get("/api/v1/programas")
                        .header("Origin", "http://localhost:4200"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.matchesPattern(
                        "[0-9a-f-]{36}")))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Expose-Headers",
                        org.hamcrest.Matchers.containsString("X-Request-Id")));
    }

    @Test
    void cuerpoDeLoginDe16385BytesResponde413ConCORSYCorrelacion() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .contentType("application/json")
                        .content("a".repeat(16_385)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error").value("PAYLOAD_TOO_LARGE"))
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.matchesPattern(
                        "[0-9a-f-]{36}")))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Expose-Headers",
                        org.hamcrest.Matchers.containsString("X-Request-Id")));
    }

    @Test
    void falloInesperadoDelControladorResponde500GenericoConCorrelacion() throws Exception {
        configurarToken("token-fallo-interno", RolUsuario.ADMINISTRADOR, "admin@uco.net.co");
        when(asignaturaService.consultar(any(), any()))
                .thenThrow(new IllegalStateException("detalle técnico que no debe salir"));

        mockMvc.perform(get("/api/v1/asignaturas")
                        .header("Origin", "http://localhost:4200")
                        .header("Authorization", "Bearer token-fallo-interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.matchesPattern(
                        "[0-9a-f-]{36}")))
                .andExpect(header().string("Access-Control-Expose-Headers",
                        org.hamcrest.Matchers.containsString("X-Request-Id")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("detalle técnico"))));
    }

    @Test
    void caidaDelRepositorioResponde503JsonSinEjecutarControlador() throws Exception {
        UUID id = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(jwtService.extraerClaims("token-db-fallo")).thenReturn(claims);
        when(claims.getSubject()).thenReturn(id.toString());
        when(claims.get("rol", String.class)).thenReturn(RolUsuario.ADMINISTRADOR.name());
        when(claims.get("correo", String.class)).thenReturn("admin@uco.net.co");
        when(usuarioRepository.findById(id))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        mockMvc.perform(get("/api/v1/asignaturas")
                        .header("Authorization", "Bearer token-db-fallo")
                        .header("Origin", "http://localhost:4200"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.matchesPattern(
                        "[0-9a-f-]{36}")))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Expose-Headers",
                        org.hamcrest.Matchers.containsString("X-Request-Id")))
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("database unavailable"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("co.edu"))));
        verifyNoInteractions(asignaturaService);
    }

    @Test
    void preflightPermitidoNoAbreLaOperacion() throws Exception {
        mockMvc.perform(options("/api/v1/programas")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void origenNoPermitidoNoRecibeCORS() throws Exception {
        mockMvc.perform(options("/api/v1/programas")
                        .header("Origin", "https://origen-no-permitido.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.matchesPattern(
                        "[0-9a-f-]{36}")))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    private void configurarToken(String token, RolUsuario rol, String correo) {
        UUID id = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(jwtService.extraerClaims(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(id.toString());
        when(claims.get("rol", String.class)).thenReturn(rol.name());
        when(claims.get("correo", String.class)).thenReturn(correo);
        Usuario usuario = new UsuarioStub(id, correo, rol);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
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
