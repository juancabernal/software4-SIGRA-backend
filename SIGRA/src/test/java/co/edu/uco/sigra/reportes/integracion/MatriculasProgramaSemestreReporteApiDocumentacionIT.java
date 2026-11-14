package co.edu.uco.sigra.reportes.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import co.edu.uco.sigra.config.OpenApiConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MatriculasProgramaSemestreReporteApiDocumentacionIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentaRutaParametrosRespuestasYSeguridad() throws Exception {
        String docs = mockMvc.perform(get("/v3/api-docs")).andReturn()
                .getResponse().getContentAsString();
        Map<String, Object> operacion = JsonPath.read(docs,
                "$.paths['/api/v1/reportes/matriculas-programa-semestre'].get");
        assertThat((List<Map<String, Object>>) operacion.get("parameters"))
                .extracting(p -> p.get("name"))
                .containsExactlyInAnyOrder("semestreId", "programaId", "facultadId");
        assertThat((Map<String, Object>) operacion.get("responses"))
                .containsKeys("200", "400", "401", "403", "404");

        assertThat((List<Map<String, Object>>) operacion.get("security"))
                .anySatisfy(s -> assertThat(s).containsKey(OpenApiConfig.ESQUEMA_BEARER));
    }
}
