package co.edu.uco.sigra.calificaciones.exception;

import co.edu.uco.sigra.common.exception.ErrorResponseBuilder;
import co.edu.uco.sigra.estudiantes.exception.MatriculaNoEncontradaException;
import co.edu.uco.sigra.evaluaciones.exception.EvaluacionNoEncontradaException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Excepciones de negocio del módulo de calificaciones (RF-12 / RF-13). Solo aplica a los controladores
 * de este paquete. El 401, el 403 (AccessDeniedException), las validaciones de DTO y los errores
 * inesperados los resuelve el manejador global, como en los demás módulos.
 */
@RestControllerAdvice(basePackages = "co.edu.uco.sigra.calificaciones")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CalificacionExceptionHandler {

    @ExceptionHandler(CalificacionReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> manejarReglaNegocio(CalificacionReglaNegocioException ex) {
        return ErrorResponseBuilder.build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null);
    }

    @ExceptionHandler({CalificacionNoEncontradaException.class, EvaluacionNoEncontradaException.class,
            MatriculaNoEncontradaException.class})
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RuntimeException ex) {
        return ErrorResponseBuilder.build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(CalificacionDuplicadaException.class)
    public ResponseEntity<Map<String, Object>> manejarDuplicada(CalificacionDuplicadaException ex) {
        return ErrorResponseBuilder.build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarIntegridad(DataIntegrityViolationException ex) {
        return ErrorResponseBuilder.build(HttpStatus.CONFLICT, "CONFLICT", CalificacionDuplicadaException.MENSAJE, null);
    }
}