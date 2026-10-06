package co.edu.uco.sigra.auth.controller;

import co.edu.uco.sigra.auth.dto.LoginRequestDTO;
import co.edu.uco.sigra.auth.dto.LoginResponseDTO;
import co.edu.uco.sigra.auth.dto.UsuarioSesionDTO;
import co.edu.uco.sigra.auth.exception.AuthExceptionHandler;
import co.edu.uco.sigra.auth.exception.CredencialesInvalidasException;
import co.edu.uco.sigra.auth.exception.UsuarioBloqueadoException;
import co.edu.uco.sigra.auth.service.AuthService;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// UNIT de controlador: MockMvc standalone, sin SecurityFilterChain (eso lo cubre AuthSecurityWebTest).
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String URL_LOGIN = "/api/v1/auth/login";

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new AuthExceptionHandler(), new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void loginExitosoDevuelve200ConEstructuraEsperada() throws Exception {
        UsuarioSesionDTO sesionDTO = new UsuarioSesionDTO(
                UUID.randomUUID(),
                "Profesor Prueba",
                "profesor@uco.net.co",
                RolUsuario.PROFESOR
        );
        LoginResponseDTO responseDTO = new LoginResponseDTO("token.jwt.valido", "Bearer", 3600L, sesionDTO);

        when(authService.autenticar(any(LoginRequestDTO.class))).thenReturn(responseDTO);

        String jsonPeticion = """
                {
                    "correoInstitucional": "profesor@uco.net.co",
                    "contrasena": "Password123*"
                }
                """;

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPeticion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token.jwt.valido"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEn").value(3600))
                .andExpect(jsonPath("$.usuario.correoInstitucional").value("profesor@uco.net.co"))
                .andExpect(jsonPath("$.usuario.rol").value("PROFESOR"))
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROFESOR@UCO.NET.CO", "  profesor@uco.net.co  "})
    void correoEnMayusculasOConEspaciosEsAceptadoYLlegaAlServicio(String correo) throws Exception {
        UsuarioSesionDTO sesionDTO = new UsuarioSesionDTO(UUID.randomUUID(), "Profesor", "profesor@uco.net.co", RolUsuario.PROFESOR);
        when(authService.autenticar(any(LoginRequestDTO.class)))
                .thenReturn(new LoginResponseDTO("token", "Bearer", 3600L, sesionDTO));

        String json = """
                {
                    "correoInstitucional": "%s",
                    "contrasena": "Password123*"
                }
                """.formatted(correo);

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(authService).autenticar(any(LoginRequestDTO.class));
    }

    @Test
    void loginConCorreoVacioDevuelve400() throws Exception {
        String json = """
                {
                    "correoInstitucional": "",
                    "contrasena": "Password123*"
                }
                """;

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void loginConPasswordVacioDevuelve400() throws Exception {
        String json = """
                {
                    "correoInstitucional": "profesor@uco.net.co",
                    "contrasena": ""
                }
                """;

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void loginConCorreoNoInstitucionalDevuelve400() throws Exception {
        String json = """
                {
                    "correoInstitucional": "usuario@gmail.com",
                    "contrasena": "Password123*"
                }
                """;

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void loginConDominioDistintoAunqueSeaEnMayusculasDevuelve400() throws Exception {
        String json = """
                {
                    "correoInstitucional": "PROFESOR@GMAIL.COM",
                    "contrasena": "Password123*"
                }
                """;

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginCredencialesInvalidasDevuelve401SinDetallesTecnicos() throws Exception {
        when(authService.autenticar(any(LoginRequestDTO.class)))
                .thenThrow(new CredencialesInvalidasException());

        String json = """
                {
                    "correoInstitucional": "profesor@uco.net.co",
                    "contrasena": "PasswordErroneo"
                }
                """;

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.sql").doesNotExist());
    }

    @Test
    void loginUsuarioBloqueadoDevuelve423Locked() throws Exception {
        when(authService.autenticar(any(LoginRequestDTO.class)))
                .thenThrow(new UsuarioBloqueadoException());

        String json = """
                {
                    "correoInstitucional": "profesor@uco.net.co",
                    "contrasena": "PasswordCorrecto"
                }
                """;

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.status").value(423))
                .andExpect(jsonPath("$.error").value("LOCKED"))
                .andExpect(jsonPath("$.mensaje").value("El usuario está temporalmente bloqueado. Intente nuevamente más tarde."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // RNF-17: ninguna respuesta de error debe revelar detalles internos.

    @Test
    void jsonMalformadoDevuelve400SinDetallesDeParseo() throws Exception {
        String jsonRoto = "{\"correoInstitucional\": \"profesor@uco.net.co\", ";

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRoto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.mensaje").value("El cuerpo de la petición no es válido o está mal formado"))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("fasterxml"))))
                .andExpect(content().string(not(containsString("co.edu"))));
    }

    @Test
    void contentTypeNoSoportadoDevuelve415SinDetallesTecnicos() throws Exception {
        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("correoInstitucional=profesor@uco.net.co"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("co.edu"))));
    }

    @Test
    void errorInternoConDetalleDeBaseDeDatosNoSeFiltraEnLaRespuesta() throws Exception {
        when(authService.autenticar(any(LoginRequestDTO.class)))
                .thenThrow(new IllegalStateException("org.postgresql.util.PSQLException: password=secreto123 SQL [select]"));

        String json = """
                {
                    "correoInstitucional": "profesor@uco.net.co",
                    "contrasena": "PasswordCorrecto"
                }
                """;

        mockMvc.perform(post(URL_LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(content().string(not(containsString("PSQL"))))
                .andExpect(content().string(not(containsString("secreto123"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("SQL"))))
                .andExpect(content().string(not(containsString("co.edu"))));
    }
}
