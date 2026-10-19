package co.edu.uco.sigra.resultadosaprendizaje.controller;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.security.CurrentUser;
import co.edu.uco.sigra.auth.security.SecurityAuthorizationService;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.config.SecurityConfig;
import co.edu.uco.sigra.resultadosaprendizaje.service.ResultadoAprendizajeService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seguridad de resultados de aprendizaje (RF-06) con la cadena de Spring Security real y el servicio simulado.
 * Comprueba, por rol, la matriz de {@code docs/security/RBAC_MATRIX.md}:
 * <ul>
 *   <li>Registrar, listar todos, modificar, inactivar y reactivar: solo ADMINISTRADOR.</li>
 *   <li>Consultar por asignatura y por id: ADMINISTRADOR, o PROFESOR con asignación docente activa.</li>
 * </ul>
 * La regla de asignación ({@code @securityAuthorization}) se simula: aquí se verifica que la anotación la
 * invoque y respete su respuesta, y que ESTUDIANTE y anónimo nunca lleguen al servicio.
 */
@WebMvcTest(controllers = ResultadoAprendizajeController.class)
@Import({SecurityConfig.class, CurrentUser.class})
@DisplayName("Seguridad de resultados de aprendizaje (cadena real)")
class ResultadoAprendizajeSeguridadWebTest {

    private static final UUID ID_ASIGNATURA = UUID.fromString("00000000-0000-4000-f100-000000000001");
    private static final UUID ID_RA = UUID.fromString("00000000-0000-4000-c000-000000000003");
    private static final String CUERPO_CREAR = "{\"codigo\":\"RA-01\",\"descripcion\":\"Analiza los requisitos de un sistema\"}";
    private static final String CUERPO_ACTUALIZAR = "{\"descripcion\":\"Nueva descripción del resultado\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResultadoAprendizajeService resultadoAprendizajeService;

    /** Nombre del bean que usan las expresiones {@code @securityAuthorization...} del controlador. */
    @MockitoBean(name = "securityAuthorization")
    private SecurityAuthorizationService securityAuthorization;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    record Operacion(String nombre, HttpMethod metodo, String ruta, String cuerpo, int estadoExito) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    /** Operaciones exclusivas del administrador. */
    static Stream<Operacion> soloAdministrador() {
        return Stream.of(
                new Operacion("POST registrar RA", HttpMethod.POST,
                        "/api/v1/asignaturas/" + ID_ASIGNATURA + "/resultados-aprendizaje", CUERPO_CREAR, 201),
                new Operacion("GET listar todos los RA", HttpMethod.GET,
                        "/api/v1/resultados-aprendizaje", null, 200),
                new Operacion("PUT modificar descripción", HttpMethod.PUT,
                        "/api/v1/resultados-aprendizaje/" + ID_RA, CUERPO_ACTUALIZAR, 200),
                new Operacion("PATCH inactivar RA", HttpMethod.PATCH,
                        "/api/v1/resultados-aprendizaje/" + ID_RA + "/inactivar", null, 200),
                new Operacion("PATCH reactivar RA", HttpMethod.PATCH,
                        "/api/v1/resultados-aprendizaje/" + ID_RA + "/reactivar", null, 200));
    }

    /** Consultas abiertas al administrador y al profesor asignado. */
    static Stream<Operacion> consultasConAsignacion() {
        return Stream.of(
                new Operacion("GET RA de una asignatura", HttpMethod.GET,
                        "/api/v1/asignaturas/" + ID_ASIGNATURA + "/resultados-aprendizaje", null, 200),
                new Operacion("GET RA por id", HttpMethod.GET,
                        "/api/v1/resultados-aprendizaje/" + ID_RA, null, 200));
    }

    static Stream<Operacion> todas() {
        return Stream.concat(soloAdministrador(), consultasConAsignacion());
    }

    @Nested
    @DisplayName("Anónimo y estudiante")
    class SinAcceso {

