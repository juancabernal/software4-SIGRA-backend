package co.edu.uco.sigra.estadisticas.service.impl;

import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estadisticas.config.RecalculoProgramadoConfig;
import co.edu.uco.sigra.estadisticas.repository.CalificacionesNivelLogroRepository;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
import co.edu.uco.sigra.estadisticas.service.NivelLogroService;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Disparador automático del recálculo del nivel de logro (RF-14 / RF-15) — decisión del módulo de estadísticas.
 * <p>
 * Cada {@code sigra.estadisticas.recalculo-programado.intervalo-ms} (por defecto 5 minutos) revisa todos los
 * (RA, semestre) que tienen evaluaciones en un semestre en curso y recalcula solo los que tienen calificaciones
 * más nuevas que su último cálculo. Se eligió un job periódico porque no depende de que los módulos de
 * calificaciones o evaluaciones publiquen eventos. El endpoint manual de recálculo sigue disponible.
 * <p>
 * Además, cada {@code sigra.estadisticas.recalculo-programado.ejecuciones-por-barrido-completo} ejecuciones se
 * hace un barrido completo (recalcula todos los pares, sin filtrar), porque la tabla {@code evaluacion} no tiene
 * columna de modificación: si alguien cambia el porcentaje de una evaluación o la inactiva, ninguna fecha de
 * calificación se mueve y el filtro de arriba nunca lo detectaría. El barrido completo es la red que lo corrige.
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
    private final NivelLogroEstudianteRepository nivelLogroEstudianteRepository;
    private final NivelLogroService nivelLogroService;

    @Value("${sigra.estadisticas.recalculo-programado.ejecuciones-por-barrido-completo:12}")
    private int ejecucionesPorBarridoCompleto = 12;

    private int ejecuciones;

    @Scheduled(fixedDelayString = "${sigra.estadisticas.recalculo-programado.intervalo-ms:300000}",
            initialDelayString = "${sigra.estadisticas.recalculo-programado.retraso-inicial-ms:60000}")
    public void recalcularSemestresEnCurso() {
        LocalDate hoy = LocalDate.now(ZONA_COLOMBIA);
        List<Object[]> pares = calificacionesNivelLogroRepository.buscarParesConEvaluacionesEnSemestreEnCurso(hoy);

        ejecuciones++;
        boolean barridoCompleto = ejecuciones % ejecucionesPorBarridoCompleto == 0;

        List<Object[]> paresARecalcular;
        if (barridoCompleto) {
            paresARecalcular = pares;
        } else {
            Map<List<UUID>, LocalDateTime> ultimaCalificacionPorPar = indexar(
                    calificacionesNivelLogroRepository.buscarUltimaCalificacionQueCuentaPorParEnSemestreEnCurso(
                            EstadoEvaluacion.ACTIVA, EstadoMatricula.ACTIVA, hoy));
            Map<List<UUID>, LocalDateTime> ultimoCalculoPorPar = indexar(
                    nivelLogroEstudianteRepository.buscarUltimoCalculoPorPar());

            paresARecalcular = pares.stream()
                    .filter(par -> tieneCalificacionesMasNuevasQueElUltimoCalculo(
                            par, ultimaCalificacionPorPar, ultimoCalculoPorPar))
                    .toList();
        }

        int fallidos = 0;
        for (Object[] par : paresARecalcular) {
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
        log.debug("Recálculo programado de nivel de logro: {} (RA, semestre) candidatos, {} recalculados, "
                        + "{} omitidos sin cambios, {} con error, barrido completo: {}",
                pares.size(), paresARecalcular.size(), pares.size() - paresARecalcular.size(), fallidos,
                barridoCompleto);
    }

    private static Map<List<UUID>, LocalDateTime> indexar(List<Object[]> filas) {
        Map<List<UUID>, LocalDateTime> porPar = new HashMap<>();
        for (Object[] fila : filas) {
            porPar.put(List.of((UUID) fila[0], (UUID) fila[1]), (LocalDateTime) fila[2]);
        }
        return porPar;
    }

    private static boolean tieneCalificacionesMasNuevasQueElUltimoCalculo(
            Object[] par, Map<List<UUID>, LocalDateTime> ultimaCalificacionPorPar,
            Map<List<UUID>, LocalDateTime> ultimoCalculoPorPar) {
        List<UUID> clave = List.of((UUID) par[0], (UUID) par[1]);
        LocalDateTime ultimaCalificacion = ultimaCalificacionPorPar.get(clave);
        if (ultimaCalificacion == null) {
            return false;
        }
        LocalDateTime ultimoCalculo = ultimoCalculoPorPar.get(clave);
        return ultimoCalculo == null || ultimaCalificacion.isAfter(ultimoCalculo);
    }
}
