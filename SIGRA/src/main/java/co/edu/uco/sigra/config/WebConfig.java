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
 * También registra el control de acceso por rol (temporal hasta RF-05): nació en el módulo de
 * asignaturas pero es genérico (solo mira la anotación {@code RolesPermitidos} del método), así que
 * también se usa para las conclusiones automáticas (RF-16) y lo que se agregue de resultados de
 * aprendizaje; con el interruptor apagado no cambia ninguna respuesta.
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
                .addPathPatterns("/api/v1/asignaturas", "/api/v1/asignaturas/**",
                        "/api/v1/conclusiones", "/api/v1/conclusiones/**",
                        "/api/v1/resultados-aprendizaje/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