        @ParameterizedTest(name = "{0}")
        @MethodSource("co.edu.uco.sigra.resultadosaprendizaje.controller.ResultadoAprendizajeSeguridadWebTest#todas")
        @DisplayName("Sin token devuelve 401 y no ejecuta el servicio")
        void anonimoEs401(Operacion op) throws Exception {
            mockMvc.perform(peticion(op, null))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
            verifyNoInteractions(resultadoAprendizajeService);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("co.edu.uco.sigra.resultadosaprendizaje.controller.ResultadoAprendizajeSeguridadWebTest#todas")
        @DisplayName("El estudiante recibe 403 y no ejecuta el servicio")
        void estudianteEs403(Operacion op) throws Exception {
            configurarToken("token-estudiante", RolUsuario.ESTUDIANTE);

            mockMvc.perform(peticion(op, "token-estudiante"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"));
            verifyNoInteractions(resultadoAprendizajeService);
        }
    }

    @Nested
    @DisplayName("Administrador")
    class Administrador {

        @ParameterizedTest(name = "{0}")
        @MethodSource("co.edu.uco.sigra.resultadosaprendizaje.controller.ResultadoAprendizajeSeguridadWebTest#todas")
        @DisplayName("Accede a todas las operaciones")
        void administradorAccede(Operacion op) throws Exception {
            configurarToken("token-admin", RolUsuario.ADMINISTRADOR);

            mockMvc.perform(peticion(op, "token-admin"))
                    .andExpect(status().is(op.estadoExito()));
        }
    }

    @Nested
    @DisplayName("Profesor")
    class Profesor {

        @ParameterizedTest(name = "{0}")
        @MethodSource("co.edu.uco.sigra.resultadosaprendizaje.controller.ResultadoAprendizajeSeguridadWebTest#soloAdministrador")
        @DisplayName("No puede gestionar RA: recibe 403 y no ejecuta el servicio")
        void profesorNoGestiona(Operacion op) throws Exception {
            configurarToken("token-profesor", RolUsuario.PROFESOR);

            mockMvc.perform(peticion(op, "token-profesor"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"));
            verifyNoInteractions(resultadoAprendizajeService);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("co.edu.uco.sigra.resultadosaprendizaje.controller.ResultadoAprendizajeSeguridadWebTest#consultasConAsignacion")
        @DisplayName("Con asignación docente activa puede consultar")
        void profesorAsignadoConsulta(Operacion op) throws Exception {
            configurarToken("token-profesor", RolUsuario.PROFESOR);
            when(securityAuthorization.hasActiveAssignment(eq(ID_ASIGNATURA), any())).thenReturn(true);
            when(securityAuthorization.hasActiveAssignmentForResultado(eq(ID_RA), any())).thenReturn(true);

            mockMvc.perform(peticion(op, "token-profesor"))
                    .andExpect(status().is(op.estadoExito()));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("co.edu.uco.sigra.resultadosaprendizaje.controller.ResultadoAprendizajeSeguridadWebTest#consultasConAsignacion")
        @DisplayName("Sin asignación docente activa recibe 403 y no ejecuta el servicio")
        void profesorSinAsignacionEs403(Operacion op) throws Exception {
            configurarToken("token-profesor", RolUsuario.PROFESOR);
            when(securityAuthorization.hasActiveAssignment(any(), any())).thenReturn(false);
            when(securityAuthorization.hasActiveAssignmentForResultado(any(), any())).thenReturn(false);

            mockMvc.perform(peticion(op, "token-profesor"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"));
            verifyNoInteractions(resultadoAprendizajeService);
        }
    }

    @Test
    @DisplayName("El filtro por estado no abre la lista general al profesor")
    void profesorNoListaTodosAunConFiltro() throws Exception {
        configurarToken("token-profesor", RolUsuario.PROFESOR);

        mockMvc.perform(request(HttpMethod.GET, "/api/v1/resultados-aprendizaje?estado=ACTIVO")
                        .header("Authorization", "Bearer token-profesor"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(resultadoAprendizajeService);
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
