package co.edu.uco.sigra.config;

import co.edu.uco.sigra.asignaturas.seguridad.ControlAccesoAsignaturasInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS para que el frontend Angular consuma la API. Los orígenes se configuran con
 * {@code cors.allowed-origins} (separados por comas) y por defecto es http://localhost:4200.
 * SecurityConfig activa {@code http.cors(...)}, que toma esta misma configuración para las
 * peticiones preflight.
 * <p>
 * También registra el control de acceso por rol del módulo de asignaturas (temporal hasta RF-05),
 * solo para /api/v1/asignaturas/**; con el interruptor apagado no cambia ninguna respuesta.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] origenesPermitidos;
    private final ControlAccesoAsignaturasInterceptor controlAccesoAsignaturas;

    public WebConfig(@Value("${cors.allowed-origins:http://localhost:4200}") String[] origenesPermitidos,
                     ControlAccesoAsignaturasInterceptor controlAccesoAsignaturas) {
        this.origenesPermitidos = origenesPermitidos;
        this.controlAccesoAsignaturas = controlAccesoAsignaturas;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(controlAccesoAsignaturas)
                .addPathPatterns("/api/v1/asignaturas", "/api/v1/asignaturas/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
