package co.edu.uco.sigra.asignaturas.integracion;

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

/** Verifica el contrato OpenAPI de RF-07 sin ejecutar sus endpoints ni modificar datos. */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Documentación OpenAPI del módulo Asignación docente")
class AsignacionDocenteApiDocumentacionIT {

    private static final String RUTA_LISTA = "/api/v1/asignaciones-docentes";
    private static final String RUTA_DESASIGNAR = "/api/v1/asignaciones-docentes/{id}/desasignar";

    @Autowired
    private MockMvc mockMvc;

    private String documento() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/v3/api-docs")).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        return new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    static Stream<Arguments> operaciones() {
        return Stream.of(
                Arguments.of(RUTA_LISTA, "post", Set.of("201", "400", "401", "403", "404")),
                Arguments.of(RUTA_DESASIGNAR, "patch", Set.of("204", "400", "401", "403", "404"))
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
    @DisplayName("Cada endpoint declara seguridad y todos sus códigos de respuesta")
    @SuppressWarnings("unchecked")
    void documentaCodigosYSeguridad(String ruta, String metodo, Set<String> codigosEsperados) throws Exception {
        String doc = documento();
        String base = "$.paths['" + ruta + "']." + metodo;

        Map<String, Object> respuestas = JsonPath.read(doc, base + ".responses");
        assertThat(respuestas.keySet()).containsAll(codigosEsperados);

        Map<String, Object> requisito = JsonPath.read(doc, base + ".security[0]");
        assertThat(requisito).containsKey("bearerAuth");
    }
}
