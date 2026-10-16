package co.edu.uco.sigra.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ErrorResponseBuilder {

    private ErrorResponseBuilder() {}

    public static ResponseEntity<Map<String, Object>> build(HttpStatus status, String error, String mensaje, Object detalles) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        // El argumento se conserva por compatibilidad de llamadas existentes, pero el contrato
        // nunca permite que status y error queden desalineados.
        body.put("error", status.name());
        body.put("mensaje", mensaje);
        if (detalles != null && (!(detalles instanceof java.util.Collection<?> collection) || !collection.isEmpty())) {
            body.put("detalles", detalles);
        }
        return ResponseEntity.status(status).body(body);
    }
}
