package co.edu.uco.sigra.auth.security;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.common.util.Correos;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.UUID;

/** Convierte un Bearer JWT validado en una autenticación de Spring por solicitud. */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final tools.jackson.databind.json.JsonMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository,
                                   AuthenticationEntryPoint authenticationEntryPoint,
                                   tools.jackson.databind.json.JsonMapper objectMapper) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // La API es stateless: una autenticación nunca puede heredarse de otra solicitud.
        SecurityContextHolder.clearContext();
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!header.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
            rechazar(request, response, new BadCredentialsException("Bearer ausente"));
            return;
        }
        String token = header.substring(PREFIJO.length()).strip();
        if (token.isEmpty()) {
            rechazar(request, response, new BadCredentialsException("Bearer vacío"));
            return;
        }

        Claims claims;
        UUID id;
        RolUsuario rolToken;
        String correoToken;
        try {
            claims = jwtService.extraerClaims(token);
            id = UUID.fromString(claims.getSubject());
            rolToken = RolUsuario.valueOf(claims.get("rol", String.class));
            correoToken = Correos.normalizar(claims.get("correo", String.class));
        } catch (BadCredentialsException ex) {
            SecurityContextHolder.clearContext();
            log.debug("Evento seguridad=JWT_REJECTED requestId={} causa={}",
                    RequestCorrelationFilter.currentRequestId(), ex.getClass().getSimpleName());
            rechazar(request, response, ex);
            return;
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            log.debug("Evento seguridad=JWT_REJECTED requestId={} causa={}",
                    RequestCorrelationFilter.currentRequestId(), ex.getClass().getSimpleName());
            rechazar(request, response, new BadCredentialsException("Bearer JWT inválido", ex));
            return;
        }

        Usuario usuario;
        try {
            usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado"));
        } catch (BadCredentialsException ex) {
            SecurityContextHolder.clearContext();
            log.debug("Evento seguridad=JWT_REJECTED requestId={} causa={}",
                    RequestCorrelationFilter.currentRequestId(), ex.getClass().getSimpleName());
            rechazar(request, response, ex);
            return;
        } catch (DataAccessException | PersistenceException | CannotCreateTransactionException ex) {
            SecurityContextHolder.clearContext();
            log.error("Evento seguridad=AUTH_REPOSITORY_UNAVAILABLE requestId={} causa={}",
                    RequestCorrelationFilter.currentRequestId(), ex.getClass().getSimpleName());
            JsonSecurityResponse.write(response, HttpStatus.SERVICE_UNAVAILABLE,
                    "SERVICE_UNAVAILABLE", "El servicio no está disponible temporalmente", objectMapper);
            return;
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            log.error("Evento seguridad=AUTH_INTERNAL_ERROR requestId={} causa={}",
                    RequestCorrelationFilter.currentRequestId(), ex.getClass().getSimpleName());
            JsonSecurityResponse.write(response, HttpStatus.INTERNAL_SERVER_ERROR,
                    "INTERNAL_SERVER_ERROR", "Ocurrió un error inesperado en el servidor", objectMapper);
            return;
        }

        try {
            validarUsuario(usuario, rolToken, correoToken);
            establecerAutenticacion(usuario);
        } catch (BadCredentialsException ex) {
            SecurityContextHolder.clearContext();
            log.debug("Evento seguridad=JWT_REJECTED requestId={} causa={}",
                    RequestCorrelationFilter.currentRequestId(), ex.getClass().getSimpleName());
            rechazar(request, response, ex);
            return;
        }

        // La cadena posterior queda fuera del bloque que traduce credenciales. Sus excepciones
        // conservan su semántica y no se convierten en 401.
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Evita que una ejecución directa del filtro en un contenedor o en una prueba deje
            // identidad en el hilo reutilizado por otra solicitud.
            SecurityContextHolder.clearContext();
        }
    }

    private void validarUsuario(Usuario usuario, RolUsuario rolToken, String correoToken) {
        if (usuario.getEstado() != EstadoRegistro.ACTIVO
                || usuario.getRol() != rolToken
                || !Correos.normalizar(usuario.getCorreoInstitucional()).equals(correoToken)) {
            throw new BadCredentialsException("Identidad JWT inconsistente");
        }
    }

    private void establecerAutenticacion(Usuario usuario) {
        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new UsuarioPrincipal(usuario.getId(), usuario.getCorreoInstitucional(), usuario.getRol()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return ("POST".equalsIgnoreCase(request.getMethod()) && "/api/v1/auth/login".equals(path))
                || ("GET".equalsIgnoreCase(request.getMethod()) && "/actuator/health".equals(path))
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private void rechazar(HttpServletRequest request, HttpServletResponse response,
                          BadCredentialsException ex) throws IOException, ServletException {
        authenticationEntryPoint.commence(request, response, ex);
    }
}
