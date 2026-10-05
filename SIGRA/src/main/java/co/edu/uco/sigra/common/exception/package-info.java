/**
 * Contrato de manejo de errores de la API REST de SIGRA.
 *
 * <p>Este paquete es el territorio transversal del manejo de errores: aquí vive el contrato que
 * todo módulo debe cumplir cuando responde con un código 4xx o 5xx. La guía está en el código
 * fuente, y no en un documento aparte, para que quien agregue un advice de módulo nuevo la
 * encuentre en el mismo lugar donde trabaja.
 *
 * <h2>1. Forma del cuerpo de error</h2>
 *
 * <p>Toda respuesta de error entrega un cuerpo JSON con estas claves, <strong>en este orden</strong>:
 *
 * <table border="1">
 *   <caption>Claves del cuerpo de error</caption>
 *   <tr><th>Clave</th><th>Obligatoria</th><th>Contenido</th></tr>
 *   <tr>
 *     <td>{@code timestamp}</td><td>sí</td>
 *     <td>Instante en que se generó la respuesta, con zona.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code status}</td><td>sí</td>
 *     <td>El mismo código HTTP de la respuesta, como número.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code error}</td><td>sí</td>
 *     <td>Nombre del código HTTP en mayúsculas: {@code BAD_REQUEST}, {@code NOT_FOUND},
 *         {@code CONFLICT}, … Se deriva del status, nunca se escribe a mano.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code mensaje}</td><td>sí</td>
 *     <td>Explicación en español, dirigida a una persona, de qué estuvo mal.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code detalles}</td><td>no</td>
 *     <td>Lista de errores puntuales, un campo inválido por elemento. La clave
 *         <strong>se omite</strong> cuando no hay nada que enumerar; no se envía {@code null}
 *         ni una lista vacía.</td>
 *   </tr>
 * </table>
 *
 * <p>Dos reglas que no se negocian:
 *
 * <ul>
 *   <li><strong>El orden de las claves es estable</strong> entre peticiones y entre módulos. Por eso
 *       el cuerpo se arma con {@link java.util.LinkedHashMap} y no con {@link java.util.HashMap}.</li>
 *   <li><strong>El campo {@code error} corresponde siempre a {@code status}.</strong> Un 404 con
 *       {@code error: "BAD_REQUEST"} es un defecto, no un detalle cosmético: el frontend ramifica
 *       sobre ese campo.</li>
 * </ul>
 *
 * <h2>2. La API nunca expone detalles técnicos (RNF-17)</h2>
 *
 * <p>Ningún campo del cuerpo puede revelar la implementación interna. Está prohibido: trazas de
 * pila, nombres de clase Java, el texto {@code Exception}, rutas de paquete como {@code at co.edu},
 * fragmentos de SQL, nombres de tablas, columnas o restricciones de base de datos, y rutas del
 * sistema de archivos.
 *
 * <p>Esto aplica <em>también</em> cuando el fallo es inesperado. El patrón correcto es registrar y
 * no filtrar: {@code log.error("…", ex)} con la causa completa en el servidor, y un {@code mensaje}
 * genérico para el cliente. Entregar {@code ex.getMessage()} al cliente es el antipatrón que esta
 * regla existe para evitar — expone el detalle técnico <em>en vez de</em> registrarlo, y deja el
 * 500 sin rastro en el log.
 *
 * <h2>3. Precedencia entre advices</h2>
 *
 * <p>Spring resuelve una excepción recorriendo los {@code @ControllerAdvice} ordenados por
 * {@code @Order}; para cada uno pregunta si aplica al controlador y si tiene un método que acepte
 * esa excepción. <strong>El primer advice con un método aplicable gana:</strong> el orden entre
 * advices manda por encima de lo específico que sea el método. Un advice que no declara orden
 * recibe {@link org.springframework.core.Ordered#LOWEST_PRECEDENCE}.
 *
 * <p>Mapa de los seis advices del proyecto, de mayor a menor precedencia:
 *
 * <table border="1">
 *   <caption>Advices registrados y su precedencia</caption>
 *   <tr>
 *     <th>Advice</th><th>Orden declarado</th><th>Precedencia efectiva</th>
 *     <th>Alcance</th><th>¿Declara {@code Exception}?</th>
 *   </tr>
 *   <tr>
 *     <td>{@code asignaturas.exception.AsignaturaExceptionHandler}</td>
 *     <td>{@code HIGHEST_PRECEDENCE}</td><td>{@code Integer.MIN_VALUE}</td>
 *     <td>{@code assignableTypes = AsignaturaController}</td><td>No</td>
 *   </tr>
 *   <tr>
 *     <td>{@code semestres.exception.SemestreExceptionHandler}</td>
 *     <td>{@code HIGHEST_PRECEDENCE}</td><td>{@code Integer.MIN_VALUE}</td>
 *     <td>{@code assignableTypes = SemestreController}</td><td>No</td>
 *   </tr>
 *   <tr>
 *     <td>{@code resultadosaprendizaje.exception.ResultadoAprendizajeExceptionHandler}</td>
 *     <td>{@code HIGHEST_PRECEDENCE}</td><td>{@code Integer.MIN_VALUE}</td>
 *     <td>{@code basePackages = co.edu.uco.sigra.resultadosaprendizaje}</td><td>No</td>
 *   </tr>
 *   <tr>
 *     <td>{@code common.exception.ManejadorErroresTransversal}</td>
 *     <td>{@code 0}</td><td>{@code 0}</td>
 *     <td>global</td><td><strong>No, y no puede</strong> — ver §5</td>
 *   </tr>
 *   <tr>
 *     <td>{@code profesores.exception.ProfesorExceptionHandler}</td>
 *     <td>—</td><td>{@code Integer.MAX_VALUE}</td>
 *     <td>global</td><td>No</td>
 *   </tr>
 *   <tr>
 *     <td>{@code common.exception.GlobalExceptionHandler}</td>
 *     <td>—</td><td>{@code Integer.MAX_VALUE}</td>
 *     <td>global</td><td><strong>Sí</strong>, es el respaldo de último recurso</td>
 *   </tr>
 * </table>
 *
 * <p>Dos observaciones sobre esta tabla, útiles al depurar un código HTTP inesperado:
 *
 * <ul>
 *   <li>{@code ProfesorExceptionHandler} y {@code GlobalExceptionHandler} <strong>empatan</strong>
 *       en {@code LOWEST_PRECEDENCE}. El desempate lo define el orden de los beans, que viene del
 *       escaneo del classpath, y Spring no garantiza que sea el mismo entre máquinas. Si el global
 *       se evalúa primero, su método para {@code Exception} convierte las excepciones de
 *       {@code profesores} en 500 en lugar de 404 o 409. La corrección es una línea en el advice de
 *       {@code profesores} — declarar {@code @Order(Ordered.HIGHEST_PRECEDENCE)} — y le corresponde
 *       a su dueño.</li>
 *   <li>Un advice de módulo con máxima precedencia que declare {@code Exception} se queda con
 *       <em>todo</em> lo que ocurra en sus controladores, incluidos los errores de petición mal
 *       formada que el advice transversal sabría mapear mejor. Ningún advice aditivo puede ganarle,
 *       porque el orden manda sobre lo específico del método. Por eso §4 lo prohíbe.</li>
 * </ul>
 *
 * <h2>4. Cómo enchufar el advice de un módulo nuevo</h2>
 *
 * <p>Los módulos de los Sprints 2 y 3 ({@code estudiantes}, {@code evaluaciones},
 * {@code estadisticas}, {@code mejoras}, {@code reportes}) siguen esta receta y no necesitan tocar
 * nada de este paquete:
 *
 * <ol>
 *   <li><strong>Nombre y ubicación:</strong> {@code {Modulo}ExceptionHandler} en
 *       {@code co.edu.uco.sigra.{modulo}.exception}, por convención del equipo.</li>
 *   <li><strong>Acota el alcance</strong> con
 *       {@code @RestControllerAdvice(assignableTypes = TuController.class)} o con
 *       {@code basePackages = "co.edu.uco.sigra.{modulo}"}. Un advice de módulo registrado como
 *       global aplica también a los controladores de los demás, que es exactamente lo que no se
 *       quiere.</li>
 *   <li><strong>Declara {@code @Order(Ordered.HIGHEST_PRECEDENCE)}.</strong> Sin orden quedas
 *       empatado con {@code GlobalExceptionHandler} y tu 404 pasa a depender del classpath.</li>
 *   <li><strong>Declara solo las excepciones de tu dominio.</strong> Nunca {@code Exception} ni
 *       {@code Throwable}: con tu precedencia te quedarías con los errores de petición mal formada
 *       que el advice transversal mapea a 415, 406 y 400, y los devolverías como 500.</li>
 *   <li><strong>Arma el cuerpo con {@code ConstructorRespuestaError}</strong> de este paquete, para
 *       que el orden de las claves y la coherencia entre {@code status} y {@code error} salgan por
 *       construcción y no por disciplina.</li>
 *   <li><strong>Verifica el contrato con {@code AsercionesContratoError}</strong>, en las fuentes de
 *       prueba de este paquete: una sola llamada comprueba la forma del cuerpo y la ausencia de
 *       detalle técnico sobre un resultado de {@code MockMvc}.</li>
 * </ol>
 *
 * <p>Lo que <strong>no</strong> hace falta declarar en un advice de módulo, porque ya está cubierto:
 * validación de DTO (400), cuerpo JSON ilegible (400), tipo de parámetro inválido (400), método no
 * soportado (405), ruta inexistente (404), 401, 403 y el respaldo 500 los resuelve
 * {@code GlobalExceptionHandler}; {@code Content-Type} no soportado (415), {@code Accept} no
 * satisfacible (406), parámetro de consulta o parte multiparte obligatoria ausente (400),
 * {@code ConstraintViolationException} de Jakarta (400) y violación de integridad de datos (409)
 * los resuelve {@code ManejadorErroresTransversal}.
 *
 * <h2>5. La posición {@code 0} queda reservada para el advice transversal</h2>
 *
 * <p>El orden {@code 0} pertenece a {@code ManejadorErroresTransversal} y <strong>ningún otro
 * advice debe usarlo</strong>, ni usar un valor menor o igual a {@code 0} en un advice global.
 *
 * <p>La razón es que {@code 0} es la única posición que sirve. Gana contra
 * {@code LOWEST_PRECEDENCE}, así que se evalúa antes que el respaldo para {@code Exception} del
 * advice global y puede mapear 415, 406 y 400 donde hoy saldría 500. Y pierde contra
 * {@code HIGHEST_PRECEDENCE}, así que los advices de módulo conservan intacta la prioridad que sus
 * dueños les dieron. Un segundo advice global en {@code 0} reintroduce el desempate no determinista
 * que esta reserva existe para eliminar.
 *
 * <p>Queda espacio deliberado a ambos lados — los enteros negativos y los positivos — por si el
 * equipo necesita insertar una capa intermedia más adelante.
 *
 * <p>A cambio de ocupar esa posición, el advice transversal acepta una restricción estricta:
 * <strong>declara únicamente tipos de excepción que ningún advice existente declara, y nunca
 * {@code Exception} ni {@code Throwable}</strong>. Es lo que hace que agregarlo no pueda cambiar
 * ninguna respuesta que hoy sea correcta. Si declarara {@code Exception}, con el orden {@code 0} le
 * ganaría a {@code ProfesorExceptionHandler} y convertiría sus 404 y 409 en 500: rompería el módulo
 * de un compañero para arreglar un contrato.
 *
 * <h2>6. Deuda documentada: tres implementaciones del cuerpo de error (H-05)</h2>
 *
 * <p>El contrato de este paquete lo cumplen hoy <strong>tres</strong> implementaciones distintas del
 * mismo cuerpo JSON, y no hay nada que impida que aparezca una cuarta:
 *
 * <ul>
 *   <li>{@link ErrorResponseBuilder}, con {@link java.util.HashMap} — <strong>no garantiza el orden
 *       de las claves</strong> — y que recibe {@code error} como string literal, lo que permite
 *       incoherencias como un 404 con {@code error: "BAD_REQUEST"}. La usan
 *       {@code GlobalExceptionHandler}, {@code ProfesorExceptionHandler},
 *       {@code ResultadoAprendizajeExceptionHandler} y {@code SemestreExceptionHandler}.</li>
 *   <li>Un método privado {@code construirRespuestaError}, con {@link java.util.LinkedHashMap}, en
 *       {@code AsignaturaExceptionHandler}. Ya no es la única copia de este patrón: hasta el
 *       2026-10-03 {@code SemestreExceptionHandler} tenía la suya, consolidada en ese módulo hacia
 *       {@code ErrorResponseBuilder} — la prueba de que acercar estas implementaciones es viable sin
 *       romper nada.</li>
 *   <li>{@link ConstructorRespuestaError}, de este paquete, que corrige las dos fallas de
 *       {@code ErrorResponseBuilder}: orden de claves garantizado y {@code error} derivado de
 *       {@code status}. Es la que deben usar los módulos nuevos (§4); no sustituye a las otras dos en
 *       los módulos que ya las adoptaron, porque hacerlo exigiría editar sus archivos.</li>
 * </ul>
 *
 * <p>Ninguna de las tres es, hoy, un defecto observable desde fuera: las tres producen un cuerpo que
 * cumple el contrato cuando se usan correctamente. El riesgo es de mantenimiento, no de
 * comportamiento — tres lugares donde corregir el mismo bug, y la posibilidad real de que diverjan
 * más de lo que ya divergieron. La consolidación hacia {@code ConstructorRespuestaError} queda
 * propuesta como cambio futuro, cuando los módulos de los Sprints 2 y 3 estén cerrados y el refactor
 * no choque con nadie en paralelo.
 */
package co.edu.uco.sigra.common.exception;
