package co.edu.uco.sigra.common.openapi;

/**
 * Ejemplos de cuerpo de error reutilizables en {@code @ExampleObject(value = ...)}. Son los errores
 * transversales (validación, 401, 403, parámetro inválido): los mensajes coinciden con los que
 * devuelven {@code GlobalExceptionHandler} y los handlers de seguridad. Los ejemplos propios de un
 * módulo (por ejemplo, «profesor no encontrado») se definen como constantes en su controlador,
 * usando {@link #TIMESTAMP_EJEMPLO}.
 *
 * <p>Deben ser constantes de compilación: las anotaciones no aceptan valores calculados.
 */
public final class EjemplosError {

    private EjemplosError() {
    }

    public static final String TIMESTAMP_EJEMPLO = "2026-10-14T12:00:00-05:00";

    /** 400 por validación del cuerpo: incluye {@code detalles}. */
    public static final String VALIDACION_400 =
            "{\"timestamp\":\"" + TIMESTAMP_EJEMPLO + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"Los datos de entrada no cumplen con las validaciones requeridas\","
                    + "\"detalles\":[\"numeroDocumento: El número de documento debe tener entre 6 y 10 dígitos numéricos\"]}";

    /** 400 por un parámetro de ruta o de consulta con formato inválido (UUID o enum). */
    public static final String PARAMETRO_INVALIDO_400 =
            "{\"timestamp\":\"" + TIMESTAMP_EJEMPLO + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"El valor del parámetro 'id' no es válido\"}";

    /** 401: falta el token, es inválido o expiró. */
    public static final String NO_AUTENTICADO_401 =
            "{\"timestamp\":\"" + TIMESTAMP_EJEMPLO + "\",\"status\":401,\"error\":\"UNAUTHORIZED\","
                    + "\"mensaje\":\"Debe iniciar sesión para acceder a este recurso\"}";

    /** 403: el rol del usuario no permite la operación. */
    public static final String PROHIBIDO_403 =
            "{\"timestamp\":\"" + TIMESTAMP_EJEMPLO + "\",\"status\":403,\"error\":\"FORBIDDEN\","
                    + "\"mensaje\":\"No tiene permisos suficientes para realizar esta acción\"}";
}