package co.edu.uco.sigra.facultades.exception;

import co.edu.uco.sigra.common.exception.ConstructorRespuestaError;
import co.edu.uco.sigra.facultades.controller.FacultadController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Manejo de errores exclusivo del módulo de facultades. La validación, el JSON ilegible y los
 * parámetros inválidos los resuelven los manejadores transversales de common/exception.
 * FacultadNoEncontradaException y FacultadInactivaException, cuando se lanzan desde el módulo
 * de programas (al registrar un programa), las atiende ProgramaExceptionHandler.
 */
@RestControllerAdvice(assignableTypes = FacultadController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FacultadExceptionHandler {

    @ExceptionHandler(FacultadNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrada(RuntimeException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            NombreFacultadDuplicadoException.class,
            FacultadConProgramasActivosException.class
    })
    public ResponseEntity<Map<String, Object>> manejarConflicto(RuntimeException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.CONFLICT, ex.getMessage());
    }
}