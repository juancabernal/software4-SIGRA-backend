package co.edu.uco.sigra.asignaturas.exception;

import co.edu.uco.sigra.asignaturas.controller.AsignaturaController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manejo de errores exclusivo del módulo de asignaturas.
 * - Atiende solo los errores propios de asignaturas y los de petición mal formada (validación,
 *   JSON ilegible, parámetro con formato inválido, conflicto de base de datos).
 * - Los errores inesperados y el 403 por permisos los atiende el manejador global de common/exception,
 *   por eso aquí no hay un método para Exception.class.
 * - Solo aplica a AsignaturaController (assignableTypes), así no afecta a otros módulos.
 * - Tiene la máxima prioridad para que el manejador global no reemplace estas respuestas.
 * - Usa la misma estructura JSON que el resto del equipo (timestamp, status, error, mensaje, detalles).
 * - Nunca expone trazas ni mensajes técnicos (RNF-17).
 */
@RestControllerAdvice(assignableTypes = AsignaturaController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AsignaturaExceptionHandler {

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
        return construirRespuestaError(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido.", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> manejarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return construirRespuestaError(HttpStatus.BAD_REQUEST,
                "El valor del parámetro '" + ex.getName() + "' no es válido.", null);
    }

    @ExceptionHandler({RangoRaInvalidoException.class, ProgramaInactivoException.class, FiltroInvalidoException.class})
    public ResponseEntity<Map<String, Object>> manejarReglaNegocio(RuntimeException ex) {
        return construirRespuestaError(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler({AsignaturaNoEncontradaException.class, ProgramaNoEncontradoException.class})
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RuntimeException ex) {
        return construirRespuestaError(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler({CodigoAsignaturaDuplicadoException.class, TransicionEstadoInvalidaException.class})
    public ResponseEntity<Map<String, Object>> manejarConflicto(RuntimeException ex) {
        return construirRespuestaError(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    /** Respaldo ante una carrera: dos peticiones simultáneas con el mismo código. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarIntegridad(DataIntegrityViolationException ex) {
        return construirRespuestaError(HttpStatus.CONFLICT,
                "La operación entra en conflicto con una asignatura existente", null);
    }
}
