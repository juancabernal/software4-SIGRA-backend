package co.edu.uco.sigra.semestres.integracion;

import co.edu.uco.sigra.semestres.config.SemestreConfig;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// INTEGRATION: contexto completo + PostgreSQL real (SIGRA/.env). Ejecutar con: .\gradlew.bat integrationTest
//
// Sin @Transactional: el servicio confirma de verdad y cada caso verifica la respuesta HTTP y la fila real.
//
// Fecha controlada: el reloj del módulo (bean "relojSemestres") se reemplaza por un mock, así "hoy" es una
// fecha fija del año 2190 y se puede mover para probar los cambios de estado sin proceso programado.
//
// Aislamiento de datos: todos los semestres de estas pruebas usan códigos de los años RESERVADOS 2190-2199
// (ningún dato real ni seed del equipo usa ese rango), así nunca se solapan con semestres existentes.
// @BeforeEach y @AfterEach borran SOLO los códigos 219X-1 / 219X-2.
//
// IMPORTANTE: estas pruebas NO llaman a GET /api/v1/semestres (listar). Listar recalcula y guarda el estado de
// TODOS los semestres de la base; con el reloj en 2190 dejaría INACTIVOS los semestres reales de tu base local.
// Por eso solo se consulta por código, que únicamente toca la fila consultada.
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class SemestreIntegracionIT {

    private static final String URL = "/api/v1/semestres";
    private static final Pattern CODIGO_RESERVADO = Pattern.compile("^219\\d-[12]$");
    private static final LocalDate HOY = LocalDate.of(2190, 1, 10);

    @Autowired private MockMvc mockMvc;
    @Autowired private SemestreRepository semestreRepository;

    @MockitoBean(name = SemestreConfig.RELOJ_SEMESTRES)
    private Clock reloj;

    @BeforeEach
    void preparar() {
        limpiar();
        fijarHoy(HOY);
    }

    @AfterEach
    void limpiar() {
        semestreRepository.deleteAll(semestreRepository.findAll().stream()
                .filter(s -> CODIGO_RESERVADO.matcher(s.getCodigo()).matches())
                .toList());
    }

    // ---------------------------------------------------------------- IT-01

    @Test
    @DisplayName("IT-01 Registrar: el estado se calcula por fechas y se guarda; el código duplicado da 409")
    void it01Registrar() throws Exception {
        // Empieza hoy (2190-01-10): queda ACTIVO.
        MvcResult activo = enviar(post(URL), json("2190-1", "2190-01-10", "2190-06-10"));
        String cuerpoActivo = cuerpo(activo);
        assertThat(activo.getResponse().getStatus()).isEqualTo(201);
        assertThat((String) JsonPath.read(cuerpoActivo, "$.codigo")).isEqualTo("2190-1");
        assertThat((String) JsonPath.read(cuerpoActivo, "$.fechaInicio")).isEqualTo("2190-01-10");
        assertThat((String) JsonPath.read(cuerpoActivo, "$.fechaFin")).isEqualTo("2190-06-10");
        assertThat((String) JsonPath.read(cuerpoActivo, "$.estado")).isEqualTo("ACTIVO");

        Semestre enBase = semestreRepository.findById(UUID.fromString(JsonPath.read(cuerpoActivo, "$.id")))
                .orElseThrow();
        assertThat(enBase.getCodigo()).isEqualTo("2190-1");
        assertThat(enBase.getFechaInicio()).isEqualTo(LocalDate.of(2190, 1, 10));
        assertThat(enBase.getFechaFin()).isEqualTo(LocalDate.of(2190, 6, 10));
        assertThat(enBase.getEstado()).isEqualTo(EstadoSemestre.ACTIVO);

        // Empieza en el futuro: queda INACTIVO.
        MvcResult futuro = enviar(post(URL), json("2190-2", "2190-07-20", "2190-12-05"));
        assertThat(futuro.getResponse().getStatus()).isEqualTo(201);
        assertThat((String) JsonPath.read(cuerpo(futuro), "$.estado")).isEqualTo("INACTIVO");
        assertThat(enBase("2190-2").getEstado()).isEqualTo(EstadoSemestre.INACTIVO);

        // Consultar por código devuelve lo guardado.
        MvcResult consulta = enviar(get(URL + "/2190-2"), null);
        assertThat(consulta.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(consulta), "$.codigo")).isEqualTo("2190-2");
        assertThat((String) JsonPath.read(cuerpo(consulta), "$.fechaInicio")).isEqualTo("2190-07-20");

        // Código repetido (con fechas que no se cruzan): 409 y no se crea otra fila.
        MvcResult duplicado = enviar(post(URL), json("2190-1", "2190-12-20", "2190-12-30"));
        assertThat(duplicado.getResponse().getStatus()).isEqualTo(409);
        assertSinDetallesTecnicos(cuerpo(duplicado));
        assertThat(contarReservados()).isEqualTo(2);
        assertThat(enBase("2190-1").getFechaInicio()).isEqualTo(LocalDate.of(2190, 1, 10));
    }

    // ---------------------------------------------------------------- IT-02

    static Stream<Arguments> entradasInvalidas() {
        return Stream.of(
                Arguments.of("código con periodo 3", json("2190-3", "2190-02-01", "2190-06-01")),
                Arguments.of("código con año de 3 dígitos", json("219-1", "2190-02-01", "2190-06-01")),
                Arguments.of("código con letras", json("AAAA-1", "2190-02-01", "2190-06-01")),
                Arguments.of("código vacío", json("", "2190-02-01", "2190-06-01")),
                Arguments.of("código nulo", json(null, "2190-02-01", "2190-06-01")),
                Arguments.of("fechaInicio nula", json("2190-1", null, "2190-06-01")),
                Arguments.of("fechaFin nula", json("2190-1", "2190-02-01", null)),
                Arguments.of("fechaFin igual a fechaInicio", json("2190-1", "2190-02-01", "2190-02-01")),
                Arguments.of("fechaFin anterior a fechaInicio", json("2190-1", "2190-06-01", "2190-02-01")),
                Arguments.of("fechaInicio en el pasado", json("2190-1", "2190-01-09", "2190-06-01")),
                Arguments.of("año del código distinto al de fechaInicio", json("2191-1", "2190-02-01", "2190-06-01")),
                Arguments.of("fecha inexistente", json("2190-1", "2190-13-45", "2190-06-01")),
                Arguments.of("fecha con formato dd/MM/yyyy", json("2190-1", "01/02/2190", "2190-06-01")));
    }

    @ParameterizedTest(name = "IT-02 {0} -> 400 sin guardar nada")
    @MethodSource("entradasInvalidas")
    @DisplayName("IT-02 Entradas inválidas: 400, sin detalles técnicos y no se guarda nada")
    void it02EntradasInvalidas(String caso, String cuerpoPeticion) throws Exception {
        long antes = semestreRepository.count();

        MvcResult r = enviar(post(URL), cuerpoPeticion);

        assertThat(r.getResponse().getStatus()).as(caso).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(r));
        assertThat(semestreRepository.count()).as(caso).isEqualTo(antes);
        assertThat(contarReservados()).as(caso).isZero();
    }

    // ---------------------------------------------------------------- IT-03

    static Stream<Arguments> rangosQueSeCruzan() {
        // Semestre base: 2190-02-01 a 2190-06-30. Los extremos cuentan como cruce.
        return Stream.of(
                Arguments.of("termina el día que empieza el base", "2190-01-15", "2190-02-01"),
                Arguments.of("empieza el día que termina el base", "2190-06-30", "2190-08-01"),
                Arguments.of("cruce parcial al final", "2190-05-01", "2190-08-01"),
                Arguments.of("contenido en el base", "2190-03-01", "2190-04-01"),
                Arguments.of("contiene al base", "2190-01-20", "2190-07-15"),
                Arguments.of("mismas fechas", "2190-02-01", "2190-06-30"));
    }

    @ParameterizedTest(name = "IT-03 {0} -> 409")
    @MethodSource("rangosQueSeCruzan")
    @DisplayName("IT-03 Sin solapamiento: cruces, contención y extremos dan 409; el base no cambia")
    void it03SinSolapamiento(String caso, String inicio, String fin) throws Exception {
        assertThat(enviar(post(URL), json("2190-1", "2190-02-01", "2190-06-30")).getResponse().getStatus())
                .isEqualTo(201);

        MvcResult cruce = enviar(post(URL), json("2190-2", inicio, fin));
        assertThat(cruce.getResponse().getStatus()).as(caso).isEqualTo(409);
        assertSinDetallesTecnicos(cuerpo(cruce));
        assertThat(semestreRepository.findByCodigo("2190-2")).as(caso).isEmpty();
        Semestre base = enBase("2190-1");
        assertThat(base.getFechaInicio()).isEqualTo(LocalDate.of(2190, 2, 1));
        assertThat(base.getFechaFin()).isEqualTo(LocalDate.of(2190, 6, 30));

        // Contiguo, sin compartir días: sí se permite.
        MvcResult contiguo = enviar(post(URL), json("2190-2", "2190-07-01", "2190-11-30"));
        assertThat(contiguo.getResponse().getStatus()).as(caso).isEqualTo(201);
        assertThat(contarReservados()).isEqualTo(2);
    }

    // ---------------------------------------------------------------- IT-04

    @Test
    @DisplayName("IT-04 Extender fechaFin: solo hacia adelante, sin cruzarse y no si el semestre ya terminó")
    void it04ExtenderFechaFin() throws Exception {
        assertThat(enviar(post(URL), json("2190-1", "2190-01-10", "2190-05-31")).getResponse().getStatus())
                .isEqualTo(201);
        assertThat(enviar(post(URL), json("2190-2", "2190-07-01", "2190-11-30")).getResponse().getStatus())
                .isEqualTo(201);

        MvcResult extender = enviar(patch(URL + "/2190-1"), jsonFin("2190-06-15"));
        assertThat(extender.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(extender), "$.fechaFin")).isEqualTo("2190-06-15");
        assertThat((String) JsonPath.read(cuerpo(extender), "$.estado")).isEqualTo("ACTIVO");
        assertThat(enBase("2190-1").getFechaFin()).isEqualTo(LocalDate.of(2190, 6, 15));
        assertThat(enBase("2190-1").getFechaInicio()).isEqualTo(LocalDate.of(2190, 1, 10));

        assertRechazoSinCambios(jsonFin("2190-06-01"), 400, "acortar");
        assertRechazoSinCambios(jsonFin("2190-06-15"), 400, "misma fecha");
        assertRechazoSinCambios(jsonFin("2190-07-01"), 409, "cruzarse con 2190-2");
        assertRechazoSinCambios("{}", 400, "sin fechaFin");

        MvcResult inexistente = enviar(patch(URL + "/2199-2"), jsonFin("2199-12-01"));
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(inexistente));

        // 2190-1 terminó el 2190-06-15: ya no se puede modificar.
        fijarHoy(LocalDate.of(2190, 6, 16));
        assertRechazoSinCambios(jsonFin("2190-06-25"), 400, "semestre terminado");

        // 2190-2 aún no empieza: se puede extender y sigue INACTIVO.
        MvcResult futuro = enviar(patch(URL + "/2190-2"), jsonFin("2190-12-15"));
        assertThat(futuro.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(futuro), "$.estado")).isEqualTo("INACTIVO");
        assertThat(enBase("2190-2").getFechaFin()).isEqualTo(LocalDate.of(2190, 12, 15));
    }

    // ---------------------------------------------------------------- IT-05

    @Test
    @DisplayName("IT-05 Estado por fechas: al consultar se recalcula y se guarda en la base (extremos incluidos)")
    void it05EstadoSeRecalculaYSeGuarda() throws Exception {
        assertThat(enviar(post(URL), json("2190-2", "2190-07-01", "2190-11-30")).getResponse().getStatus())
                .isEqualTo(201);
        assertThat(enBase("2190-2").getEstado()).isEqualTo(EstadoSemestre.INACTIVO);

        assertEstadoAlConsultar(LocalDate.of(2190, 6, 30), "INACTIVO");  // un día antes
        assertEstadoAlConsultar(LocalDate.of(2190, 7, 1), "ACTIVO");     // primer día
        assertEstadoAlConsultar(LocalDate.of(2190, 11, 30), "ACTIVO");   // último día
        assertEstadoAlConsultar(LocalDate.of(2190, 12, 1), "INACTIVO");  // un día después

        MvcResult inexistente = enviar(get(URL + "/2199-1"), null);
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(inexistente));
    }

    // ---------------------------------------------------------------- utilidades

    private void fijarHoy(LocalDate fecha) {
        when(reloj.getZone()).thenReturn(SemestreConfig.ZONA_COLOMBIA);
        when(reloj.instant()).thenReturn(fecha.atTime(LocalTime.NOON).atZone(SemestreConfig.ZONA_COLOMBIA).toInstant());
    }

    private void assertRechazoSinCambios(String cuerpoPeticion, int estadoHttp, String caso) throws Exception {
        LocalDate finAntes = enBase("2190-1").getFechaFin();
        MvcResult r = enviar(patch(URL + "/2190-1"), cuerpoPeticion);
        assertThat(r.getResponse().getStatus()).as(caso).isEqualTo(estadoHttp);
        assertSinDetallesTecnicos(cuerpo(r));
        assertThat(enBase("2190-1").getFechaFin()).as(caso).isEqualTo(finAntes);
    }

    private void assertEstadoAlConsultar(LocalDate hoy, String esperado) throws Exception {
        fijarHoy(hoy);
        MvcResult r = enviar(get(URL + "/2190-2"), null);
        assertThat(r.getResponse().getStatus()).as("hoy = " + hoy).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(r), "$.estado")).as("hoy = " + hoy).isEqualTo(esperado);
        assertThat(enBase("2190-2").getEstado().name()).as("columna estado con hoy = " + hoy).isEqualTo(esperado);
    }

    private MvcResult enviar(MockHttpServletRequestBuilder peticion, String json) throws Exception {
        if (json != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8).content(json);
        }
        return mockMvc.perform(peticion).andReturn();
    }

    private static String cuerpo(MvcResult r) {
        return new String(r.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private static void assertSinDetallesTecnicos(String cuerpo) {
        assertThat(cuerpo).doesNotContain("Exception", "org.", "java.", "SQL", "trace", "constraint");
    }

    private Semestre enBase(String codigo) {
        return semestreRepository.findByCodigo(codigo).orElseThrow();
    }

    private long contarReservados() {
        return semestreRepository.findAll().stream()
                .filter(s -> CODIGO_RESERVADO.matcher(s.getCodigo()).matches())
                .count();
    }

    private static String json(String codigo, String fechaInicio, String fechaFin) {
        return "{\"codigo\":" + texto(codigo) + ",\"fechaInicio\":" + texto(fechaInicio)
                + ",\"fechaFin\":" + texto(fechaFin) + "}";
    }

    private static String jsonFin(String fechaFin) {
        return "{\"fechaFin\":" + texto(fechaFin) + "}";
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }
}
