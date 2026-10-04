package co.edu.uco.sigra.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arma el cuerpo de las respuestas de error según el contrato documentado en
 * {@code package-info.java} de este paquete.
 *
 * <p>Tres cosas salen correctas por construcción, en lugar de depender de que quien llama se
 * acuerde:
 *
 * <ul>
 *   <li><strong>El orden de las claves es estable.</strong> El cuerpo se arma con un
 *       {@link LinkedHashMap} en el orden del contrato — {@code timestamp}, {@code status},
 *       {@code error}, {@code mensaje}, {@code detalles} — así que el JSON sale igual en cada
 *       petición y en cada módulo.</li>
 *   <li><strong>{@code error} se deriva de {@code status}.</strong> No se recibe como parámetro,
 *       por lo que no existe la posibilidad de responder un 404 con {@code error: "BAD_REQUEST"}.</li>
 *   <li><strong>{@code detalles} se omite cuando no hay nada que enumerar.</strong> Ni {@code null}
 *       ni una lista vacía llegan al cliente: la clave simplemente no aparece.</li>
 * </ul>
 *
 * <p>El {@code timestamp} usa {@link OffsetDateTime}, que lleva zona, para que el cliente pueda
 * interpretarlo sin suponer en qué zona corre el servidor.
 *
 * <p>Uso desde el advice de un módulo:
 *
 * <pre>{@code
 * return ConstructorRespuestaError.construir(HttpStatus.NOT_FOUND, "El profesor solicitado no existe");
 * }</pre>
 *
 * <p>Esta clase no reemplaza a {@link ErrorResponseBuilder}, que sigue en uso en los advices que ya
 * lo adoptaron. Es la implementación a la que deben apuntar los módulos nuevos; la consolidación de
 * las dos queda como deuda documentada.
 */
public final class ConstructorRespuestaError {

    private ConstructorRespuestaError() {
    }

    /**
     * Respuesta de error sin detalles puntuales: el cuerpo trae {@code timestamp}, {@code status},
     * {@code error} y {@code mensaje}, y la clave {@code detalles} no aparece.
     *
     * @param status  código HTTP de la respuesta; de él se deriva el campo {@code error}
     * @param mensaje explicación en español, dirigida a una persona, de qué estuvo mal
     */
    public static ResponseEntity<Map<String, Object>> construir(HttpStatus status, String mensaje) {
        return construir(status, mensaje, null);
    }

    /**
     * Respuesta de error con detalles puntuales.
     *
     * @param status   código HTTP de la respuesta; de él se deriva el campo {@code error}
     * @param mensaje  explicación en español, dirigida a una persona, de qué estuvo mal
     * @param detalles lista de errores puntuales, uno por elemento. Si es {@code null} o está
     *                 vacía, la clave {@code detalles} se omite del cuerpo
     */
    public static ResponseEntity<Map<String, Object>> construir(HttpStatus status, String mensaje,
                                                                List<String> detalles) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", OffsetDateTime.now());
        cuerpo.put("status", status.value());
        cuerpo.put("error", status.name());
        cuerpo.put("mensaje", mensaje);
        if (detalles != null && !detalles.isEmpty()) {
            cuerpo.put("detalles", List.copyOf(detalles));
        }
        return ResponseEntity.status(status).body(cuerpo);
    }
}
