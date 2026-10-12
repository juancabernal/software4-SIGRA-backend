package co.edu.uco.sigra.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> manejarAccesoDenegado(AccessDeniedException ex) {
        return ErrorResponseBuilder.build(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "No tiene permisos suficientes para realizar esta acción", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> manejarNoAutenticado(AuthenticationException ex) {
        return ErrorResponseBuilder.build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Debe iniciar sesión para acceder a este recurso", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacionesDTO(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        return ErrorResponseBuilder.build(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "Los datos de entrada no cumplen con las validaciones requeridas", errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return ErrorResponseBuilder.build(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "El cuerpo de la petición no es válido o está mal formado", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> manejarTipoDeContenidoNoSoportado(HttpMediaTypeNotSupportedException ex) {
        return ErrorResponseBuilder.build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "El tipo de contenido de la petición no es compatible. Use application/json", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> manejarTipoDeParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ErrorResponseBuilder.build(HttpStatus.BAD_REQUEST, "BAD_REQUEST",
                "El valor del parámetro '" + ex.getName() + "' no es válido", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        return ErrorResponseBuilder.build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex.getMessage(), null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> manejarRutaNoEncontrada(NoResourceFoundException ex) {
        return ErrorResponseBuilder.build(HttpStatus.NOT_FOUND, "NOT_FOUND", "El recurso solicitado no existe", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarErrorGenerico(Exception ex) {
        // El detalle técnico (SQL, Hibernate, trazas) va solo al log del servidor, nunca a la respuesta (RNF-17).
        log.error("Error inesperado no controlado", ex);
        return ErrorResponseBuilder.build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "Ocurrió un error inesperado en el servidor", null);
    }
}
