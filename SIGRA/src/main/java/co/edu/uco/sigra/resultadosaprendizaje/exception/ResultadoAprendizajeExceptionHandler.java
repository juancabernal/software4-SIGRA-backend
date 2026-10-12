package co.edu.uco.sigra.resultadosaprendizaje.exception;

import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.common.exception.ErrorResponseBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Excepciones de negocio del módulo de resultados de aprendizaje (RF-06).
 * <p>
 * - Solo aplica a los controladores de este paquete (basePackages).
 * - Tiene la máxima prioridad para que el manejador global no las convierta en 500
 *   (el global atrapa Exception y, sin prioridad, podría evaluarse primero).
 * - Solo maneja excepciones de negocio. Las genéricas (validación de DTOs, 401, 403, 404 de ruta, 500)
 *   las resuelve common.exception.GlobalExceptionHandler.
 */
@RestControllerAdvice(basePackages = "co.edu.uco.sigra.resultadosaprendizaje")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ResultadoAprendizajeExceptionHandler {

    @ExceptionHandler(ResultadoAprendizajeReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> manejarReglaNegocio(ResultadoAprendizajeReglaNegocioException ex) {
        return ErrorResponseBuilder.build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null);
    }

    /** Incluye la asignatura inexistente: su manejador solo aplica al controlador de asignaturas. */
    @ExceptionHandler({ResultadoAprendizajeNoEncontradoException.class, AsignaturaNoEncontradaException.class})
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RuntimeException ex) {
        return ErrorResponseBuilder.build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(CodigoResultadoAprendizajeDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> manejarCodigoDuplicado(CodigoResultadoAprendizajeDuplicadoException ex) {
        return ErrorResponseBuilder.build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null);
    }

    /** Respaldo ante una carrera: dos peticiones simultáneas registran el mismo código (uk_ra_asignatura_codigo). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarIntegridad(DataIntegrityViolationException ex) {
        return ErrorResponseBuilder.build(HttpStatus.CONFLICT, "CONFLICT",
                "Ya existe un resultado de aprendizaje con ese código en esta asignatura.", null);
    }
}
