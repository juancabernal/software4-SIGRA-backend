package co.edu.uco.sigra.asignaturas.controller;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.ReglasEntrada;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaExceptionHandler;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.common.exception.GlobalExceptionHandler;
import co.edu.uco.sigra.common.exception.ManejadorErroresTransversal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Validación de entrada vista desde HTTP. El controlador se envuelve con el mismo interceptor de
 * validación de métodos que aplica Spring en ejecución por {@code @Validated}; así los parámetros
 * fuera de rango llegan como ConstraintViolationException al manejador transversal, igual que en vivo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Validación de entrada del controlador de asignaturas")
class AsignaturaControllerValidacionTest {

    private static final String URL = "/api/v1/asignaturas";
    private static final String MENSAJE_PARAMETROS =
            "Los parámetros de la petición no cumplen con las validaciones requeridas";
    private static final String MENSAJE_CUERPO = "Los datos de entrada no cumplen con las validaciones requeridas";

    @Mock
    private AsignaturaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ProxyFactory fabrica = new ProxyFactory(new AsignaturaController(service));
        fabrica.setProxyTargetClass(true);
        fabrica.addAdvice(new MethodValidationInterceptor((jakarta.validation.Validator) validator));
        AsignaturaController conValidacion = (AsignaturaController) fabrica.getProxy();

        mockMvc = MockMvcBuilders.standaloneSetup(conValidacion)
                .setControllerAdvice(new AsignaturaExceptionHandler(), new ManejadorErroresTransversal(),
                        new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private static String cuerpo(String codigo, String nombre) {
        return """
                {"codigo":%s,"nombre":%s,"programaId":"%s"}
                """.formatted(json(codigo), json(nombre), UUID.randomUUID());
    }

    private static String json(String valor) {
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Test
    @DisplayName("Un texto de búsqueda de 100 caracteres se acepta")
    void textoDe100() throws Exception {
        when(service.consultar(any(AsignaturaFiltroDTO.class))).thenReturn(List.of());

        mockMvc.perform(get(URL).param("texto", "a".repeat(100))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un texto de búsqueda de 101 caracteres devuelve 400 sin repetir el texto")
    void textoDe101() throws Exception {
        String texto = "q".repeat(101);
        mockMvc.perform(get(URL).param("texto", texto))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_PARAMETROS))
                .andExpect(jsonPath("$.detalles", hasItem("texto: " + ReglasEntrada.TEXTO_LONGITUD)))
                .andExpect(content().string(not(containsString(texto))));
        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "{0}={1} devuelve 400")
    @CsvSource({"raMin, -1, La cantidad de RA no puede ser negativa", "raMax, -5, La cantidad de RA no puede ser negativa",
            "raMin, 1000, La cantidad de RA no puede ser mayor que 999", "raMax, 1000, La cantidad de RA no puede ser mayor que 999"})
    @DisplayName("raMin y raMax fuera de 0 a 999 devuelven 400 con un mensaje claro")
    void raFueraDeRango(String parametro, String valor, String mensaje) throws Exception {
        mockMvc.perform(get(URL).param(parametro, valor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_PARAMETROS))
                .andExpect(jsonPath("$.detalles", hasItem(parametro + ": " + mensaje)));
        verifyNoInteractions(service);
    }

    @ParameterizedTest(name = "raMin=raMax={0} se acepta")
    @ValueSource(strings = {"0", "999"})
    @DisplayName("Los límites 0 y 999 son válidos")
    void limitesValidos(String valor) throws Exception {
        when(service.consultar(any(AsignaturaFiltroDTO.class))).thenReturn(List.of());

        mockMvc.perform(get(URL).param("raMin", valor).param("raMax", valor)).andExpect(status().isOk());
    }

    @ParameterizedTest(name = "código «{0}», nombre «{1}»")
    @CsvSource(delimiter = '|', value = {
            "AB                    | Nombre válido",
            "ABCDEFGHIJKLMNOPQRSTU | Nombre válido",
            "MAT 401               | Nombre válido",
            "MAT#401               | Nombre válido",
            "-MAT401               | Nombre válido",
            "MAT--401              | Nombre válido",
            "MAT-401               | ab",
            "MAT-401               | <script>alert(1)</script>",
    })
    @DisplayName("Registrar con código o nombre inválido devuelve 400 sin repetir el valor enviado")
    void registrarInvalido(String codigo, String nombre) throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo(codigo, nombre)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_CUERPO))
                .andExpect(content().string(not(containsString(codigo))))
                .andExpect(content().string(not(containsString(nombre))));
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Modificar con un nombre de 101 caracteres devuelve 400")
    void modificarNombreLargo() throws Exception {
        mockMvc.perform(put(URL + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"" + "n".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles", hasItem("nombre: " + ReglasEntrada.NOMBRE_LONGITUD)));
        verifyNoInteractions(service);
    }
}
