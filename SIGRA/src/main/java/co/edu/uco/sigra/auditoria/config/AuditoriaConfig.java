package co.edu.uco.sigra.auditoria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuditoriaConfig {

    public static final String RELOJ_AUDITORIA = "relojAuditoria";
    public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Bean(name = RELOJ_AUDITORIA)
    public Clock relojAuditoria() {
        return Clock.system(ZONA_COLOMBIA);
    }
}