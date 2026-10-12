package co.edu.uco.sigra.estudiantes.service.impl;

import co.edu.uco.sigra.estudiantes.service.CalificacionesDeMatricula;

import java.util.UUID;

/**
 * Implementación neutra de {@link CalificacionesDeMatricula} que se activa mientras el módulo
 * {@code calificaciones} no publique su propio bean (design.md, D4).
 * <p>
 * Esta clase no lleva anotaciones de Spring a propósito: la condición que decide si se registra
 * como bean ({@code @ConditionalOnMissingBean}) vive en el método {@code @Bean} que la instancia
 * en {@link co.edu.uco.sigra.estudiantes.config.MatriculaConfig}, no aquí. Ponerla directamente
 * sobre una clase escaneada por componentes no evalúa la condición de forma confiable y deja la
 * aplicación sin ningún bean de este tipo.
 */
public class CalificacionesDeMatriculaPorDefecto implements CalificacionesDeMatricula {

    @Override
    public long contarPorMatricula(UUID matriculaId) {
        return 0;
    }
}
