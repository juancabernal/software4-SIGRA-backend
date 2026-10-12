package co.edu.uco.sigra.estudiantes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj usado por el módulo de matrículas para calcular "hoy" en la zona horaria de Colombia
 * (design.md, D2).
 * <p>
 * Se inyecta en lugar de llamar a {@code LocalDate.now()} porque es la única forma de fijar la
 * fecha en las pruebas: sin él no se puede comprobar de manera determinista que la rematrícula
 * actualiza la fecha (hay que mover el reloj a otro día).
 * <p>
 * Tiene nombre propio y no se reusa {@code SemestreConfig.RELOJ_SEMESTRES} para no atar este
 * módulo a un bean ajeno por conveniencia: declarar el propio cuesta diez líneas y es lo que el
 * autor de aquel documentó que se hiciera, justamente para que dos módulos no choquen por un
 * mismo {@link Clock}.
 */
@Configuration
public class MatriculaConfig {

    public static final String RELOJ_MATRICULAS = "relojMatriculas";
    public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Bean(name = RELOJ_MATRICULAS)
    public Clock relojMatriculas() {
        return Clock.system(ZONA_COLOMBIA);
    }
}
