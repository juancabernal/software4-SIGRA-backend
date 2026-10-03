package co.edu.uco.sigra.semestres.exception;

import co.edu.uco.sigra.semestres.controller.SemestreController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manejo de errores exclusivo del módulo de semestres.
 * - Solo aplica a SemestreController (assignableTypes), así no afecta a otros módulos.
 * - Tiene la máxima prioridad para que el manejador genérico de otros módulos no convierta
 *   estas excepciones en 500.
 * - Usa la misma estructura JSON que el resto del equipo (timestamp, status, error, mensaje, detalles).
 * - Nunca expone trazas ni mensajes técnicos (RNF-17).
 */
@RestControllerAdvice(assignableTypes = SemestreController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SemestreExceptionHandler {

    private ResponseEntity<Map<String, Object>> construirRespuestaError(HttpStatus status, String mensaje, Object detalles) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.name());
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
        return construirRespuestaError(HttpStatus.BAD_REQUEST,
                "Los datos de entrada no cumplen con las validaciones requeridas", errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return construirRespuestaError(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es válido. Las fechas deben tener formato AAAA-MM-DD", null);
    }

    @ExceptionHandler(SemestreReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> manejarReglaNegocio(SemestreReglaNegocioException ex) {
        return construirRespuestaError(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(SemestreNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(SemestreNoEncontradoException ex) {
        return construirRespuestaError(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler({SemestreDuplicadoException.class, SemestreSolapadoException.class})
    public ResponseEntity<Map<String, Object>> manejarConflicto(RuntimeException ex) {
        return construirRespuestaError(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    /** Respaldo ante una carrera: dos peticiones simultáneas con el mismo código. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarIntegridad(DataIntegrityViolationException ex) {
        return construirRespuestaError(HttpStatus.CONFLICT,
                "La operación entra en conflicto con un semestre existente", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarErrorGenerico(Exception ex) {
        return construirRespuestaError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado en el servidor", null);
    }
}
