package co.edu.uco.sigra.auditoria.exception;

import co.edu.uco.sigra.common.exception.ConstructorRespuestaError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "co.edu.uco.sigra.auditoria")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuditoriaExceptionHandler {

    @ExceptionHandler(RangoFechasInvalidoException.class)
    public ResponseEntity<Map<String, Object>> manejarRangoInvalido(RangoFechasInvalidoException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}