package co.edu.uco.sigra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS para que el frontend Angular consuma la API. Los orígenes se configuran con
 * {@code cors.allowed-origins} (separados por comas) y por defecto es http://localhost:4200.
 * SecurityConfig activa {@code http.cors(...)}, que toma esta misma configuración para las
 * peticiones preflight.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] origenesPermitidos;

    public WebConfig(@Value("${cors.allowed-origins:http://localhost:4200}") String[] origenesPermitidos) {
        this.origenesPermitidos = origenesPermitidos;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
