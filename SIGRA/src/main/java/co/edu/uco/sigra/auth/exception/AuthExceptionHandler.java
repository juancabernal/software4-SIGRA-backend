package co.edu.uco.sigra.auth.exception;

import co.edu.uco.sigra.common.exception.ErrorResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> manejarCredencialesInvalidas(CredencialesInvalidasException ex) {
        return ErrorResponseBuilder.build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), null);
    }

    @ExceptionHandler(UsuarioBloqueadoException.class)
    public ResponseEntity<Map<String, Object>> manejarUsuarioBloqueado(UsuarioBloqueadoException ex) {
        return ErrorResponseBuilder.build(HttpStatus.LOCKED, "LOCKED", ex.getMessage(), null);
    }
}
