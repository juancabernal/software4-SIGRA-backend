package co.edu.uco.sigra.semestres.integracion;

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
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Verifica el documento OpenAPI que genera springdoc para el módulo Semestres: que exista el esquema
 * Bearer, que cada endpoint exija ese esquema y que documente los códigos de respuesta que puede
 * devolver. Solo lee {@code /v3/api-docs}: no toca la base de datos más allá de arrancar el contexto.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Documentación OpenAPI del módulo Semestres")
class SemestreApiDocumentacionIT {

    private static final String RUTA_LISTA = "/api/v1/semestres";
    private static final String RUTA_CODIGO = "/api/v1/semestres/{codigo}";

    @Autowired
    private MockMvc mockMvc;

    private String documento() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/v3/api-docs")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);

        return new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    static Stream<Arguments> operaciones() {
        return Stream.of(
                Arguments.of(RUTA_LISTA, "post", Set.of("201", "400", "401", "403", "409")),
                Arguments.of(RUTA_LISTA, "get", Set.of("200", "401", "403")),
                Arguments.of(RUTA_CODIGO, "get", Set.of("200", "401", "403", "404")),
                Arguments.of(RUTA_CODIGO, "patch", Set.of("200", "400", "401", "403", "404", "409"))
        );
    }

    @Test
    @DisplayName("Define el esquema de seguridad Bearer JWT")
    void defineEsquemaBearer() throws Exception {
        String doc = documento();

        assertThat((String) JsonPath.read(doc, "$.components.securitySchemes.bearerAuth.type"))
                .isEqualTo("http");
        assertThat((String) JsonPath.read(doc, "$.components.securitySchemes.bearerAuth.scheme"))
                .isEqualTo("bearer");
        assertThat((String) JsonPath.read(doc, "$.components.securitySchemes.bearerAuth.bearerFormat"))
                .isEqualTo("JWT");
    }

    @ParameterizedTest(name = "{1} {0} documenta {2} y exige bearerAuth")
    @MethodSource("operaciones")
    @DisplayName("Cada endpoint documenta sus códigos de respuesta y exige Bearer")
    void documentaCodigosYSeguridad(String ruta, String metodo, Set<String> codigosEsperados) throws Exception {
        String doc = documento();
        String base = "$.paths['" + ruta + "']." + metodo;

        Map<String, Object> respuestas = JsonPath.read(doc, base + ".responses");

        assertThat(respuestas.keySet()).containsAll(codigosEsperados);

        Map<String, Object> requisito = JsonPath.read(doc, base + ".security[0]");

        assertThat(requisito).containsKey("bearerAuth");
    }
}