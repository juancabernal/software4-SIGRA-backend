package co.edu.uco.sigra.evaluaciones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj del módulo de evaluaciones para calcular "hoy" (semestre activo, fecha dentro del semestre) en
 * la zona horaria de Colombia. Se inyecta en lugar de llamar a LocalDate.now() para poder fijar la
 * fecha en las pruebas. Tiene nombre propio, como los de semestres, auth y matrícula.
 */
@Configuration
public class EvaluacionConfig {

    public static final String RELOJ_EVALUACIONES = "relojEvaluaciones";
    public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Bean(name = RELOJ_EVALUACIONES)
    public Clock relojEvaluaciones() {
        return Clock.system(ZONA_COLOMBIA);
    }
}
