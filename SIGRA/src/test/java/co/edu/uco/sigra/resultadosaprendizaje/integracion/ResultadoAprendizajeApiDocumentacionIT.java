package co.edu.uco.sigra.resultadosaprendizaje.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Verifica el documento OpenAPI que genera springdoc para Resultados de aprendizaje y su rendimiento: que cada endpoint exija el esquema
 * Bearer, esté bajo su tag y documente exactamente los códigos de respuesta que puede devolver (guía
 * docs/documentacion_openapi/swagger-profesores.md). Solo lee {@code /v3/api-docs}.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Documentación OpenAPI de Resultados de aprendizaje y su rendimiento")
class ResultadoAprendizajeApiDocumentacionIT {

    private static final String RUTA_POR_ASIGNATURA = "/api/v1/asignaturas/{asignaturaId}/resultados-aprendizaje";
    private static final String RUTA_TODOS = "/api/v1/resultados-aprendizaje";
    private static final String RUTA_ID = "/api/v1/resultados-aprendizaje/{id}";
    private static final String RUTA_RENDIMIENTO =
            "/api/v1/asignaturas/{asignaturaId}/resultados-aprendizaje/rendimiento";

    @Autowired
    private MockMvc mockMvc;

    private String documento() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/v3/api-docs")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);

        return new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    static Stream<Arguments> operaciones() {
        return Stream.of(
                Arguments.of(RUTA_POR_ASIGNATURA, "post", Set.of("201", "400", "401", "403", "404", "409")),
                Arguments.of(RUTA_POR_ASIGNATURA, "get", Set.of("200", "400", "401", "403", "404")),
                Arguments.of(RUTA_TODOS, "get", Set.of("200", "400", "401", "403")),
                Arguments.of(RUTA_ID, "get", Set.of("200", "400", "401", "403", "404")),
                Arguments.of(RUTA_ID, "put", Set.of("200", "400", "401", "403", "404")),
                Arguments.of(RUTA_ID + "/inactivar", "patch", Set.of("200", "400", "401", "403", "404")),
                Arguments.of(RUTA_ID + "/reactivar", "patch", Set.of("200", "400", "401", "403", "404")),
                Arguments.of(RUTA_RENDIMIENTO, "get", Set.of("200", "400", "401", "403", "404"))
        );
    }

    @ParameterizedTest(name = "{1} {0} documenta {2} y exige bearerAuth")
    @MethodSource("operaciones")
    @DisplayName("Cada endpoint documenta sus códigos de respuesta, su tag y exige Bearer")
    void documentaCodigosYSeguridad(String ruta, String metodo, Set<String> codigosEsperados) throws Exception {
        String doc = documento();
        String base = "$.paths['" + ruta + "']." + metodo;

        Map<String, Object> respuestas = JsonPath.read(doc, base + ".responses");
        assertThat(respuestas.keySet()).containsExactlyInAnyOrderElementsOf(codigosEsperados);

        Map<String, Object> requisito = JsonPath.read(doc, base + ".security[0]");
        assertThat(requisito).containsKey("bearerAuth");

        List<String> tags = JsonPath.read(doc, base + ".tags");
        assertThat(tags).containsAnyOf("Resultados de aprendizaje", "Rendimiento de resultados de aprendizaje");

        String resumen = JsonPath.read(doc, base + ".summary");
        assertThat(resumen).isNotBlank();
    }

    @Test
    @DisplayName("El rendimiento documenta el parámetro opcional semestreId y los campos del resultado")
    void documentaRendimiento() throws Exception {
        String doc = documento();

        List<Map<String, Object>> parametros = JsonPath.read(doc,
                "$.paths['" + RUTA_RENDIMIENTO + "'].get.parameters[?(@.name=='semestreId')]");
        assertThat(parametros).hasSize(1);
        assertThat(parametros.get(0).get("required")).isEqualTo(false);

        Map<String, Object> propiedades = JsonPath.read(doc,
                "$.components.schemas.RendimientoResultadoAprendizaje.properties");
        assertThat(propiedades.keySet()).contains("estado", "nivelLogro", "promedioNota", "porcentajeAsignado",
                "porcentajeEvaluado", "evaluacionesActivas", "minimoEvaluaciones", "completo");
    }

    @Test
    @DisplayName("Los cuerpos de entrada documentan sus campos")
    void documentaCuerpos() throws Exception {
        String doc = documento();

        Map<String, Object> crear = JsonPath.read(doc,
                "$.components.schemas.ResultadoAprendizajeCrearRequest.properties");
        assertThat(crear.keySet()).containsExactlyInAnyOrder("codigo", "descripcion");
        Map<String, Object> actualizar = JsonPath.read(doc,
                "$.components.schemas.ResultadoAprendizajeActualizarRequest.properties");
        assertThat(actualizar.keySet()).containsExactly("descripcion");
    }
}
