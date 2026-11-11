package co.edu.uco.sigra.facultades.integracion;

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
 * Verifica el documento OpenAPI que genera springdoc para el módulo Facultades: que exista el esquema
 * Bearer, que cada endpoint exija ese esquema, que documente los códigos de respuesta que puede
 * devolver y que el campo facultadId aparezca en el contrato de programas. Solo lee
 * {@code /v3/api-docs}: no toca la base de datos más allá de arrancar el contexto.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Documentación OpenAPI del módulo Facultades")
class FacultadApiDocumentacionIT {

    private static final String RUTA_LISTA = "/api/v1/facultades";
    private static final String RUTA_ID = "/api/v1/facultades/{id}";
    private static final String RUTA_INACTIVAR = "/api/v1/facultades/{id}/inactivar";

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
                Arguments.of(RUTA_LISTA, "get", Set.of("200", "400", "401", "403")),
                Arguments.of(RUTA_ID, "get", Set.of("200", "400", "401", "403", "404")),
                Arguments.of(RUTA_ID, "put", Set.of("200", "400", "401", "403", "404", "409")),
                Arguments.of(RUTA_INACTIVAR, "patch", Set.of("200", "400", "401", "403", "404", "409"))
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

    @Test
    @DisplayName("El módulo aparece agrupado bajo la etiqueta Facultades")
    void agrupaBajoEtiquetaFacultades() throws Exception {
        String doc = documento();

        List<String> etiquetas = JsonPath.read(doc, "$.paths['" + RUTA_LISTA + "'].post.tags");

        assertThat(etiquetas).containsExactly("Facultades");
    }

    @Test
    @DisplayName("Los esquemas de facultad y de programa exponen los campos de facultad")
    void esquemasExponenCamposDeFacultad() throws Exception {
        String doc = documento();

        Map<String, Object> facultadRequest = JsonPath.read(doc, "$.components.schemas.FacultadRequest.properties");
        Map<String, Object> facultadResponse = JsonPath.read(doc, "$.components.schemas.FacultadResponse.properties");
        Map<String, Object> programaRequest = JsonPath.read(doc, "$.components.schemas.ProgramaRequest.properties");
        Map<String, Object> programaResponse = JsonPath.read(doc, "$.components.schemas.ProgramaResponse.properties");

        assertThat(facultadRequest).containsOnlyKeys("nombre");
        assertThat(facultadResponse).containsKeys("id", "nombre", "estado");
        assertThat(programaRequest).containsKey("facultadId");
        assertThat(programaResponse).containsKeys("facultadId", "facultadNombre");

        Map<String, Object> programaUpdate = JsonPath.read(doc, "$.components.schemas.ProgramaUpdate.properties");
        assertThat(programaUpdate).containsKeys("nombre", "facultadId");
    }

    @Test
    @DisplayName("El listado de programas documenta el filtro facultadId y el PUT los errores de facultad")
    void programasDocumentanFacultad() throws Exception {
        String doc = documento();

        List<String> parametros = JsonPath.read(doc, "$.paths['/api/v1/programas'].get.parameters[*].name");
        assertThat(parametros).contains("facultadId");

        Map<String, Object> respuestasPut = JsonPath.read(doc, "$.paths['/api/v1/programas/{id}'].put.responses");
        assertThat(respuestasPut.keySet()).contains("404", "409");
        Map<String, Object> respuestasPost = JsonPath.read(doc, "$.paths['/api/v1/programas'].post.responses");
        assertThat(respuestasPost.keySet()).contains("404", "409");
    }
}
