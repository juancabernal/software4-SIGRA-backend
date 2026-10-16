package co.edu.uco.sigra.calificaciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class CalificacionConfig {

    public static final String RELOJ_CALIFICACIONES = "relojCalificaciones";
    public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Bean(name = RELOJ_CALIFICACIONES)
    public Clock relojCalificaciones() {
        return Clock.system(ZONA_COLOMBIA);
    }
}