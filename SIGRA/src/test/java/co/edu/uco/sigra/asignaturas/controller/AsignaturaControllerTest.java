package co.edu.uco.sigra.asignaturas.controller;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaFiltroDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaUpdateDTO;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaExceptionHandler;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.exception.CodigoAsignaturaDuplicadoException;
import co.edu.uco.sigra.asignaturas.exception.FiltroInvalidoException;
import co.edu.uco.sigra.asignaturas.exception.ProgramaInactivoException;
import co.edu.uco.sigra.asignaturas.exception.ProgramaNoEncontradoException;
import co.edu.uco.sigra.asignaturas.exception.RangoRaInvalidoException;
import co.edu.uco.sigra.asignaturas.exception.TransicionEstadoInvalidaException;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de la capa web en modo standalone: no levanta contexto Spring, base de datos ni seguridad.
 * Verifica rutas, códigos HTTP, validaciones de los DTO y el formato de error del módulo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Controlador de asignaturas")
class AsignaturaControllerTest {

    private static final String URL = "/api/v1/asignaturas";
    private static final UUID ID = UUID.randomUUID();
    private static final UUID ID_PROGRAMA = UUID.randomUUID();
    private static final String URL_ID = URL + "/" + ID;
    private static final String CODIGO = "ISW4";
    private static final String NOMBRE = "Ingeniería de Software IV";

    private static final String JSON_MENSAJE = "$.mensaje";
    private static final String JSON_STATUS = "$.status";
    private static final String JSON_ESTADO = "$.estado";
    private static final String MENSAJE_VALIDACION = "Los datos de entrada no cumplen con las validaciones requeridas";

