package co.edu.uco.sigra.estudiantes.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MiProcesoApiDocumentacionIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentaLasCuatroConsultasDelProceso() throws Exception {
        String doc = mockMvc.perform(get("/v3/api-docs")).andReturn()
                .getResponse().getContentAsString();
        for (String ruta : new String[]{"/api/v1/estudiantes/me/materias",
                "/api/v1/estudiantes/me/materias/{matriculaId}",
                "/api/v1/estudiantes/me/progreso",
                "/api/v1/estudiantes/me/historial"}) {
            Map<String, Object> operacion = JsonPath.read(doc, "$.paths['" + ruta + "'].get");
            assertThat(operacion).containsKeys("responses", "security");
        }
    }
}
