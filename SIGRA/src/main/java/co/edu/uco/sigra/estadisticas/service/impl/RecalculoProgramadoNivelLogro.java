package co.edu.uco.sigra.estadisticas.service.impl;

import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.config.RecalculoProgramadoConfig;
import co.edu.uco.sigra.estadisticas.repository.CalificacionesNivelLogroRepository;
import co.edu.uco.sigra.estadisticas.service.NivelLogroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Disparador automático del recálculo del nivel de logro (RF-14 / RF-15) — decisión del módulo de estadísticas.
 * <p>
 * Cada {@code sigra.estadisticas.recalculo-programado.intervalo-ms} (por defecto 5 minutos) recalcula todos los
 * (RA, semestre) que tienen evaluaciones en un semestre en curso. Se eligió un job periódico porque no depende de
 * que los módulos de calificaciones o evaluaciones publiquen eventos y cubre también la inactivación y
 * reactivación de evaluaciones, que no notifican a nadie. Costo: recalcula aunque nada haya cambiado (el
 * recálculo es idempotente y RF-16 no duplica conclusiones). El endpoint manual de recálculo sigue disponible.
 * <p>
 * Corre como el sistema, no como un usuario: {@link #SISTEMA} es una identidad interna con rol ADMINISTRADOR que
 * nunca sale de este componente ni llega desde un cliente (el filtro JWT construye sus propios principals).
 * Un error en un (RA, semestre) se registra y no detiene el resto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = RecalculoProgramadoConfig.PROPIEDAD_HABILITADO, havingValue = "true",
        matchIfMissing = true)
public class RecalculoProgramadoNivelLogro {

    /** Identidad interna del job (UUID nulo reservado, rol ADMINISTRADOR para el alcance global). */
    static final UsuarioPrincipal SISTEMA = new UsuarioPrincipal(new UUID(0L, 0L), RolUsuario.ADMINISTRADOR);

    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    private final CalificacionesNivelLogroRepository calificacionesNivelLogroRepository;
    private final NivelLogroService nivelLogroService;

    @Scheduled(fixedDelayString = "${sigra.estadisticas.recalculo-programado.intervalo-ms:300000}",
            initialDelayString = "${sigra.estadisticas.recalculo-programado.retraso-inicial-ms:60000}")
    public void recalcularSemestresEnCurso() {
        List<Object[]> pares = calificacionesNivelLogroRepository
                .buscarParesConEvaluacionesEnSemestreEnCurso(LocalDate.now(ZONA_COLOMBIA));
        int fallidos = 0;
        for (Object[] par : pares) {
            UUID resultadoAprendizajeId = (UUID) par[0];
            UUID semestreId = (UUID) par[1];
            try {
                nivelLogroService.recalcular(resultadoAprendizajeId, semestreId, SISTEMA);
            } catch (RuntimeException ex) {
                fallidos++;
                log.warn("Recálculo programado fallido para RA {} y semestre {}: {}",
                        resultadoAprendizajeId, semestreId, ex.getMessage());
            }
        }
        log.debug("Recálculo programado de nivel de logro: {} (RA, semestre), {} con error", pares.size(), fallidos);
    }
}