    @Mock
    private AsignaturaService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new AsignaturaController(service))
                .setControllerAdvice(new AsignaturaExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private static AsignaturaResponseDTO respuesta(String codigo, EstadoAsignatura estado, long cantidadRa) {
        return new AsignaturaResponseDTO(ID, codigo, NOMBRE, ID_PROGRAMA, "Ingeniería de Sistemas", estado, cantidadRa);
    }

    private static String cuerpoRegistro(String codigo, String nombre, UUID programaId) {
        String programa = programaId == null ? "null" : "\"" + programaId + "\"";
        return """
                {"codigo":"%s","nombre":"%s","programaId":%s}
                """.formatted(codigo, nombre, programa);
    }

    private ResultActions registrar(String cuerpo) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions modificar(String cuerpo) throws Exception {
        return mockMvc.perform(put(URL_ID).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    @Nested
    @DisplayName("POST registrar")
    class Registrar {

        @Test
        @DisplayName("Con datos válidos devuelve 201 y la asignatura en BORRADOR")
        void valido() throws Exception {
            when(service.registrarAsignatura(any(AsignaturaRequestDTO.class)))
                    .thenReturn(respuesta(CODIGO, EstadoAsignatura.BORRADOR, 0));

            registrar(cuerpoRegistro(CODIGO, NOMBRE, ID_PROGRAMA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.codigo").value(CODIGO))
                    .andExpect(jsonPath(JSON_ESTADO).value("BORRADOR"))
                    .andExpect(jsonPath("$.cantidadRa").value(0));
        }

        @Test
        @DisplayName("Un código con guion es válido y devuelve 201")
        void codigoConGuion() throws Exception {
            when(service.registrarAsignatura(any(AsignaturaRequestDTO.class)))
                    .thenReturn(respuesta("IS-401", EstadoAsignatura.BORRADOR, 0));

            registrar(cuerpoRegistro("IS-401", NOMBRE, ID_PROGRAMA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.codigo").value("IS-401"));
        }

        @Test
        @DisplayName("Sin programa devuelve 400 y no llama al servicio")
        void sinPrograma() throws Exception {
            registrar(cuerpoRegistro(CODIGO, NOMBRE, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value(MENSAJE_VALIDACION))
                    .andExpect(jsonPath("$.detalles[0]").value("programaId: Debe seleccionar un programa académico"));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con código y nombre vacíos devuelve 400 y no llama al servicio")
        void codigoYNombreVacios() throws Exception {
            registrar(cuerpoRegistro("", "", ID_PROGRAMA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_STATUS).value(400))
                    .andExpect(jsonPath("$.detalles").isArray());
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con un nombre de más de 100 caracteres devuelve 400")
        void nombreDemasiadoLargo() throws Exception {
            registrar(cuerpoRegistro(CODIGO, "a".repeat(101), ID_PROGRAMA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles[0]").value(startsWith("nombre")));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con caracteres inválidos en el código devuelve 400")
        void codigoInvalido() throws Exception {
            registrar(cuerpoRegistro("IS W4!", NOMBRE, ID_PROGRAMA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles[0]").value(startsWith("codigo")));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Con código duplicado devuelve 409 con el mensaje")
        void codigoDuplicado() throws Exception {
            when(service.registrarAsignatura(any(AsignaturaRequestDTO.class)))
                    .thenThrow(new CodigoAsignaturaDuplicadoException(CODIGO));

            registrar(cuerpoRegistro(CODIGO, NOMBRE, ID_PROGRAMA))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("CONFLICT"))
                    .andExpect(jsonPath(JSON_MENSAJE).value("Ya existe una asignatura con el código ISW4."));
        }

        @Test
        @DisplayName("Con un programa inactivo devuelve 400")
        void programaInactivo() throws Exception {
            when(service.registrarAsignatura(any(AsignaturaRequestDTO.class)))
                    .thenThrow(new ProgramaInactivoException("Ingeniería de Sistemas"));

            registrar(cuerpoRegistro(CODIGO, NOMBRE, ID_PROGRAMA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value(containsString("está inactivo")));
        }

        @Test
        @DisplayName("Con un programa inexistente devuelve 404")
        void programaInexistente() throws Exception {
            when(service.registrarAsignatura(any(AsignaturaRequestDTO.class)))
                    .thenThrow(new ProgramaNoEncontradoException(ID_PROGRAMA));

            registrar(cuerpoRegistro(CODIGO, NOMBRE, ID_PROGRAMA))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("Con un JSON mal formado devuelve 400 con un mensaje genérico")
        void jsonMalFormado() throws Exception {
            registrar("{\"codigo\": \"ISW4\", ")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value("El cuerpo de la petición no es válido."))
                    .andExpect(jsonPath("$.detalles").doesNotExist());
            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("GET consultar")
    class Consultar {

        @Test
        @DisplayName("Sin filtros devuelve 200 con la lista")
        void sinFiltros() throws Exception {
            when(service.consultar(new AsignaturaFiltroDTO(null, null, null, null, null)))
                    .thenReturn(List.of(respuesta(CODIGO, EstadoAsignatura.ACTIVA, 6),
                            respuesta("BD2", EstadoAsignatura.BORRADOR, 0)));

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].codigo").value(CODIGO))
                    .andExpect(jsonPath("$[0].cantidadRa").value(6));
        }

        @Test
        @DisplayName("Sin registros devuelve 200 y una lista vacía")
        void sinRegistros() throws Exception {
            when(service.consultar(any(AsignaturaFiltroDTO.class))).thenReturn(List.of());

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Envía al servicio todos los filtros recibidos")
        void conTodosLosFiltros() throws Exception {
            when(service.consultar(any(AsignaturaFiltroDTO.class))).thenReturn(List.of());

            mockMvc.perform(get(URL)
                            .param("texto", "software")
                            .param("programaId", ID_PROGRAMA.toString())
                            .param("estado", "ACTIVA")
                            .param("raMin", "5")
                            .param("raMax", "7"))
                    .andExpect(status().isOk());

            ArgumentCaptor<AsignaturaFiltroDTO> captor = ArgumentCaptor.forClass(AsignaturaFiltroDTO.class);
            verify(service).consultar(captor.capture());
            assertThat(captor.getValue())
                    .isEqualTo(new AsignaturaFiltroDTO("software", ID_PROGRAMA, EstadoAsignatura.ACTIVA, 5, 7));
        }

        @Test
        @DisplayName("Un programaId que no es UUID devuelve 400")
        void programaIdInvalido() throws Exception {
            mockMvc.perform(get(URL).param("programaId", "no-es-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value("El valor del parámetro 'programaId' no es válido."));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Un estado desconocido devuelve 400 y no 500")
        void estadoDesconocido() throws Exception {
            mockMvc.perform(get(URL).param("estado", "ARCHIVADA"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value("El valor del parámetro 'estado' no es válido."));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("Un filtro inválido devuelve 400 con el mensaje")
        void filtroInvalido() throws Exception {
            when(service.consultar(any(AsignaturaFiltroDTO.class)))
                    .thenThrow(new FiltroInvalidoException("La cantidad mínima de RA no puede ser mayor que la máxima."));

            mockMvc.perform(get(URL).param("raMin", "7").param("raMax", "5"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value("La cantidad mínima de RA no puede ser mayor que la máxima."));
        }
    }

    @Nested
    @DisplayName("GET por id")
    class Obtener {

        @Test
        @DisplayName("Existente devuelve 200")
        void existente() throws Exception {
            when(service.obtenerAsignatura(ID)).thenReturn(respuesta(CODIGO, EstadoAsignatura.ACTIVA, 5));

            mockMvc.perform(get(URL_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ID.toString()))
                    .andExpect(jsonPath("$.programaNombre").value("Ingeniería de Sistemas"));
        }

        @Test
        @DisplayName("Inexistente devuelve 404")
        void inexistente() throws Exception {
            when(service.obtenerAsignatura(ID)).thenThrow(new AsignaturaNoEncontradaException(ID));

            mockMvc.perform(get(URL_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath(JSON_MENSAJE).value("No existe una asignatura con id " + ID + "."));
        }
    }

    @Nested
    @DisplayName("PUT modificar")
    class Modificar {

        @Test
        @DisplayName("Con un nombre válido devuelve 200")
        void valido() throws Exception {
            when(service.modificarAsignatura(eq(ID), any(AsignaturaUpdateDTO.class)))
                    .thenReturn(respuesta(CODIGO, EstadoAsignatura.BORRADOR, 0));

            modificar("{\"nombre\":\"" + NOMBRE + "\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombre").value(NOMBRE));
            verify(service).modificarAsignatura(ID, new AsignaturaUpdateDTO(NOMBRE));
        }

        @Test
        @DisplayName("Con el nombre vacío devuelve 400 y no llama al servicio")
        void nombreVacio() throws Exception {
            modificar("{\"nombre\":\"\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detalles[0]").value(startsWith("nombre")));
            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("El DTO de modificación solo admite el nombre: código y programa son inmutables")
        void soloNombreEsModificable() {
            assertThat(Arrays.stream(AsignaturaUpdateDTO.class.getRecordComponents()).map(RecordComponent::getName))
                    .containsExactly("nombre");
        }
    }

    @Nested
    @DisplayName("PATCH activar")
    class Activar {

        private ResultActions activar() throws Exception {
            return mockMvc.perform(patch(URL_ID + "/activar"));
        }

        @Test
        @DisplayName("Con RA suficientes devuelve 200 y la asignatura ACTIVA")
        void ok() throws Exception {
            when(service.activar(ID)).thenReturn(respuesta(CODIGO, EstadoAsignatura.ACTIVA, 5));

            activar()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_ESTADO).value("ACTIVA"));
        }

        @Test
        @DisplayName("Con menos de 5 RA devuelve 400 indicando el mínimo")
        void pocosRa() throws Exception {
            when(service.activar(ID)).thenThrow(RangoRaInvalidoException.pocosRa(5, 4));

            activar()
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value(containsString("al menos 5")));
        }

        @Test
        @DisplayName("Con más de 7 RA devuelve 400 indicando el máximo")
        void demasiadosRa() throws Exception {
            when(service.activar(ID)).thenThrow(RangoRaInvalidoException.demasiadosRa(7, 8));

            activar()
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(JSON_MENSAJE).value(containsString("máximo 7")));
        }

        @Test
        @DisplayName("Desde un estado inválido devuelve 409")
        void estadoInvalido() throws Exception {
            when(service.activar(ID)).thenThrow(new TransicionEstadoInvalidaException(EstadoAsignatura.ACTIVA,
                    "activar", "Solo se pueden activar asignaturas en estado BORRADOR."));

            activar()
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath(JSON_MENSAJE).value(containsString("No se puede activar")));
        }

        @Test
        @DisplayName("Inexistente devuelve 404")
        void inexistente() throws Exception {
            when(service.activar(ID)).thenThrow(new AsignaturaNoEncontradaException(ID));

            activar().andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH inactivar")
    class Inactivar {

        private ResultActions inactivar() throws Exception {
            return mockMvc.perform(patch(URL_ID + "/inactivar"));
        }

        @Test
        @DisplayName("Desde ACTIVA devuelve 200 y la asignatura INACTIVA")
        void ok() throws Exception {
            when(service.inactivar(ID)).thenReturn(respuesta(CODIGO, EstadoAsignatura.INACTIVA, 0));

            inactivar()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(JSON_ESTADO).value("INACTIVA"));
        }

        @Test
        @DisplayName("Inexistente devuelve 404")
        void inexistente() throws Exception {
            when(service.inactivar(ID)).thenThrow(new AsignaturaNoEncontradaException(ID));

            inactivar().andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Desde un estado inválido devuelve 409")
        void estadoInvalido() throws Exception {
            when(service.inactivar(ID)).thenThrow(new TransicionEstadoInvalidaException(EstadoAsignatura.BORRADOR,
                    "inactivar", "Solo se pueden inactivar asignaturas en estado ACTIVA."));

            inactivar().andExpect(status().isConflict());
        }
    }

    @Test
    @DisplayName("Un error inesperado no lo convierte este manejador: lo atiende el manejador global")
    void errorInesperadoLoAtiendeElManejadorGlobal() {
        RuntimeException original = new RuntimeException("error inesperado");
        when(service.obtenerAsignatura(ID)).thenThrow(original);

        assertThatThrownBy(() -> mockMvc.perform(get(URL_ID)))
                .rootCause()
                .isSameAs(original);
    }
}
