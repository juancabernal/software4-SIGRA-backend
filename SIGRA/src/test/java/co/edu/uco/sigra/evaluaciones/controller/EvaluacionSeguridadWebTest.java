package co.edu.uco.sigra.evaluaciones.controller;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.config.SecurityConfig;
import co.edu.uco.sigra.evaluaciones.service.EvaluacionService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seguridad de evaluaciones (RF-10 / RF-11) con la cadena de Spring Security real y el servicio simulado.
 * Comprueba, por rol, la matriz documentada en {@code docs/security/RBAC_MATRIX.md}: las seis operaciones son
 * exclusivas del PROFESOR. La propiedad sobre la asignatura se valida en el servicio y se prueba en
 * {@code EvaluacionServiceImplTest}.
 */
@WebMvcTest(controllers = EvaluacionController.class)
@Import({SecurityConfig.class, CurrentUser.class})
@DisplayName("Seguridad de evaluaciones (cadena real)")
class EvaluacionSeguridadWebTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-e400-000000000001");
    private static final String CUERPO = """
            {"nombre":"Parcial 1","fechaTentativa":"2026-11-10","porcentaje":30,"descripcion":"Primer parcial del corte"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvaluacionService evaluacionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    /** Operación protegida y el código que recibe el profesor cuando la ejecuta con éxito. */
    record Operacion(String nombre, HttpMethod metodo, String ruta, String cuerpo, int estadoExito) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    static Stream<Operacion> operaciones() {
        return Stream.of(
                new Operacion("POST crear evaluación", HttpMethod.POST,
                        "/api/v1/resultados-aprendizaje/" + ID + "/evaluaciones", CUERPO, 201),
                new Operacion("GET evaluaciones de un RA", HttpMethod.GET,
                        "/api/v1/resultados-aprendizaje/" + ID + "/evaluaciones", null, 200),
                new Operacion("GET evaluación por id", HttpMethod.GET,
                        "/api/v1/evaluaciones/" + ID, null, 200),
                new Operacion("PUT modificar evaluación", HttpMethod.PUT,
                        "/api/v1/evaluaciones/" + ID, CUERPO, 200),
                new Operacion("PATCH inactivar evaluación", HttpMethod.PATCH,
                        "/api/v1/evaluaciones/" + ID + "/inactivar", null, 200),
                new Operacion("PATCH reactivar evaluación", HttpMethod.PATCH,
                        "/api/v1/evaluaciones/" + ID + "/reactivar", null, 200));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operaciones")
    @DisplayName("Sin token devuelve 401 y no ejecuta el servicio")
    void anonimoEs401(Operacion op) throws Exception {
        mockMvc.perform(peticion(op, null))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        verifyNoInteractions(evaluacionService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operaciones")
    @DisplayName("El estudiante recibe 403 y no ejecuta el servicio")
    void estudianteEs403(Operacion op) throws Exception {
        configurarToken("token-estudiante", RolUsuario.ESTUDIANTE);

        mockMvc.perform(peticion(op, "token-estudiante"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        verifyNoInteractions(evaluacionService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operaciones")
    @DisplayName("El administrador recibe 403 y no ejecuta el servicio")
    void administradorEs403(Operacion op) throws Exception {
        configurarToken("token-admin", RolUsuario.ADMINISTRADOR);

        mockMvc.perform(peticion(op, "token-admin"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        verifyNoInteractions(evaluacionService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operaciones")
    @DisplayName("El profesor accede y llega al servicio")
    void profesorAccede(Operacion op) throws Exception {
        configurarToken("token-profesor", RolUsuario.PROFESOR);

        mockMvc.perform(peticion(op, "token-profesor"))
                .andExpect(status().is(op.estadoExito()));
    }

    private static MockHttpServletRequestBuilder peticion(Operacion op, String token) {
        MockHttpServletRequestBuilder builder = request(op.metodo(), op.ruta());
        if (op.cuerpo() != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(op.cuerpo());
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return builder;
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
