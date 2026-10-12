package co.edu.uco.sigra.estudiantes.service.impl;

import co.edu.uco.sigra.estudiantes.service.CalificacionesDeMatricula;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implementación neutra de {@link CalificacionesDeMatricula} que se activa mientras el módulo
 * {@code calificaciones} no publique su propio bean (design.md, D4).
 * <p>
 * <strong>Advertencia explícita:</strong> mientras este bean esté activo, la confirmación
 * adicional al desvincular una matrícula <strong>nunca se exige en producción</strong>, porque
 * el conteo que devuelve es siempre {@code 0}. Eso es correcto hoy —no hay calificaciones reales
 * que proteger porque el módulo que las guarda todavía no existe—, pero que nadie confunda las
 * pruebas en verde que simulan el puerto con que esta advertencia ya le aparece a un usuario
 * real: a un usuario real no le va a aparecer hasta que el módulo {@code calificaciones}
 * publique su implementación y este bean se retire solo por {@code @ConditionalOnMissingBean}.
 */
@Service
@ConditionalOnMissingBean(CalificacionesDeMatricula.class)
public class CalificacionesDeMatriculaPorDefecto implements CalificacionesDeMatricula {

    @Override
    public long contarPorMatricula(UUID matriculaId) {
        return 0;
    }
}
