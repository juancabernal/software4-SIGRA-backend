package co.edu.uco.sigra.estudiantes.exception;

import co.edu.uco.sigra.common.exception.ConstructorRespuestaError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

/**
 * Excepciones de negocio del módulo de estudiantes (RF-09a).
 * <p>
 * - Solo aplica a los controladores de este paquete (basePackages).
 * - Tiene la máxima prioridad para que el manejador global no las convierta en 500
 *   (el global atrapa Exception y, sin prioridad, el desempate con este advice no es determinista).
 * - Solo maneja excepciones de negocio y la validación de los DTOs de este módulo. Las demás
 *   genéricas (401, 403, 404 de ruta, 500) las resuelve common.exception.GlobalExceptionHandler.
 */
@RestControllerAdvice(basePackages = "co.edu.uco.sigra.estudiantes")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ManejadorErroresEstudiantes {

    @ExceptionHandler({EstudianteNoEncontradoException.class, ReferenciaNoEncontradaException.class,
            MatriculaNoEncontradaException.class})
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RuntimeException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({EstudianteYaRegistradoException.class, CorreoEstudianteDuplicadoException.class,
            MatriculaDuplicadaException.class, MatriculaYaInactivaException.class})
    public ResponseEntity<Map<String, Object>> manejarDuplicado(RuntimeException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({DocumentoNoModificableException.class, ReglaDeMatriculaException.class})
    public ResponseEntity<Map<String, Object>> manejarReglaDeNegocio(RuntimeException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacionesDTO(MethodArgumentNotValidException ex) {
        List<String> errores = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        return ConstructorRespuestaError.construir(HttpStatus.BAD_REQUEST,
                "Los datos de entrada no cumplen con las validaciones requeridas", errores);
    }
}
