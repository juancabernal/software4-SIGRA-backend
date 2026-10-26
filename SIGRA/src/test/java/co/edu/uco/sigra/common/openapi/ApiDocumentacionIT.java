package co.edu.uco.sigra.common.openapi;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Verificación global de RNF-20: lee /v3/api-docs y exige que cada endpoint protegido de /api/** declare
 * el esquema bearerAuth, un código de éxito, el 401 y el 403 (guía docs/documentacion_openapi/swagger-profesores.md).
 * <p>
 * Los controladores que su responsable aún no documenta están en {@link #PENDIENTES}, por el tag que
 * springdoc les asigna por defecto («nombre-controller»). La lista solo puede reducirse: cuando un
 * controlador ya tiene {@code @Tag}, la segunda prueba falla hasta que se quite de aquí. La tarea de
 * consolidación queda cumplida cuando la lista está vacía y las dos pruebas pasan.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Documentación OpenAPI de todos los endpoints protegidos (RNF-20)")
class ApiDocumentacionIT {

    private static final String ESQUEMA_BEARER = "bearerAuth";
    private static final Set<String> METODOS_HTTP = Set.of("get", "post", "put", "patch", "delete");
    private static final Set<String> RUTAS_PUBLICAS = Set.of("/api/v1/auth/login");

    /** Tag por defecto de springdoc → clase del controlador pendiente de documentar. */
    private static final Map<String, String> PENDIENTES = Map.ofEntries(
            Map.entry("tipo-documento-controller", "co.edu.uco.sigra.common.controller.TipoDocumentoController")
    );

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Cada endpoint documentado exige Bearer y documenta éxito, 401 y 403")
    @SuppressWarnings("unchecked")
    void endpointsDocumentados() throws Exception {
        String docs = leerDocs();
        boolean bearerGlobal = tieneBearer(leerOpcional(docs, "$.security"));
        Map<String, Map<String, Object>> rutas = JsonPath.read(docs, "$.paths");

        List<String> faltantes = new ArrayList<>();
        rutas.forEach((ruta, operaciones) -> operaciones.forEach((metodo, valor) -> {
            if (!METODOS_HTTP.contains(metodo) || !ruta.startsWith("/api/") || RUTAS_PUBLICAS.contains(ruta)) {
                return;
            }
            Map<String, Object> operacion = (Map<String, Object>) valor;
            List<String> tags = (List<String>) operacion.getOrDefault("tags", List.of());
            if (tags.stream().anyMatch(PENDIENTES::containsKey)) {
                return;
            }
            String id = metodo.toUpperCase(Locale.ROOT) + " " + ruta;
            if (!bearerGlobal && !tieneBearer(operacion.get("security"))) {
                faltantes.add(id + ": falta @SecurityRequirement(name = \"bearerAuth\")");
            }
            Map<String, Object> respuestas = (Map<String, Object>) operacion.getOrDefault("responses", Map.of());
            if (respuestas.keySet().stream().noneMatch(codigo -> codigo.startsWith("2"))) {
                faltantes.add(id + ": falta el código de éxito (200/201/204)");
            }
            for (String codigo : List.of("401", "403")) {
                if (!respuestas.containsKey(codigo)) {
                    faltantes.add(id + ": falta el " + codigo);
                }
            }
        }));

        assertThat(faltantes)
                .as("Endpoints sin documentar según docs/documentacion_openapi/swagger-profesores.md")
                .isEmpty();
    }

    @Test
    @DisplayName("La lista de pendientes solo contiene controladores que de verdad siguen sin @Tag")
    void pendientesSiguenPendientes() {
        List<String> yaDocumentados = new ArrayList<>();
        Set<String> inexistentes = new HashSet<>();
        PENDIENTES.forEach((tag, clase) -> {
            try {
                if (Class.forName(clase).isAnnotationPresent(io.swagger.v3.oas.annotations.tags.Tag.class)) {
                    yaDocumentados.add(tag);
                }
            } catch (ClassNotFoundException e) {
                // El controlador aún no existe en esta rama (por ejemplo, evaluaciones en main).
                inexistentes.add(tag);
            }
        });

        assertThat(yaDocumentados)
                .as("Ya tienen @Tag: quítelos de PENDIENTES para que se verifique su documentación "
                        + "(no existen en esta rama: " + inexistentes + ")")
                .isEmpty();
    }

    @Test
    @DisplayName("El login público documenta su contrato, errores y ausencia de Bearer")
    @SuppressWarnings("unchecked")
    void documentaAutenticacion() throws Exception {
        String docs = leerDocs();
        Map<String, Object> login = JsonPath.read(docs, "$.paths['/api/v1/auth/login'].post");

        assertThat((List<String>) login.get("tags")).containsExactly("Autenticación");
        Object seguridad = login.get("security");
        assertThat(seguridad == null || (seguridad instanceof List<?> lista && lista.isEmpty()))
                .as("El login es público y no debe heredar bearerAuth")
                .isTrue();

        Map<String, Object> cuerpo = (Map<String, Object>) login.get("requestBody");
        assertThat(cuerpo).containsEntry("required", true);
        assertThat(JsonPath.<String>read(docs,
                "$.paths['/api/v1/auth/login'].post.requestBody.content['application/json'].schema.$ref"))
                .isEqualTo("#/components/schemas/LoginRequest");

        Map<String, Object> solicitud = JsonPath.read(docs, "$.components.schemas.LoginRequest");
        assertThat((List<String>) solicitud.get("required"))
                .containsExactlyInAnyOrder("correoInstitucional", "contrasena");
        Map<String, Object> propiedadesSolicitud = (Map<String, Object>) solicitud.get("properties");
        assertThat(propiedadesSolicitud).containsKeys("correoInstitucional", "contrasena");
        assertThat((Number) ((Map<String, Object>) propiedadesSolicitud.get("correoInstitucional")).get("maxLength"))
                .isEqualTo(254);
        assertThat(((Map<String, Object>) propiedadesSolicitud.get("contrasena")).get("description").toString())
                .contains("72 bytes UTF-8");

        Map<String, Object> respuestas = (Map<String, Object>) login.get("responses");
        assertThat(respuestas.keySet()).containsExactlyInAnyOrder("200", "400", "401", "423", "413", "415", "500");
        assertThat(JsonPath.<String>read(docs,
                "$.paths['/api/v1/auth/login'].post.responses['200'].content['application/json'].schema.$ref"))
                .isEqualTo("#/components/schemas/LoginResponse");
        Map<String, Object> respuesta = JsonPath.read(docs, "$.components.schemas.LoginResponse");
        assertThat((List<String>) respuesta.get("required"))
                .containsExactlyInAnyOrder("token", "tipo", "expiraEn", "usuario");
        for (String codigo : List.of("400", "401", "423", "413", "415", "500")) {
            assertThat(JsonPath.<String>read(docs,
                    "$.paths['/api/v1/auth/login'].post.responses['" + codigo
                            + "'].content['application/json'].schema.$ref"))
                    .as("respuesta %s", codigo)
                    .isEqualTo("#/components/schemas/RespuestaError");
        }
        assertThat(((Map<String, Object>) respuestas.get("423")).get("description").toString())
                .contains("15 minutos", "cinco intentos");

        Map<String, Object> usuario = JsonPath.read(docs, "$.components.schemas.UsuarioSesion");
        Map<String, Object> propiedadesUsuario = (Map<String, Object>) usuario.get("properties");
        assertThat(propiedadesUsuario).containsKeys("id", "nombreCompleto", "correoInstitucional", "rol")
                .doesNotContainKey("passwordHash");
        assertThat((Map<String, Object>) JsonPath.read(docs, "$.components.schemas.LoginResponse.properties"))
                .doesNotContainKey("passwordHash");
    }

    @Test
    @DisplayName("Auditoría documenta parámetros, paginación, esquemas y autorización de administrador")
    @SuppressWarnings("unchecked")
    void documentaAuditoria() throws Exception {
        String docs = leerDocs();
        Map<String, Object> auditoria = JsonPath.read(docs, "$.paths['/api/v1/auditoria'].get");

        assertThat((List<String>) auditoria.get("tags")).containsExactly("Auditoría");
        Map<String, Object> requisito = JsonPath.read(docs, "$.paths['/api/v1/auditoria'].get.security[0]");
        assertThat(requisito).containsKey(ESQUEMA_BEARER);

        List<Map<String, Object>> parametros = JsonPath.read(docs,
                "$.paths['/api/v1/auditoria'].get.parameters");
        assertThat(parametros.stream().map(p -> p.get("name")).toList())
                .containsExactlyInAnyOrder("desde", "hasta", "tipoEntidad", "pagina", "tamano");
        Map<String, Object> pagina = parametros.stream()
                .filter(p -> p.get("name").equals("pagina"))
                .findFirst().orElseThrow();
        Map<String, Object> paginaSchema = (Map<String, Object>) pagina.get("schema");
        assertThat(paginaSchema).containsEntry("default", 0).containsEntry("minimum", 0);
        Map<String, Object> tamano = parametros.stream()
                .filter(p -> p.get("name").equals("tamano"))
                .findFirst().orElseThrow();
        Map<String, Object> tamanoSchema = (Map<String, Object>) tamano.get("schema");
        assertThat(tamanoSchema).containsEntry("default", 20).containsEntry("minimum", 1).containsEntry("maximum", 100);

        Map<String, Object> respuestas = (Map<String, Object>) auditoria.get("responses");
        assertThat(respuestas.keySet()).containsExactlyInAnyOrder("200", "400", "401", "403");
        assertThat(JsonPath.<String>read(docs,
                "$.paths['/api/v1/auditoria'].get.responses['200'].content['application/json'].schema.$ref"))
                .isEqualTo("#/components/schemas/PaginaAuditoria");
        for (String codigo : List.of("400", "401", "403")) {
            assertThat(JsonPath.<String>read(docs,
                    "$.paths['/api/v1/auditoria'].get.responses['" + codigo
                            + "'].content['application/json'].schema.$ref"))
                    .as("respuesta %s", codigo)
                    .isEqualTo("#/components/schemas/RespuestaError");
        }

        Map<String, Object> registro = JsonPath.read(docs, "$.components.schemas.RegistroAuditoria");
        Map<String, Object> propiedadesRegistro = (Map<String, Object>) registro.get("properties");
        assertThat(propiedadesRegistro).containsKeys("id", "fechaHora", "accion", "tipoEntidad", "idEntidad",
                "usuarioId", "rolUsuario", "detalle");
        assertThat(((Map<String, Object>) propiedadesRegistro.get("fechaHora")).get("example").toString())
                .doesNotContain("-05:00");
        assertThat(((List<String>) ((Map<String, Object>) propiedadesRegistro.get("idEntidad")).get("type")))
                .contains("null");
        assertThat(((List<String>) ((Map<String, Object>) propiedadesRegistro.get("usuarioId")).get("type")))
                .contains("null");
        assertThat(((List<String>) ((Map<String, Object>) propiedadesRegistro.get("rolUsuario")).get("type")))
                .contains("null");

        Map<String, Object> paginaSchemaCompleta = JsonPath.read(docs, "$.components.schemas.PaginaAuditoria");
        assertThat((Map<String, Object>) paginaSchemaCompleta.get("properties"))
                .containsKeys("contenido", "pagina", "tamano", "totalElementos", "totalPaginas");
        assertThat((List<String>) paginaSchemaCompleta.get("required"))
                .containsExactlyInAnyOrder("contenido", "pagina", "tamano", "totalElementos", "totalPaginas");
    }

    private String leerDocs() throws Exception {
        return new String(mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8);
    }

    private static Object leerOpcional(String json, String ruta) {
        try {
            return JsonPath.read(json, ruta);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    private static boolean tieneBearer(Object seguridad) {
        return seguridad instanceof List<?> requisitos
                && requisitos.stream().anyMatch(r -> r instanceof Map<?, ?> m && m.containsKey(ESQUEMA_BEARER));
    }
}
