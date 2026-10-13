package co.edu.uco.sigra.estudiantes.seguridad;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el control de acceso por rol del módulo de matrículas (temporal hasta RF-05), solo para
 * /api/v1/matriculas/**; con el interruptor apagado no cambia ninguna respuesta.
 * <p>
 * Es propio del módulo y no modifica {@code config.WebConfig}: Spring compone todos los beans
 * {@code WebMvcConfigurer} del contexto, así que este registro funciona igual sin tocar un archivo
 * compartido con otros módulos.
 */
@Configuration
@RequiredArgsConstructor
public class ConfiguracionWebMatricula implements WebMvcConfigurer {

    private final ControlAccesoMatriculaInterceptor controlAccesoMatricula;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(controlAccesoMatricula)
                .addPathPatterns("/api/v1/matriculas", "/api/v1/matriculas/**");
    }
}
