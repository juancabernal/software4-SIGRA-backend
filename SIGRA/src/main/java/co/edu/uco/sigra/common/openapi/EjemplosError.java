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

    /** 400 por las validaciones del cuerpo de login. */
    public static final String VALIDACION_LOGIN_400 =
            "{\"timestamp\":\"" + TIMESTAMP_EJEMPLO + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"Los datos de entrada no cumplen con las validaciones requeridas\","
                    + "\"detalles\":[\"correoInstitucional: El correo debe tener formato usuario@uco.net.co\"]}";

    /** 401 por credenciales inválidas o usuario inactivo en el login. */
    public static final String CREDENCIALES_INVALIDAS_401 =
            "{\"timestamp\":\"" + TIMESTAMP_EJEMPLO + "\",\"status\":401,\"error\":\"UNAUTHORIZED\","
                    + "\"mensaje\":\"Correo o contraseña incorrectos\"}";

    /** 413 cuando el filtro de tamaño rechaza el cuerpo real del login. */
    public static final String PAYLOAD_TOO_LARGE_413 =
            "{\"timestamp\":\"" + TIMESTAMP_EJEMPLO + "\",\"status\":413,\"error\":\"PAYLOAD_TOO_LARGE\","
                    + "\"mensaje\":\"El cuerpo de la petición supera el tamaño permitido\"}";

    /** 423 durante el bloqueo temporal de una cuenta. */
    public static final String USUARIO_BLOQUEADO_423 =
            "{\"timestamp\":\"" + TIMESTAMP_EJEMPLO + "\",\"status\":423,\"error\":\"LOCKED\","
                    + "\"mensaje\":\"El usuario está temporalmente bloqueado. Intente nuevamente más tarde.\"}";

    /** 503 cuando la capa de autenticación no puede consultar la persistencia. */
    public static final String SERVICIO_NO_DISPONIBLE_503 =
            "{\"timestamp\":\"" + TIMESTAMP_EJEMPLO + "\",\"status\":503,\"error\":\"SERVICE_UNAVAILABLE\","
                    + "\"mensaje\":\"El servicio no está disponible temporalmente\"}";

    /** 500 para fallos inesperados sin detalles técnicos. */
    public static final String ERROR_INTERNO_500 =
            "{\"timestamp\":\"" + TIMESTAMP_EJEMPLO + "\",\"status\":500,\"error\":\"INTERNAL_SERVER_ERROR\","
                    + "\"mensaje\":\"Ocurrió un error inesperado en el servidor\"}";

    /** 400 por un tipo de entidad de auditoría que no pertenece al enum del contrato. */
    public static final String TIPO_ENTIDAD_INVALIDO_400 =
            "{\"timestamp\":\"" + TIMESTAMP_EJEMPLO + "\",\"status\":400,\"error\":\"BAD_REQUEST\","
                    + "\"mensaje\":\"El valor del parámetro 'tipoEntidad' no es válido\"}";
}
