package co.edu.uco.sigra.semestres.exception;

import co.edu.uco.sigra.common.exception.ErrorResponseBuilder;
import co.edu.uco.sigra.semestres.controller.SemestreController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Manejo de errores propio del módulo de semestres (convención del equipo: {Modulo}ExceptionHandler).
 * - Solo aplica a SemestreController (assignableTypes) y solo maneja errores propios del módulo.
 * - Validaciones de DTO (400), permisos (403), autenticación (401), rutas inexistentes (404)
 *   y errores inesperados (500) los resuelve common/exception/GlobalExceptionHandler.
 *   Por eso aquí NO se declara un manejador de Exception: si existiera, al tener máxima
 *   prioridad capturaría también AccessDeniedException y la convertiría en 500 en lugar de 403.
 * - Usa ErrorResponseBuilder para que el formato de error sea idéntico en toda la API.
 */
@RestControllerAdvice(assignableTypes = SemestreController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SemestreExceptionHandler {

    @ExceptionHandler(SemestreReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> manejarReglaNegocio(SemestreReglaNegocioException ex) {
        return ErrorResponseBuilder.build(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(), ex.getMessage(), null);
    }

    @ExceptionHandler(SemestreNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(SemestreNoEncontradoException ex) {
        return ErrorResponseBuilder.build(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.name(), ex.getMessage(), null);
    }

    @ExceptionHandler({SemestreDuplicadoException.class, SemestreSolapadoException.class})
    public ResponseEntity<Map<String, Object>> manejarConflicto(RuntimeException ex) {
        return ErrorResponseBuilder.build(HttpStatus.CONFLICT, HttpStatus.CONFLICT.name(), ex.getMessage(), null);
    }

    /**
     * El global no maneja cuerpos ilegibles (terminarían en 500). En semestres el caso típico
     * es una fecha con formato incorrecto, por eso el mensaje orienta sobre AAAA-MM-DD.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return ErrorResponseBuilder.build(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.name(),
                "El cuerpo de la petición no es válido. Las fechas deben tener formato AAAA-MM-DD", null);
    }

    /** Respaldo ante una carrera: dos peticiones simultáneas con el mismo código. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarIntegridad(DataIntegrityViolationException ex) {
        return ErrorResponseBuilder.build(HttpStatus.CONFLICT, HttpStatus.CONFLICT.name(),
                "La operación entra en conflicto con un semestre existente", null);
    }
}