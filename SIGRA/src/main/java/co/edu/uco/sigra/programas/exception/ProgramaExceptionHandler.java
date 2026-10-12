package co.edu.uco.sigra.programas.exception;

import co.edu.uco.sigra.common.exception.ConstructorRespuestaError;
import co.edu.uco.sigra.programas.controller.ProgramaAcademicoController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Manejo de errores exclusivo del módulo de programas académicos.
 * Declara solo excepciones de su dominio; la validación, el JSON ilegible y los parámetros inválidos
 * los resuelven los manejadores transversales de common/exception.
 */
@RestControllerAdvice(assignableTypes = ProgramaAcademicoController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProgramaExceptionHandler {

    @ExceptionHandler(ProgramaAcademicoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(RuntimeException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            NombreProgramaDuplicadoException.class,
            CodigoProgramaDuplicadoException.class,
            ProgramaConAsignaturasActivasException.class
    })
    public ResponseEntity<Map<String, Object>> manejarConflicto(RuntimeException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.CONFLICT, ex.getMessage());
    }
}
