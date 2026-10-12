package co.edu.uco.sigra.common.exception;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Verifica, en una sola llamada, que una respuesta de error cumple el contrato documentado en el
 * {@code package-info.java} de este paquete.
 *
 * <p>Existe para que nadie tenga que copiar el bloque de aserciones del contrato en su módulo. La
 * forma del cuerpo de error es transversal: si cada integrante la verifica a mano en sus pruebas,
 * se verifica distinto en cada módulo y deja de ser un contrato.
 *
 * <h2>Cómo se usa desde el módulo propio</h2>
 *
 * <p>Tres líneas sobre un {@code @WebMvcTest} del controlador propio — se provoca el error, se
 * confirma el código HTTP y se verifica el contrato completo del cuerpo:
 *
 * <pre>{@code
 * MvcResult resultado = mockMvc.perform(post("/api/v1/estudiantes")
 *         .contentType(MediaType.TEXT_PLAIN).content("no es json"))
 *     .andExpect(status().isUnsupportedMediaType()).andReturn();
 * AsercionesContratoError.verificarContratoSinDetalles(resultado, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
 * }</pre>
 *
 * <p>Los métodos devuelven el cuerpo ya parseado, de modo que la prueba pueda seguir con lo que sea
 * propio de su caso:
 *
 * <pre>{@code
 * JsonNode cuerpo = AsercionesContratoError.verificarContratoConDetalles(resultado, HttpStatus.BAD_REQUEST);
 * assertThat(cuerpo.get("mensaje").stringValue()).contains("programaId");
 * }</pre>
 *
 * <h2>Qué verifica</h2>
 *
 * <p>{@link #verificarContratoDeError(MvcResult, HttpStatus)} es la comprobación base, y cubre lo
 * que vale para cualquier respuesta de error:
 *
 * <ul>
 *   <li>El código HTTP de la respuesta es el esperado y el cuerpo es un objeto JSON.</li>
 *   <li>Están las cuatro claves obligatorias — {@code timestamp}, {@code status}, {@code error} y
 *       {@code mensaje} — y no hay ninguna clave fuera del contrato.</li>
 *   <li>{@code status} coincide con el código HTTP de la respuesta y {@code error} con su nombre:
 *       un 404 con {@code error: "BAD_REQUEST"} se detecta aquí.</li>
 *   <li>{@code mensaje} trae texto, no una cadena vacía.</li>
 *   <li><strong>El cuerpo serializado completo no filtra detalle técnico</strong> (RNF-17): ni el
 *       texto {@code Exception}, ni nombres de clase Java —cualificados o con sufijo reconocible—,
 *       ni {@code at co.edu} ni ningún otro marco de traza de pila, ni nombres de restricción,
 *       tabla o columna de base de datos.</li>
 * </ul>
 *
 * <p>Sobre esa base, {@link #verificarContratoSinDetalles(MvcResult, HttpStatus)} exige además que
 * la clave {@code detalles} <strong>no aparezca</strong>, y
 * {@link #verificarContratoConDetalles(MvcResult, HttpStatus)} que aparezca como una lista no vacía
 * de textos. Son los dos tipos de escenario del contrato, y conviene usar el método específico en
 * lugar del base: el caso sin detalles es justamente donde se cuela un {@code "detalles": null} o
 * un {@code "detalles": []} que el cliente no debería recibir.
 *
 * <p>El <strong>orden de las claves</strong> se comprueba aparte, con
 * {@link #verificarOrdenDeClaves(MvcResult)}, porque hoy conviven varias implementaciones del
 * cuerpo de error y no todas lo garantizan — {@code ErrorResponseBuilder} usa {@code HashMap}.
 * Úsalo en los advices que arman el cuerpo con {@link ConstructorRespuestaError}, donde el orden
 * sale por construcción. Que el {@code timestamp} lleve zona se verifica en el origen, en
 * {@code ConstructorRespuestaErrorTest}; aquí solo se exige que sea una fecha-hora legible, para
 * que la utilidad siga sirviendo a los módulos que todavía usan {@code ErrorResponseBuilder}.
 */
public final class AsercionesContratoError {

    private static final List<String> CLAVES_OBLIGATORIAS =
            List.of("timestamp", "status", "error", "mensaje");

    private static final List<String> CLAVES_DEL_CONTRATO =
            List.of("timestamp", "status", "error", "mensaje", "detalles");

    /**
     * Fragmentos que, si aparecen en el cuerpo, delatan implementación interna. Se comparan sin
     * distinguir mayúsculas. Ninguno colisiona con el español de los mensajes: {@code exception} no
     * es «excepción», y de una restricción de base de datos se habla como «restricción», no como
     * {@code constraint}.
     */
    private static final List<String> FRAGMENTOS_PROHIBIDOS = List.of(
            "exception", "throwable", "stacktrace",
            "at co.edu", ".java:", "\tat ",
            "sqlstate", "constraint", "violates", "duplicate key", "relation \"",
            "uk_", "fk_", "pk_", "idx_");

    /** Nombre de clase cualificado: {@code co.edu.uco.sigra.profesores.ProfesorService}. */
    private static final Pattern NOMBRE_DE_CLASE_CUALIFICADO =
            Pattern.compile("\\b[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+\\.[A-Z][A-Za-z0-9_]*\\b");

    /** Nombre de clase simple con sufijo reconocible: {@code ProfesorServiceImpl}. */
    private static final Pattern NOMBRE_DE_CLASE_SIMPLE =
            Pattern.compile("\\b[A-Z][A-Za-z0-9_]*(?:Exception|Throwable|Handler|Impl|Repository)\\b");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AsercionesContratoError() {
    }

    /**
     * Comprobación base del contrato, sin pronunciarse sobre {@code detalles}.
     *
     * @param resultado      resultado de un {@code mockMvc.perform(...)}
     * @param statusEsperado código HTTP que debe traer la respuesta
     * @return el cuerpo ya parseado, para seguir con las aserciones propias del caso
     */
    public static JsonNode verificarContratoDeError(ResultActions resultado, HttpStatus statusEsperado) {
        return verificarContratoDeError(resultado.andReturn(), statusEsperado);
    }

    /**
     * Comprobación base del contrato, sin pronunciarse sobre {@code detalles}.
     *
     * @param resultado      resultado de un {@code mockMvc.perform(...).andReturn()}
     * @param statusEsperado código HTTP que debe traer la respuesta
     * @return el cuerpo ya parseado, para seguir con las aserciones propias del caso
     */
    public static JsonNode verificarContratoDeError(MvcResult resultado, HttpStatus statusEsperado) {
        String cuerpoCrudo = cuerpoCrudoDe(resultado);
        JsonNode cuerpo = parsear(cuerpoCrudo);

        assertThat(resultado.getResponse().getStatus())
                .as("El código HTTP de la respuesta. Cuerpo recibido: %s", cuerpoCrudo)
                .isEqualTo(statusEsperado.value());

        assertThat(cuerpo.isObject())
                .as("El cuerpo de error debe ser un objeto JSON. Cuerpo recibido: %s", cuerpoCrudo)
                .isTrue();

        verificarClaves(cuerpo, cuerpoCrudo);
        verificarCoherenciaEntreStatusYError(cuerpo, statusEsperado, cuerpoCrudo);
        verificarTimestamp(cuerpo, cuerpoCrudo);
        verificarMensaje(cuerpo, cuerpoCrudo);
        verificarQueNoFiltraDetalleTecnico(cuerpoCrudo);

        return cuerpo;
    }

    /**
     * Contrato completo para un error <strong>sin</strong> errores puntuales que enumerar: la clave
     * {@code detalles} no debe aparecer en el cuerpo, ni como {@code null} ni como lista vacía.
     *
     * @see #verificarContratoDeError(MvcResult, HttpStatus)
     */
    public static JsonNode verificarContratoSinDetalles(ResultActions resultado, HttpStatus statusEsperado) {
        return verificarContratoSinDetalles(resultado.andReturn(), statusEsperado);
    }

    /**
     * Contrato completo para un error <strong>sin</strong> errores puntuales que enumerar: la clave
     * {@code detalles} no debe aparecer en el cuerpo, ni como {@code null} ni como lista vacía.
     *
     * @see #verificarContratoDeError(MvcResult, HttpStatus)
     */
    public static JsonNode verificarContratoSinDetalles(MvcResult resultado, HttpStatus statusEsperado) {
        JsonNode cuerpo = verificarContratoDeError(resultado, statusEsperado);

        assertThat(cuerpo.has("detalles"))
                .as("Sin errores puntuales que enumerar, la clave detalles debe omitirse del cuerpo: "
                        + "no se envía ni nula ni vacía. Cuerpo recibido: %s", cuerpoCrudoDe(resultado))
                .isFalse();

        return cuerpo;
    }

    /**
     * Contrato completo para un error <strong>con</strong> errores puntuales: {@code detalles} debe
     * estar presente, ser una lista, traer al menos un elemento, y cada uno ser texto con contenido.
     *
     * @see #verificarContratoDeError(MvcResult, HttpStatus)
     */
    public static JsonNode verificarContratoConDetalles(ResultActions resultado, HttpStatus statusEsperado) {
        return verificarContratoConDetalles(resultado.andReturn(), statusEsperado);
    }

    /**
     * Contrato completo para un error <strong>con</strong> errores puntuales: {@code detalles} debe
     * estar presente, ser una lista, traer al menos un elemento, y cada uno ser texto con contenido.
     *
     * @see #verificarContratoDeError(MvcResult, HttpStatus)
     */
    public static JsonNode verificarContratoConDetalles(MvcResult resultado, HttpStatus statusEsperado) {
        String cuerpoCrudo = cuerpoCrudoDe(resultado);
        JsonNode cuerpo = verificarContratoDeError(resultado, statusEsperado);

        JsonNode detalles = cuerpo.get("detalles");
        assertThat(detalles)
                .as("El cuerpo debe traer la clave detalles con los errores puntuales. "
                        + "Cuerpo recibido: %s", cuerpoCrudo)
                .isNotNull();
        assertThat(detalles.isArray())
                .as("detalles debe ser una lista, un elemento por error puntual. "
                        + "Cuerpo recibido: %s", cuerpoCrudo)
                .isTrue();
        assertThat(detalles.isEmpty())
                .as("Una lista de detalles vacía no aporta nada al cliente: la clave debería "
                        + "haberse omitido. Cuerpo recibido: %s", cuerpoCrudo)
                .isFalse();

        for (JsonNode detalle : detalles) {
            assertThat(detalle.isString())
                    .as("Cada detalle debe ser un texto dirigido a una persona. "
                            + "Cuerpo recibido: %s", cuerpoCrudo)
                    .isTrue();
            assertThat(detalle.stringValue())
                    .as("Un detalle vacío no explica nada. Cuerpo recibido: %s", cuerpoCrudo)
                    .isNotBlank();
        }

        return cuerpo;
    }

    /**
     * Verifica que las claves salgan en el orden del contrato — {@code timestamp}, {@code status},
     * {@code error}, {@code mensaje} y, si aparece, {@code detalles} al final.
     *
     * <p>Se comprueba aparte de {@link #verificarContratoDeError(MvcResult, HttpStatus)} porque
     * solo lo garantizan los advices que arman el cuerpo con {@link ConstructorRespuestaError}; los
     * que todavía usan {@code ErrorResponseBuilder} dependen del orden arbitrario de un
     * {@code HashMap}.
     */
    public static void verificarOrdenDeClaves(ResultActions resultado) {
        verificarOrdenDeClaves(resultado.andReturn());
    }

    /**
     * Verifica que las claves salgan en el orden del contrato — {@code timestamp}, {@code status},
     * {@code error}, {@code mensaje} y, si aparece, {@code detalles} al final.
     *
     * <p>Se comprueba aparte de {@link #verificarContratoDeError(MvcResult, HttpStatus)} porque
     * solo lo garantizan los advices que arman el cuerpo con {@link ConstructorRespuestaError}; los
     * que todavía usan {@code ErrorResponseBuilder} dependen del orden arbitrario de un
     * {@code HashMap}.
     */
    public static void verificarOrdenDeClaves(MvcResult resultado) {
        String cuerpoCrudo = cuerpoCrudoDe(resultado);
        JsonNode cuerpo = parsear(cuerpoCrudo);

        List<String> presentes = clavesDe(cuerpo);
        List<String> esperadas = CLAVES_DEL_CONTRATO.stream().filter(presentes::contains).toList();

        assertThat(presentes)
                .as("El orden de las claves debe ser estable y el del contrato. Cuerpo recibido: %s",
                        cuerpoCrudo)
                .containsExactlyElementsOf(esperadas);
    }

    private static void verificarClaves(JsonNode cuerpo, String cuerpoCrudo) {
        List<String> presentes = clavesDe(cuerpo);

        assertThat(presentes)
                .as("Las cuatro claves obligatorias del contrato. Cuerpo recibido: %s", cuerpoCrudo)
                .containsAll(CLAVES_OBLIGATORIAS);

        assertThat(presentes)
                .as("El cuerpo de error no puede traer claves fuera del contrato: lo que no está "
                        + "especificado, el frontend no lo puede asumir. Cuerpo recibido: %s", cuerpoCrudo)
                .isSubsetOf(CLAVES_DEL_CONTRATO);
    }

    private static void verificarCoherenciaEntreStatusYError(JsonNode cuerpo, HttpStatus statusEsperado,
                                                             String cuerpoCrudo) {
        JsonNode status = cuerpo.get("status");
        assertThat(status.isInt())
                .as("El campo status debe ser un número, no un texto. Cuerpo recibido: %s", cuerpoCrudo)
                .isTrue();
        assertThat(status.asInt())
                .as("El campo status debe traer el mismo código HTTP de la respuesta. "
                        + "Cuerpo recibido: %s", cuerpoCrudo)
                .isEqualTo(statusEsperado.value());

        JsonNode error = cuerpo.get("error");
        assertThat(error.isString())
                .as("El campo error debe ser el nombre del código HTTP, como texto. "
                        + "Cuerpo recibido: %s", cuerpoCrudo)
                .isTrue();
        assertThat(error.stringValue())
                .as("El campo error debe ser el nombre del código HTTP; un %d con otro nombre es un "
                                + "defecto, porque el frontend ramifica sobre ese campo. Cuerpo recibido: %s",
                        statusEsperado.value(), cuerpoCrudo)
                .isEqualTo(statusEsperado.name());
    }

    private static void verificarTimestamp(JsonNode cuerpo, String cuerpoCrudo) {
        JsonNode timestamp = cuerpo.get("timestamp");
        assertThat(timestamp.isNull())
                .as("El cuerpo debe traer el instante en que se generó la respuesta. "
                        + "Cuerpo recibido: %s", cuerpoCrudo)
                .isFalse();

        if (timestamp.isString()) {
            String texto = timestamp.stringValue();
            if (!esFechaHoraLegible(texto)) {
                fail("El timestamp debe ser una fecha-hora ISO-8601 legible por el cliente, y llegó "
                        + "'%s'. Cuerpo recibido: %s", texto, cuerpoCrudo);
            }
        }
    }

    private static boolean esFechaHoraLegible(String texto) {
        try {
            OffsetDateTime.parse(texto);
            return true;
        } catch (DateTimeParseException sinZona) {
            try {
                LocalDateTime.parse(texto);
                return true;
            } catch (DateTimeParseException ilegible) {
                return false;
            }
        }
    }

    private static void verificarMensaje(JsonNode cuerpo, String cuerpoCrudo) {
        JsonNode mensaje = cuerpo.get("mensaje");
        assertThat(mensaje.isString())
                .as("El mensaje debe ser texto dirigido a una persona. Cuerpo recibido: %s", cuerpoCrudo)
                .isTrue();
        assertThat(mensaje.stringValue())
                .as("Un mensaje vacío deja al cliente sin saber qué estuvo mal. Cuerpo recibido: %s",
                        cuerpoCrudo)
                .isNotBlank();
    }

    private static void verificarQueNoFiltraDetalleTecnico(String cuerpoCrudo) {
        List<String> filtraciones = new ArrayList<>();
        String enMinuscula = cuerpoCrudo.toLowerCase();

        for (String fragmento : FRAGMENTOS_PROHIBIDOS) {
            if (enMinuscula.contains(fragmento)) {
                filtraciones.add("fragmento técnico '" + fragmento + "'");
            }
        }
        agregarCoincidencia(filtraciones, NOMBRE_DE_CLASE_CUALIFICADO, cuerpoCrudo,
                "nombre de clase cualificado");
        agregarCoincidencia(filtraciones, NOMBRE_DE_CLASE_SIMPLE, cuerpoCrudo, "nombre de clase");

        assertThat(filtraciones)
                .as("RNF-17: el cuerpo de error no puede revelar la implementación interna. La causa "
                        + "real va al log del servidor; al cliente, solo el mensaje. Cuerpo recibido: %s",
                        cuerpoCrudo)
                .isEmpty();
    }

    private static void agregarCoincidencia(List<String> filtraciones, Pattern patron, String cuerpoCrudo,
                                            String descripcion) {
        Matcher coincidencia = patron.matcher(cuerpoCrudo);
        if (coincidencia.find()) {
            filtraciones.add(descripcion + " '" + coincidencia.group() + "'");
        }
    }

    private static List<String> clavesDe(JsonNode cuerpo) {
        return new ArrayList<>(cuerpo.propertyNames());
    }

    private static String cuerpoCrudoDe(MvcResult resultado) {
        try {
            return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException ex) {
            return fail("No se pudo leer el cuerpo de la respuesta como UTF-8", ex);
        }
    }

    private static JsonNode parsear(String cuerpoCrudo) {
        if (cuerpoCrudo == null || cuerpoCrudo.isBlank()) {
            return fail("La respuesta de error llegó sin cuerpo, y el contrato exige siempre uno");
        }
        try {
            return MAPPER.readTree(cuerpoCrudo);
        } catch (JacksonException ex) {
            return fail("El cuerpo de error no es JSON válido: %s".formatted(cuerpoCrudo), ex);
        }
    }
}
