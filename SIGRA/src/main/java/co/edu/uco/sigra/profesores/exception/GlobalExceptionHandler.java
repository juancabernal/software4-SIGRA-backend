package co.edu.uco.sigra.profesores.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Helper para construir la estructura uniforme del JSON de error
    private ResponseEntity<Map<String, Object>> construirRespuestaError(HttpStatus status, String error, String mensaje, Object detalles) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("mensaje", mensaje);
        if (detalles != null) {
            body.put("detalles", detalles);
        }
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacionesDTO(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();

        return construirRespuestaError(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                "Los datos de entrada no cumplen con las validaciones requeridas",
                errores
        );
    }

    @ExceptionHandler({TipoDocumentoNoEncontradoException.class, ProfesorNoEncontradoException.class})
    public ResponseEntity<Map<String, Object>> manejarRecursoNoEncontrado(RuntimeException ex) {
        return construirRespuestaError(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler({CorreoDuplicadoException.class, DocumentoDuplicadoException.class})
    public ResponseEntity<Map<String, Object>> manejarRecursoDuplicado(RuntimeException ex) {
        return construirRespuestaError(
                HttpStatus.CONFLICT,
                "CONFLICT",
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler({CampoNoModificableException.class, ProfesorConAsignacionesActivasException.class})
    public ResponseEntity<Map<String, Object>> manejarReglasDeNegocio(RuntimeException ex) {
        return construirRespuestaError(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                ex.getMessage(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarErrorGenerico(Exception ex) {
        return construirRespuestaError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Ocurrió un error inesperado en el servidor",
                ex.getMessage()
        );
    }
}
