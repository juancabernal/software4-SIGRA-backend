package co.edu.uco.sigra.evaluaciones.integracion;

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
 * Verifica el documento OpenAPI que genera springdoc para Evaluaciones: que cada endpoint exija el esquema
 * Bearer, esté bajo su tag y documente exactamente los códigos de respuesta que puede devolver (guía
 * docs/documentacion_openapi/swagger-profesores.md). Solo lee {@code /v3/api-docs}.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Documentación OpenAPI de Evaluaciones")
class EvaluacionApiDocumentacionIT {

    private static final String RUTA_POR_RA = "/api/v1/resultados-aprendizaje/{raId}/evaluaciones";
    private static final String RUTA_ID = "/api/v1/evaluaciones/{id}";

    @Autowired
    private MockMvc mockMvc;

    private String documento() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/v3/api-docs")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);

        return new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    static Stream<Arguments> operaciones() {
        return Stream.of(
                Arguments.of(RUTA_POR_RA, "post", Set.of("201", "400", "401", "403", "404", "409")),
                Arguments.of(RUTA_POR_RA, "get", Set.of("200", "400", "401", "403", "404")),
                Arguments.of(RUTA_ID, "get", Set.of("200", "400", "401", "403", "404")),
                Arguments.of(RUTA_ID, "put", Set.of("200", "400", "401", "403", "404", "409")),
                Arguments.of(RUTA_ID + "/inactivar", "patch", Set.of("200", "400", "401", "403", "404")),
                Arguments.of(RUTA_ID + "/reactivar", "patch", Set.of("200", "400", "401", "403", "404"))
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
        assertThat(tags).containsAnyOf("Evaluaciones");

        String resumen = JsonPath.read(doc, base + ".summary");
        assertThat(resumen).isNotBlank();
    }

    @Test
    @DisplayName("Los cuerpos y la respuesta documentan sus campos")
    void documentaCuerpos() throws Exception {
        String doc = documento();

        Map<String, Object> crear = JsonPath.read(doc, "$.components.schemas.EvaluacionCrearRequest.properties");
        assertThat(crear.keySet()).containsExactlyInAnyOrder("nombre", "fechaTentativa", "porcentaje", "descripcion");
        Map<String, Object> respuesta = JsonPath.read(doc, "$.components.schemas.EvaluacionResponse.properties");
        assertThat(respuesta.keySet()).contains("id", "semestreCodigo", "porcentaje", "estado", "tieneCalificaciones");
    }
}
