package co.edu.uco.sigra.asignaturas.exception;

import co.edu.uco.sigra.asignaturas.controller.AsignacionDocenteController;
import co.edu.uco.sigra.common.exception.ConstructorRespuestaError;
import co.edu.uco.sigra.profesores.exception.ProfesorNoEncontradoException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Manejo de errores exclusivo de AsignacionDocenteController.
 * Declara solo las excepciones de su dominio; la validación, el JSON ilegible y los parámetros
 * inválidos los resuelven los manejadores transversales de common/exception.
 */
@RestControllerAdvice(assignableTypes = AsignacionDocenteController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AsignacionDocenteExceptionHandler {

    @ExceptionHandler({
            ProfesorNoEncontradoException.class,
            AsignaturaNoEncontradaException.class,
            AsignacionDocenteNoEncontradaException.class
    })
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RuntimeException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
