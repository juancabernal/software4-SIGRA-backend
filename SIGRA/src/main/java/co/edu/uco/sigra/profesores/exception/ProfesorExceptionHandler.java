package co.edu.uco.sigra.profesores.exception;

import co.edu.uco.sigra.common.exception.ErrorResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ProfesorExceptionHandler {
    @ExceptionHandler({TipoDocumentoNoEncontradoException.class, ProfesorNoEncontradoException.class})
    public ResponseEntity<Map<String, Object>> manejarRecursoNoEncontrado(RuntimeException ex) {
        return ErrorResponseBuilder.build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler({CorreoDuplicadoException.class, DocumentoDuplicadoException.class})
    public ResponseEntity<Map<String, Object>> manejarRecursoDuplicado(RuntimeException ex) {
        return ErrorResponseBuilder.build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null);
    }

    @ExceptionHandler({CampoNoModificableException.class, ProfesorConAsignacionesActivasException.class})
    public ResponseEntity<Map<String, Object>> manejarReglasDeNegocio(RuntimeException ex) {
        return ErrorResponseBuilder.build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null);
    }
}
