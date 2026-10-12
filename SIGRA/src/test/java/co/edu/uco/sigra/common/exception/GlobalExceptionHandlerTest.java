package co.edu.uco.sigra.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas del manejador global en modo standalone, con un controlador de prueba que lanza
 * las excepciones. Verifica que las respuestas no expongan detalles técnicos (RNF-17).
 */
@DisplayName("Manejador global de errores")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/error-inesperado")
        String errorInesperado() {
            throw new RuntimeException("SQL: select * from tabla_secreta");
        }

        @GetMapping("/prueba/acceso-denegado")
        String accesoDenegado() {
            throw new AccessDeniedException("Access Denied");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Un error inesperado devuelve 500 con mensaje genérico y sin el detalle técnico")
    void errorInesperadoNoExponeDetalleTecnico() throws Exception {
        mockMvc.perform(get("/prueba/error-inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado en el servidor"))
                .andExpect(jsonPath("$.detalles").doesNotExist())
                .andExpect(content().string(not(containsString("SQL"))))
                .andExpect(content().string(not(containsString("tabla_secreta"))));
    }

    @Test
    @DisplayName("Un acceso denegado devuelve 403 con el mensaje del sistema y sin el mensaje original")
    void accesoDenegadoNoExponeMensajeOriginal() throws Exception {
        mockMvc.perform(get("/prueba/acceso-denegado"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.mensaje").value("No tiene permisos suficientes para realizar esta acción"))
                .andExpect(content().string(not(containsString("Access Denied"))));
    }
}
