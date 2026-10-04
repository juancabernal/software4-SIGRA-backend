package co.edu.uco.sigra.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Manejo transversal de los errores de petición mal formada que ningún advice de módulo declara.
 *
 * <p>Cierra el hueco por el que seis familias de excepción terminaban en el método para
 * {@code Exception} de {@link GlobalExceptionHandler} y salían como <strong>500</strong> cuando el
 * código correcto era 415, 406, 400 o 409. El contrato del cuerpo, el mapa de precedencias y la
 * receta para enchufar un advice de módulo nuevo están en el {@code package-info.java} de este
 * paquete.
 *
 * <h2>Por qué {@code @Order(0)} y no otro valor</h2>
 *
 * <p>{@code 0} es la única posición que sirve, y queda reservada para esta clase (§5 del
 * {@code package-info}). Gana contra {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}, de
 * modo que se evalúa antes que el respaldo genérico del advice global y puede mapear cada excepción
 * a su código real. Y pierde contra {@link org.springframework.core.Ordered#HIGHEST_PRECEDENCE}, de
 * modo que los advices de módulo conservan intacta la prioridad que sus dueños les dieron: donde
 * {@code asignaturas}, {@code semestres} o {@code resultadosaprendizaje} ya declaran una excepción
 * —{@link DataIntegrityViolationException}, por ejemplo— siguen ganando ellos y su respuesta no
 * cambia.
 *
 * <h2>La restricción que hace seguro agregar este advice</h2>
 *
 * <p>Esta clase declara <strong>únicamente</strong> tipos de excepción que ningún advice existente
 * declara, y <strong>nunca</strong> {@code Exception} ni {@code Throwable}. No es una preferencia de
 * estilo: con el orden {@code 0}, un método para {@code Exception} le ganaría a
 * {@code ProfesorExceptionHandler} —que está en {@code LOWEST_PRECEDENCE}— y convertiría sus 404 y
 * 409 en 500. Sería romper el módulo de un compañero para arreglar un contrato. Si alguien necesita
 * un respaldo para lo inesperado, ya existe: es el de {@link GlobalExceptionHandler}.
 *
 * <h2>Lo que llega al cliente y lo que se queda en el log</h2>
 *
 * <p>Ningún mensaje de esta clase interpola {@code ex.getMessage()}. Los únicos datos de la
 * excepción que llegan al cuerpo son los que el propio cliente envió o dejó de enviar —el nombre del
 * parámetro que falta, el tipo de contenido que no se acepta—, nunca algo del servidor. La causa
 * técnica de {@link DataIntegrityViolationException}, que trae el nombre de la restricción, la tabla
 * y la columna, se registra con {@code log.error} y no sale en la respuesta (RNF-17).
 */
@Slf4j
@Order(0)
@RestControllerAdvice
public class ManejadorErroresTransversal {

    /**
     * 415 cuando el {@code Content-Type} de la petición no es uno que el endpoint pueda leer.
     *
     * <p>Los tipos soportados van en {@code detalles} porque son parte del contrato público del
     * endpoint: le dicen al cliente con qué reintentar, y no revelan nada de la implementación.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> manejarTipoDeContenidoNoSoportado(
            HttpMediaTypeNotSupportedException ex) {
        List<String> soportados = ex.getSupportedMediaTypes().stream()
                .map(MediaType::toString)
                .toList();
        return ConstructorRespuestaError.construir(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "El tipo de contenido de la petición no es soportado por este recurso",
                soportados);
    }

    /**
     * 406 cuando el encabezado {@code Accept} de la petición no se puede satisfacer con ninguna de
     * las representaciones que el endpoint produce.
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Map<String, Object>> manejarRepresentacionNoAceptable(
            HttpMediaTypeNotAcceptableException ex) {
        List<String> producidos = ex.getSupportedMediaTypes().stream()
                .map(MediaType::toString)
                .toList();
        return ConstructorRespuestaError.construir(HttpStatus.NOT_ACCEPTABLE,
                "Este recurso no puede entregar una representación compatible con el encabezado "
                        + "Accept de la petición",
                producidos);
    }

    /**
     * 400 cuando falta un parámetro de consulta obligatorio. El mensaje nombra el parámetro ausente,
     * que es un dato de la petición del cliente y no del servidor.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> manejarParametroObligatorioAusente(
            MissingServletRequestParameterException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "' en la petición");
    }

    /**
     * 400 cuando falta una parte obligatoria en una petición multiparte, por ejemplo el archivo de
     * una carga. El mensaje nombra la parte ausente.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> manejarParteObligatoriaAusente(
            MissingServletRequestPartException ex) {
        return ConstructorRespuestaError.construir(HttpStatus.BAD_REQUEST,
                "Falta la parte obligatoria '" + ex.getRequestPartName() + "' en la petición "
                        + "multiparte");
    }

    /**
     * 400 cuando un parámetro de consulta o una variable de ruta incumple una restricción de
     * validación declarada. Es la ruta que se abre al anotar un controlador con {@code @Validated} o
     * al poner validación Jakarta en una entidad; hoy ningún componente del proyecto lo hace, así
     * que este manejador es defensivo — está para que quien agregue cualquiera de las dos cosas no
     * descubra el 500 en producción.
     *
     * <p>De cada restricción incumplida se publica <strong>solo el último tramo</strong> de la ruta
     * de la propiedad. La ruta completa incluye el nombre del método Java que la declara
     * ({@code buscarPorNombre.nombre}), y ese nombre es implementación interna que RNF-17 no permite
     * filtrar; el último tramo es el nombre del parámetro, que es justo lo que el cliente necesita.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarRestriccionDeValidacionIncumplida(
            ConstraintViolationException ex) {
        List<String> detalles = ex.getConstraintViolations().stream()
                .map(violacion -> nombreDelParametro(violacion) + ": " + violacion.getMessage())
                .sorted(Comparator.naturalOrder())
                .toList();
        return ConstructorRespuestaError.construir(HttpStatus.BAD_REQUEST,
                "Los parámetros de la petición no cumplen con las validaciones requeridas",
                detalles);
    }

    /**
     * 409 cuando la operación choca con una restricción de unicidad o de integridad de la base de
     * datos.
     *
     * <p>Los módulos que ya declaran esta excepción con mayor precedencia ({@code asignaturas},
     * {@code semestres}, {@code resultadosaprendizaje}) siguen respondiendo con su propio mensaje de
     * negocio; este manejador cubre los demás, que hoy caen al 500 del advice global.
     *
     * <p>La causa raíz se registra completa porque es lo que permite diagnosticar <em>cuál</em>
     * restricción se violó, y no sale en la respuesta porque el nombre de la restricción, de la
     * tabla y de la columna es implementación interna (RNF-17).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarViolacionDeIntegridad(
            DataIntegrityViolationException ex) {
        log.error("Violación de integridad de datos en una operación de la API", ex);
        return ConstructorRespuestaError.construir(HttpStatus.CONFLICT,
                "La operación entra en conflicto con información ya registrada en el sistema");
    }

    /**
     * Último tramo de la ruta de la propiedad de una restricción incumplida: el nombre del parámetro,
     * sin el nombre del método que lo declara.
     */
    private static String nombreDelParametro(ConstraintViolation<?> violacion) {
        String ultimoTramo = null;
        for (Path.Node nodo : violacion.getPropertyPath()) {
            ultimoTramo = nodo.getName();
        }
        return ultimoTramo != null ? ultimoTramo : "parámetro";
    }
}
