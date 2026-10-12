package co.edu.uco.sigra.mejoras.exception;

import co.edu.uco.sigra.common.exception.ErrorResponseBuilder;
import co.edu.uco.sigra.estadisticas.exception.ConclusionNoEncontradaException;
import co.edu.uco.sigra.estadisticas.exception.TransicionEstadoInvalidaException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Excepciones del módulo de acciones de mejora (RF-17). Mismo estilo que
 * {@code EstadisticasExceptionHandler}: solo aplica a los controladores de este paquete y tiene la
 * máxima prioridad para que el manejador global no las convierta en 500.
 */
@RestControllerAdvice(basePackages = "co.edu.uco.sigra.mejoras")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MejorasExceptionHandler {

    @ExceptionHandler({AccionMejoraNoEncontradaException.class, ConclusionNoEncontradaException.class})
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RuntimeException ex) {
        return ErrorResponseBuilder.build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(ConclusionNoVigenteException.class)
    public ResponseEntity<Map<String, Object>> manejarConclusionNoVigente(ConclusionNoVigenteException ex) {
        return ErrorResponseBuilder.build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null);
    }

    /** Por si escapa desde marcarAtendida() en algún camino no cubierto por el chequeo explícito. */
    @ExceptionHandler(TransicionEstadoInvalidaException.class)
    public ResponseEntity<Map<String, Object>> manejarTransicionInvalida(TransicionEstadoInvalidaException ex) {
        return ErrorResponseBuilder.build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null);
    }

    /** 401 del control por rol compartido con asignaturas y estadísticas (temporal hasta RF-05). */
    @ExceptionHandler(AutenticacionRequeridaException.class)
    public ResponseEntity<Map<String, Object>> manejarNoAutenticado(AutenticacionRequeridaException ex) {
        ResponseEntity<Map<String, Object>> respuesta =
                ErrorResponseBuilder.build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), null);
        return ResponseEntity.status(respuesta.getStatusCode())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(respuesta.getBody());
    }

    /** 403 del control por rol y del alcance del profesor (temporal hasta RF-05). */
    @ExceptionHandler(PermisoInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> manejarSinPermiso(PermisoInsuficienteException ex) {
        return ErrorResponseBuilder.build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), null);
    }
}
