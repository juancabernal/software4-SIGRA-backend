package co.edu.uco.sigra.estadisticas.service;

import java.util.List;
import java.util.UUID;

/**
 * Puerto hacia las calificaciones que alimentan el nivel de logro (RF-14 / RF-15).
 * <p>
 * Lo implementarán los módulos {@code evaluaciones} y {@code calificaciones} cuando existan: deben
 * devolver todas las calificaciones registradas en las evaluaciones del RA para ese semestre, cada una
 * con la nota y el porcentaje de su evaluación. Mientras tanto lo cubre
 * {@code FuenteCalificacionesPruebaImpl} con la tabla de prueba {@code registro_calificacion_prueba}.
 * <p>
 * Las calificaciones de estudiantes desvinculados se incluyen igual: se conservan (RF-19) y siguen
 * contando para las estadísticas del semestre en que se registraron.
 */
public interface FuenteCalificaciones {

    List<CalificacionPonderada> listarCalificaciones(UUID resultadoAprendizajeId, UUID semestreId);
}
