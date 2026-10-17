package co.edu.uco.sigra.estadisticas.exception;

import co.edu.uco.sigra.common.exception.ErrorResponseBuilder;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeNoEncontradoException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Excepciones del módulo de estadísticas: nivel de logro y estadísticas (RF-14, RF-15) y conclusiones
 * automáticas (RF-16).
 * <p>
 * - Solo aplica a los controladores de este paquete (basePackages).
 * - Tiene la máxima prioridad para que el manejador global no las convierta en 500.
 * - Incluye el 404 de resultado de aprendizaje: su manejador propio solo aplica al controlador de
 *   resultadosaprendizaje (mismo patrón que ResultadoAprendizajeExceptionHandler con AsignaturaNoEncontradaException).
 * - Los errores genéricos (validación de DTOs, JSON ilegible, 500) los resuelve
 *   common.exception.GlobalExceptionHandler.
 */
@RestControllerAdvice(basePackages = "co.edu.uco.sigra.estadisticas")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EstadisticasExceptionHandler {

    @ExceptionHandler({ResultadoAprendizajeNoEncontradoException.class, SemestreNoEncontradoException.class,
            ConclusionNoEncontradaException.class})
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RuntimeException ex) {
        return ErrorResponseBuilder.build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    /** 401 normalizado para compatibilidad con errores de autenticación del módulo. */
    @ExceptionHandler(AutenticacionRequeridaException.class)
    public ResponseEntity<Map<String, Object>> manejarNoAutenticado(AutenticacionRequeridaException ex) {
        ResponseEntity<Map<String, Object>> respuesta =
                ErrorResponseBuilder.build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), null);
        return ResponseEntity.status(respuesta.getStatusCode())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(respuesta.getBody());
    }

    /** 403 normalizado para rol insuficiente o alcance de recurso. */
    @ExceptionHandler(PermisoInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> manejarSinPermiso(PermisoInsuficienteException ex) {
        return ErrorResponseBuilder.build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), null);
    }
}
