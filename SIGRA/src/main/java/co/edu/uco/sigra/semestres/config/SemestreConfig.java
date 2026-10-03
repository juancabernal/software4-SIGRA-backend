package co.edu.uco.sigra.semestres.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj usado por el módulo de semestres para calcular "hoy" en la zona horaria de Colombia.
 * Se inyecta (en lugar de llamar a LocalDate.now()) para poder fijar la fecha en las pruebas.
 * Tiene nombre propio para no chocar con un posible Clock global que defina otro módulo.
 */
@Configuration
public class SemestreConfig {

    public static final String RELOJ_SEMESTRES = "relojSemestres";
    public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Bean(name = RELOJ_SEMESTRES)
    public Clock relojSemestres() {
        return Clock.system(ZONA_COLOMBIA);
    }
}
