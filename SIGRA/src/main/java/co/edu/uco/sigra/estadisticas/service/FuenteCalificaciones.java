package co.edu.uco.sigra.estadisticas.service;

import java.util.List;
import java.util.UUID;

/**
 * Puerto hacia las calificaciones que alimentan el nivel de logro (RF-14 / RF-15).
 * <p>
 * Debe devolver todas las calificaciones que cuentan para un (RA, semestre), cada una con la nota y el
 * porcentaje de su evaluación. La implementación real es {@code FuenteCalificacionesImpl}: solo evaluaciones
 * ACTIVA del RA y semestre pedidos, y solo matrículas ACTIVA (las DESVINCULADA no cuentan).
 */
public interface FuenteCalificaciones {

    List<CalificacionPonderada> listarCalificaciones(UUID resultadoAprendizajeId, UUID semestreId);
}
