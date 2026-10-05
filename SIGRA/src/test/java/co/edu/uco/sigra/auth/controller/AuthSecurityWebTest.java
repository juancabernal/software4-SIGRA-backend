package co.edu.uco.sigra.auth.controller;

import co.edu.uco.sigra.auth.dto.LoginRequestDTO;
import co.edu.uco.sigra.auth.dto.LoginResponseDTO;
import co.edu.uco.sigra.auth.dto.UsuarioSesionDTO;
import co.edu.uco.sigra.auth.exception.CredencialesInvalidasException;
import co.edu.uco.sigra.auth.exception.UsuarioBloqueadoException;
import co.edu.uco.sigra.auth.service.AuthService;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// INTEGRACIÓN LIGERA (sin PostgreSQL): carga la cadena de seguridad real (SecurityConfig) y el controlador,
// con AuthService simulado. Comprueba que el login es público, no exige CSRF y devuelve los códigos esperados.
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthSecurityWebTest {

    private static final String URL_LOGIN = "/api/v1/auth/login";

    private static final String JSON_VALIDO = """
            {
                "correoInstitucional": "profesor.bruno@uco.net.co",
                "contrasena": "PasswordSeguro123*"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginValidoDevuelve200SinCsrfNiSesion() throws Exception {
        UsuarioSesionDTO sesion = new UsuarioSesionDTO(
                UUID.randomUUID(), "Profesor Bruno", "profesor.bruno@uco.net.co", RolUsuario.PROFESOR);
        when(authService.autenticar(any(LoginRequestDTO.class)))
                .thenReturn(new LoginResponseDTO("token.jwt", "Bearer", 3600L, sesion));

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token.jwt"))
                .andExpect(jsonPath("$.usuario.rol").value("PROFESOR"));
    }

    @Test
    void credencialesInvalidasDevuelve401() throws Exception {
        when(authService.autenticar(any(LoginRequestDTO.class)))
                .thenThrow(new CredencialesInvalidasException());

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void usuarioBloqueadoDevuelve423() throws Exception {
        when(authService.autenticar(any(LoginRequestDTO.class)))
                .thenThrow(new UsuarioBloqueadoException());

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VALIDO))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.error").value("LOCKED"));
    }

    @Test
    void validacionFallidaDevuelve400() throws Exception {
        String jsonInvalido = """
                {
                    "correoInstitucional": "",
                    "contrasena": ""
                }
                """;

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }
}
