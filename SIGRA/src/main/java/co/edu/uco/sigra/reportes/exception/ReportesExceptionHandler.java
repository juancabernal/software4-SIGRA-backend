package co.edu.uco.sigra.reportes.exception;

import co.edu.uco.sigra.common.exception.ErrorResponseBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/** Errores del módulo de reportes; los genéricos (400, 500) los resuelve GlobalExceptionHandler. */
@RestControllerAdvice(basePackages = "co.edu.uco.sigra.reportes")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReportesExceptionHandler {

    @ExceptionHandler(ReporteRecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(ReporteRecursoNoEncontradoException ex) {
        return ErrorResponseBuilder.build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(PermisoReporteInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> manejarSinPermiso(PermisoReporteInsuficienteException ex) {
        return ErrorResponseBuilder.build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), null);
    }
}
