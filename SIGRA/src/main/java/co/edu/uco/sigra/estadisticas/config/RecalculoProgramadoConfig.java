package co.edu.uco.sigra.estadisticas.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa la planificación de Spring solo para el recálculo periódico del nivel de logro
 * ({@code RecalculoProgramadoNivelLogro}).
 * <p>
 * Propiedad {@value #PROPIEDAD_HABILITADO} (por defecto true). Las pruebas la apagan en
 * {@code src/test/resources/config/application.properties} para que el job no escriba niveles sobre los
 * datos de prueba de otros módulos mientras sus pruebas de integración se ejecutan.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = RecalculoProgramadoConfig.PROPIEDAD_HABILITADO, havingValue = "true",
        matchIfMissing = true)
public class RecalculoProgramadoConfig {

    public static final String PROPIEDAD_HABILITADO = "sigra.estadisticas.recalculo-programado.habilitado";
}
