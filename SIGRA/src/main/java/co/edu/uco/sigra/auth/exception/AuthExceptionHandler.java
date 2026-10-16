package co.edu.uco.sigra.auth.exception;

import co.edu.uco.sigra.common.exception.ConstructorRespuestaError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> manejarCredencialesInvalidas(CredencialesInvalidasException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(UsuarioBloqueadoException.class)
    public ResponseEntity<Map<String, Object>> manejarUsuarioBloqueado(UsuarioBloqueadoException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.LOCKED, ex.getMessage());
    }
}
