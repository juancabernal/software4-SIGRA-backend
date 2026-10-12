package co.edu.uco.sigra.auth.security;

import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);

    private final JsonMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setHeader("WWW-Authenticate", "Bearer");
        log.debug("Evento seguridad=AUTHENTICATION_REQUIRED requestId={} causa={}",
                RequestCorrelationFilter.currentRequestId(),
                authException.getClass().getSimpleName());
        JsonSecurityResponse.write(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Debe iniciar sesión para acceder a este recurso", objectMapper);
    }
}
