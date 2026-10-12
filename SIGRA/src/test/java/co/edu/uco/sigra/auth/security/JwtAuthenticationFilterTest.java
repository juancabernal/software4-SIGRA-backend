package co.edu.uco.sigra.auth.security;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.springframework.dao.DataAccessResourceFailureException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final JsonAuthenticationEntryPoint entryPoint = mock(JsonAuthenticationEntryPoint.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
            jwtService, usuarioRepository, entryPoint, JsonMapper.builder().build());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ausenciaDeBearerDejaLaSolicitudAnonimaYNoConsultaLaBase() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/programas");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwtService, usuarioRepository, entryPoint);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void bearerVacioSeRechazaCon401YNoContinua() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/programas");
        request.addHeader("Authorization", "Bearer   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        verify(entryPoint).commence(eq(request), eq(response), any());
        verifyNoInteractions(jwtService, usuarioRepository);
    }

    @Test
    void identidadSeContrastaContraUsuarioActivoPersistido() throws Exception {
        UUID id = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        ProfesorStub usuario = new ProfesorStub(id, "profesor@uco.net.co");
        when(jwtService.extraerClaims("token-valido")).thenReturn(claims);
        when(claims.getSubject()).thenReturn(id.toString());
        when(claims.get("rol", String.class)).thenReturn(RolUsuario.PROFESOR.name());
        when(claims.get("correo", String.class)).thenReturn("profesor@uco.net.co");
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/asignaturas");
        request.addHeader("Authorization", "Bearer token-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();
        doAnswer(invocation -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(entryPoint);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void rolDeclaradoDistintoAlPersistidoSeRechaza() throws Exception {
        UUID id = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        ProfesorStub usuario = new ProfesorStub(id, "profesor@uco.net.co");
        when(jwtService.extraerClaims("token-manipulado")).thenReturn(claims);
        when(claims.getSubject()).thenReturn(id.toString());
        when(claims.get("rol", String.class)).thenReturn(RolUsuario.ADMINISTRADOR.name());
        when(claims.get("correo", String.class)).thenReturn("profesor@uco.net.co");
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/asignaturas");
        request.addHeader("Authorization", "Bearer token-manipulado");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(entryPoint).commence(eq(request), eq(response), any());
        verifyNoInteractions(chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void noReutilizaElContextoDeUnaSolicitudAnterior() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("anterior", null));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/programas");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void indisponibilidadExplicitaDePersistenciaResponde503JsonYNoSeConvierteEn401() throws Exception {
        UUID id = UUID.randomUUID();
        Claims claims = claimsConIdentidadValida(id, RolUsuario.PROFESOR, "profesor@uco.net.co");
        when(jwtService.extraerClaims("token-db-fallo")).thenReturn(claims);
        DataAccessResourceFailureException fallo = new DataAccessResourceFailureException("database unavailable");
        when(usuarioRepository.findById(id)).thenThrow(fallo);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/programas");
        request.addHeader("Authorization", "Bearer token-db-fallo");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("SERVICE_UNAVAILABLE")
                .doesNotContain("database unavailable", "RuntimeException", "co.edu");
        verifyNoInteractions(entryPoint);
    }

    @Test
    void falloInesperadoDelRepositorioResponde500JsonSinDetallesTecnicos() throws Exception {
        UUID id = UUID.randomUUID();
        Claims claims = claimsConIdentidadValida(id, RolUsuario.PROFESOR, "profesor@uco.net.co");
        when(jwtService.extraerClaims("token-fallo-interno")).thenReturn(claims);
        when(usuarioRepository.findById(id)).thenThrow(new IllegalStateException("bug interno"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/programas");
        request.addHeader("Authorization", "Bearer token-fallo-interno");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentAsString()).contains("INTERNAL_SERVER_ERROR")
                .doesNotContain("bug interno", "IllegalStateException", "co.edu");
        verifyNoInteractions(entryPoint);
    }

    @Test
    void excepcionDeLaCadenaPosteriorSePropagaSinConvertirseEn401() throws Exception {
        UUID id = UUID.randomUUID();
        Claims claims = claimsConIdentidadValida(id, RolUsuario.PROFESOR, "profesor@uco.net.co");
        when(jwtService.extraerClaims("token-downstream")).thenReturn(claims);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(new ProfesorStub(id, "profesor@uco.net.co")));
        RuntimeException fallo = new RuntimeException("controller failure");
        FilterChain chain = (req, res) -> { throw fallo; };

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/asignaturas");
        request.addHeader("Authorization", "Bearer token-downstream");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(request, response, chain)).isSameAs(fallo);
        verifyNoInteractions(entryPoint);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private Claims claimsConIdentidadValida(UUID id, RolUsuario rol, String correo) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(id.toString());
        when(claims.get("rol", String.class)).thenReturn(rol.name());
        when(claims.get("correo", String.class)).thenReturn(correo);
        return claims;
    }

    private static final class ProfesorStub extends Usuario {
        private ProfesorStub(UUID id, String correo) {
            setId(id);
            setCorreoInstitucional(correo);
            setEstado(EstadoRegistro.ACTIVO);
        }

        @Override
        public RolUsuario getRol() {
            return RolUsuario.PROFESOR;
        }
    }
}
