package co.edu.uco.sigra.auth.security;

import co.edu.uco.sigra.calificaciones.service.CalificacionService;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.auth.service.AuthService;
import co.edu.uco.sigra.common.service.TipoDocumentoService;
import co.edu.uco.sigra.estudiantes.mapper.MatriculaMapper;
import co.edu.uco.sigra.estudiantes.service.EstudianteService;
import co.edu.uco.sigra.estudiantes.service.MatriculaService;
import co.edu.uco.sigra.profesores.service.ProfesorService;
import co.edu.uco.sigra.programas.service.ProgramaService;
import co.edu.uco.sigra.resultadosaprendizaje.service.ResultadoAprendizajeService;
import co.edu.uco.sigra.semestres.service.SemestreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guardia arquitectónica basada en el registro real de Spring MVC: una ruta nueva no puede quedar
 * solo detrás de {@code authenticated()} ni escapar por ubicarse en otro paquete.
 */
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
class PrivateEndpointAuthorizationArchitectureTest {

    private static final String PUBLIC_LOGIN = "POST /api/v1/auth/login";

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    // La prueba necesita levantar todos los controladores, pero no sus servicios ni PostgreSQL.
    @MockitoBean
    private AsignaturaService asignaturaService;

    @MockitoBean
    private CalificacionService calificacionService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TipoDocumentoService tipoDocumentoService;

    @MockitoBean
    private CurrentUser currentUser;

    @MockitoBean
    private EstudianteService estudianteService;

    @MockitoBean
    private MatriculaService matriculaService;

    @MockitoBean
    private MatriculaMapper matriculaMapper;

    @MockitoBean
    private ProfesorService profesorService;

    @MockitoBean
    private ProgramaService programaService;

    @MockitoBean
    private ResultadoAprendizajeService resultadoAprendizajeService;

    @MockitoBean
    private SemestreService semestreService;

    @Test
    void todosLosMappingsPrivadosDelContextoRealDeclaranPreAuthorize() {
        List<String> privadosSinRegla = new ArrayList<>();
        List<String> mappingsAplicacion = new ArrayList<>();
        List<String> mappingsPublicos = new ArrayList<>();

        for (var entry : handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo mapping = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();
            for (String path : mapping.getPatternValues()) {
                Set<RequestMethod> methods = mapping.getMethodsCondition().getMethods();
                if (methods.isEmpty()) {
                    verificarMapping(path, null, handlerMethod, mappingsAplicacion, mappingsPublicos,
                            privadosSinRegla);
                } else {
                    for (RequestMethod method : methods) {
                        verificarMapping(path, method, handlerMethod, mappingsAplicacion, mappingsPublicos,
                                privadosSinRegla);
                    }
                }
            }
        }

        assertThat(mappingsAplicacion)
                .as("Debe existir al menos una ruta de la aplicación registrada por Spring MVC")
                .isNotEmpty();
        assertThat(mappingsPublicos)
                .as("La lista de excepciones públicas debe ser explícita y mínima")
                .containsExactly(PUBLIC_LOGIN);
        assertThat(privadosSinRegla)
                .as("Toda ruta privada real debe tener @PreAuthorize explícito")
                .isEmpty();
    }

    private void verificarMapping(String path, RequestMethod method, HandlerMethod handlerMethod,
                                  List<String> mappingsAplicacion, List<String> mappingsPublicos,
                                  List<String> privadosSinRegla) {
        if (!path.startsWith("/api/")) {
            return;
        }
        String route = (method == null ? "ANY" : method.name()) + " " + path;
        mappingsAplicacion.add(route);
        if (PUBLIC_LOGIN.equals(route)) {
            mappingsPublicos.add(route);
            return;
        }
        if (AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), PreAuthorize.class) == null
                && AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), PreAuthorize.class) == null) {
            privadosSinRegla.add(handlerMethod.getBeanType().getName() + "#"
                    + handlerMethod.getMethod().getName() + " -> " + route);
        }
    }
}
