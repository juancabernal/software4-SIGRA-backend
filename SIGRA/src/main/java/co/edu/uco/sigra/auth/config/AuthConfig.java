package co.edu.uco.sigra.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AuthConfig {

    public static final String RELOJ_AUTH = "authClock";
    public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Bean(name = RELOJ_AUTH)
    public Clock authClock() {
        return Clock.system(ZONA_COLOMBIA);
    }
}
