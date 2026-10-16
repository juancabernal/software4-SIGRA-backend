package co.edu.uco.sigra.evaluaciones.exception;

import co.edu.uco.sigra.asignaturas.exception.AutenticacionRequeridaException;
import co.edu.uco.sigra.asignaturas.exception.PermisoInsuficienteException;
import co.edu.uco.sigra.common.exception.ErrorResponseBuilder;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeNoEncontradoException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Excepciones del módulo de evaluaciones (RF-10 / RF-11).
 * <p>
 * - Solo aplica a los controladores de este paquete (basePackages) y tiene la máxima prioridad, como el
 *   de resultados de aprendizaje.
 * - Los rechazos 401/403 de autenticación y autorización provienen de Spring Security o del
 *   control de alcance docente del servicio.
 * - Las validaciones de DTO, JSON mal formado y errores inesperados los resuelve el manejador global.
 */
@RestControllerAdvice(basePackages = "co.edu.uco.sigra.evaluaciones")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EvaluacionExceptionHandler {

    @ExceptionHandler(EvaluacionReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> manejarReglaNegocio(EvaluacionReglaNegocioException ex) {
        return ErrorResponseBuilder.build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null);
    }

    @ExceptionHandler({EvaluacionNoEncontradaException.class, ResultadoAprendizajeNoEncontradoException.class,
            SemestreNoEncontradoException.class})
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RuntimeException ex) {
        return ErrorResponseBuilder.build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    @ExceptionHandler(NombreEvaluacionDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> manejarNombreDuplicado(NombreEvaluacionDuplicadoException ex) {
        return ErrorResponseBuilder.build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null);
    }

    /** Respaldo ante una carrera: dos peticiones simultáneas con el mismo nombre (uk_evaluacion_ra_semestre_nombre). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarIntegridad(DataIntegrityViolationException ex) {
        return ErrorResponseBuilder.build(HttpStatus.CONFLICT, "CONFLICT",
                NombreEvaluacionDuplicadoException.MENSAJE, null);
    }

    @ExceptionHandler(AutenticacionRequeridaException.class)
    public ResponseEntity<Map<String, Object>> manejarNoAutenticado(AutenticacionRequeridaException ex) {
        ResponseEntity<Map<String, Object>> respuesta =
                ErrorResponseBuilder.build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), null);
        return ResponseEntity.status(respuesta.getStatusCode())
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(respuesta.getBody());
    }

    @ExceptionHandler(PermisoInsuficienteException.class)
    public ResponseEntity<Map<String, Object>> manejarSinPermiso(PermisoInsuficienteException ex) {
        return ErrorResponseBuilder.build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), null);
    }
}
