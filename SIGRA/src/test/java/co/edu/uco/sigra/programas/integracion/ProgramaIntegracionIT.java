package co.edu.uco.sigra.programas.integracion;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

// INTEGRATION: contexto completo + PostgreSQL real (SIGRA/.env). Ejecutar con: .\gradlew.bat integrationTest
//
// Sin @Transactional: el servicio confirma de verdad y cada caso verifica la respuesta HTTP y la fila real.
//
// Aislamiento de datos: todos los programas de estas pruebas usan el prefijo RESERVADO "ITG-" en el código
// y "ITG Programa " en el nombre (el mismo que usa AsignaturaIntegracionIT para sus programas de prueba).
// @BeforeEach y @AfterEach borran solo las filas con ese prefijo, en programas y en asignaturas.
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMINISTRADOR")
class ProgramaIntegracionIT {

    private static final String URL = "/api/v1/programas";
    private static final String PREFIJO_CODIGO = "ITG-";
    private static final String PREFIJO_NOMBRE = "ITG Programa ";

    @Autowired private MockMvc mockMvc;
    @Autowired private ProgramaAcademicoRepository programaRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;

    @BeforeEach
    void preparar() {
        limpiar();
    }

    @AfterEach
    void limpiar() {
        List<ProgramaAcademico> reservados = programaRepository.findAll().stream()
                .filter(p -> p.getCodigo().startsWith(PREFIJO_CODIGO))
                .toList();
        List<UUID> ids = reservados.stream().map(ProgramaAcademico::getId).toList();
        if (!ids.isEmpty()) {
            asignaturaRepository.deleteAll(asignaturaRepository.findAll().stream()
                    .filter(a -> ids.contains(a.getPrograma().getId()))
                    .toList());
        }
        programaRepository.deleteAll(reservados);
    }

    // ---------------------------------------------------------------- IT-01

    @Test
    @DisplayName("IT-01 Registrar: se guarda en ACTIVO; nombre y código duplicados dan 409 sin crear fila")
    void it01Registrar() throws Exception {
        MvcResult creado = enviar(post(URL), json(nombre("Uno"), "ITG-001"));
        String cuerpoCreado = cuerpo(creado);
        assertThat(creado.getResponse().getStatus()).isEqualTo(201);
        assertThat((String) JsonPath.read(cuerpoCreado, "$.nombre")).isEqualTo(nombre("Uno"));
        assertThat((String) JsonPath.read(cuerpoCreado, "$.codigo")).isEqualTo("ITG-001");
        assertThat((String) JsonPath.read(cuerpoCreado, "$.estado")).isEqualTo("ACTIVO");

        ProgramaAcademico enBase = programaRepository.findById(UUID.fromString(JsonPath.read(cuerpoCreado, "$.id")))
                .orElseThrow();
        assertThat(enBase.getNombre()).isEqualTo(nombre("Uno"));
        assertThat(enBase.getCodigo()).isEqualTo("ITG-001");
        assertThat(enBase.getEstado().name()).isEqualTo("ACTIVO");

        // Nombre duplicado (con mayúsculas distintas): 409, no se crea otra fila.
        MvcResult nombreDuplicado = enviar(post(URL), json(nombre("Uno").toUpperCase(), "ITG-002"));
        assertThat(nombreDuplicado.getResponse().getStatus()).isEqualTo(409);
        assertSinDetallesTecnicos(cuerpo(nombreDuplicado));
        assertThat(contarReservados()).isEqualTo(1);

        // Código duplicado (con mayúsculas distintas): 409, no se crea otra fila.
        MvcResult codigoDuplicado = enviar(post(URL), json(nombre("Dos"), "itg-001"));
        assertThat(codigoDuplicado.getResponse().getStatus()).isEqualTo(409);
        assertSinDetallesTecnicos(cuerpo(codigoDuplicado));
        assertThat(contarReservados()).isEqualTo(1);
    }

    // ---------------------------------------------------------------- IT-02

    static Stream<Arguments> entradasInvalidas() {
        return Stream.of(
                Arguments.of("nombre vacío", json("", "ITG-010")),
                Arguments.of("nombre nulo", json(null, "ITG-010")),
                Arguments.of("nombre de 101 caracteres", json("A".repeat(101), "ITG-010")),
                Arguments.of("código vacío", json(nombre("X"), "")),
                Arguments.of("código nulo", json(nombre("X"), null)),
                Arguments.of("código de 3 caracteres", json(nombre("X"), "ITG")),
                Arguments.of("código de 11 caracteres", json(nombre("X"), "ITG-0012345")),
                Arguments.of("código con espacio", json(nombre("X"), "ITG 001")),
                Arguments.of("código con símbolo", json(nombre("X"), "ITG-00#")));
    }

    @ParameterizedTest(name = "IT-02 {0} -> 400 sin guardar nada")
    @MethodSource("entradasInvalidas")
    @DisplayName("IT-02 Entradas inválidas: 400, sin detalles técnicos y no se guarda nada")
    void it02EntradasInvalidas(String caso, String cuerpoPeticion) throws Exception {
        long antes = programaRepository.count();

        MvcResult r = enviar(post(URL), cuerpoPeticion);

        assertThat(r.getResponse().getStatus()).as(caso).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(r));
        assertThat(programaRepository.count()).as(caso).isEqualTo(antes);
        assertThat(contarReservados()).as(caso).isZero();
    }

    // ---------------------------------------------------------------- IT-03

    @Test
    @DisplayName("IT-03 Consultar: filtros por nombre, código y estado contra datos reales; por id existente e inexistente")
    void it03Consultar() throws Exception {
        ProgramaAcademico activo = guardar(nombre("Derecho"), "ITG-D01");
        ProgramaAcademico otroActivo = guardar(nombre("Medicina"), "ITG-M01");
        ProgramaAcademico inactivo = guardarInactivo(nombre("Historia"), "ITG-H01");

        // Filtro por nombre parcial, insensible a mayúsculas. El texto de búsqueda incluye el
        // prefijo reservado para no depender de que la base esté libre de datos reales del equipo
        // (p. ej. un "Derecho Civil" real también coincidiría con solo "derech").
        MvcResult porNombre = enviar(get(URL + "?nombre=programa derech"), null);
        assertThat(porNombre.getResponse().getStatus()).isEqualTo(200);
        List<String> nombresEncontrados = JsonPath.read(cuerpo(porNombre), "$[*].nombre");
        assertThat(nombresEncontrados).containsExactly(activo.getNombre());

        // Filtro por código parcial.
        MvcResult porCodigo = enviar(get(URL + "?codigo=itg-m"), null);
        List<String> codigosEncontrados = JsonPath.read(cuerpo(porCodigo), "$[*].codigo");
        assertThat(codigosEncontrados).containsExactly(otroActivo.getCodigo());

        // Filtro por estado: hay datos reales inactivos en la base, así que nos quedamos solo
        // con los que llevan el prefijo reservado antes de comparar.
        MvcResult inactivos = enviar(get(URL + "?estado=INACTIVO"), null);
        List<String> nombresInactivos = JsonPath.read(cuerpo(inactivos), "$[*].nombre");
        List<String> nombresInactivosReservados = nombresInactivos.stream()
                .filter(n -> n.startsWith(PREFIJO_NOMBRE))
                .toList();
        assertThat(nombresInactivosReservados).containsExactly(inactivo.getNombre());

        // Combinado: nombre + estado, sin resultados.
        MvcResult combinadoSinResultados = enviar(get(URL + "?nombre=programa derech&estado=INACTIVO"), null);
        assertThat((List<?>) JsonPath.read(cuerpo(combinadoSinResultados), "$[*]")).isEmpty();

        // Consultar por id existente.
        MvcResult porId = enviar(get(URL + "/" + activo.getId()), null);
        assertThat(porId.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(porId), "$.codigo")).isEqualTo(activo.getCodigo());

        // Consultar por id inexistente.
        MvcResult inexistente = enviar(get(URL + "/" + UUID.randomUUID()), null);
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(inexistente));
    }

    // ---------------------------------------------------------------- IT-04

    @Test
    @DisplayName("IT-04 Modificar nombre: el código no cambia; nombre duplicado da 409; nombre inválido da 400; id inexistente da 404")
    void it04ModificarNombre() throws Exception {
        ProgramaAcademico uno = guardar(nombre("Uno"), "ITG-U01");
        ProgramaAcademico dos = guardar(nombre("Dos"), "ITG-U02");

        MvcResult modificado = enviar(put(URL + "/" + uno.getId()), jsonNombre(nombre("Uno Renombrado")));
        assertThat(modificado.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(modificado), "$.nombre")).isEqualTo(nombre("Uno Renombrado"));
        assertThat((String) JsonPath.read(cuerpo(modificado), "$.codigo")).isEqualTo("ITG-U01");

        ProgramaAcademico enBase = programaRepository.findById(uno.getId()).orElseThrow();
        assertThat(enBase.getNombre()).isEqualTo(nombre("Uno Renombrado"));
        assertThat(enBase.getCodigo()).isEqualTo("ITG-U01");

        // Nombre que ya usa otro programa: 409, sin cambios.
        MvcResult duplicado = enviar(put(URL + "/" + uno.getId()), jsonNombre(dos.getNombre()));
        assertThat(duplicado.getResponse().getStatus()).isEqualTo(409);
        assertSinDetallesTecnicos(cuerpo(duplicado));
        assertThat(programaRepository.findById(uno.getId()).orElseThrow().getNombre())
                .isEqualTo(nombre("Uno Renombrado"));

        // Nombre vacío: 400.
        MvcResult vacio = enviar(put(URL + "/" + uno.getId()), jsonNombre(""));
        assertThat(vacio.getResponse().getStatus()).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(vacio));

        // Id inexistente: 404.
        MvcResult inexistente = enviar(put(URL + "/" + UUID.randomUUID()), jsonNombre(nombre("Nuevo")));
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(inexistente));
    }

    // ---------------------------------------------------------------- IT-05

    @Test
    @DisplayName("IT-05 Inactivar: pasa a INACTIVO; es idempotente; con asignatura ACTIVA da 409 y no cambia; id inexistente da 404")
    void it05Inactivar() throws Exception {
        ProgramaAcademico sinAsignaturas = guardar(nombre("Sin Asignaturas"), "ITG-S01");

        MvcResult inactivado = enviar(patch(URL + "/" + sinAsignaturas.getId() + "/inactivar"), null);
        assertThat(inactivado.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(inactivado), "$.estado")).isEqualTo("INACTIVO");
        assertThat(programaRepository.findById(sinAsignaturas.getId()).orElseThrow().getEstado().name())
                .isEqualTo("INACTIVO");

        // Inactivar uno ya inactivo: idempotente, sigue en 200.
        MvcResult repetido = enviar(patch(URL + "/" + sinAsignaturas.getId() + "/inactivar"), null);
        assertThat(repetido.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(repetido), "$.estado")).isEqualTo("INACTIVO");

        // Con una asignatura ACTIVA vinculada: 409 y el programa sigue ACTIVO.
        ProgramaAcademico conAsignaturaActiva = guardar(nombre("Con Asignatura"), "ITG-S02");
        Asignatura asignatura = new Asignatura("ITG-ASIG1", "Asignatura de prueba", conAsignaturaActiva);
        asignatura.activar(5);
        asignaturaRepository.save(asignatura);

        MvcResult conflicto = enviar(patch(URL + "/" + conAsignaturaActiva.getId() + "/inactivar"), null);
        assertThat(conflicto.getResponse().getStatus()).isEqualTo(409);
        assertSinDetallesTecnicos(cuerpo(conflicto));
        assertThat(programaRepository.findById(conAsignaturaActiva.getId()).orElseThrow().getEstado().name())
                .isEqualTo("ACTIVO");

        // Id inexistente: 404.
        MvcResult inexistente = enviar(patch(URL + "/" + UUID.randomUUID() + "/inactivar"), null);
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(inexistente));
    }

    // ---------------------------------------------------------------- utilidades

    private ProgramaAcademico guardar(String nombre, String codigo) {
        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setNombre(nombre);
        programa.setCodigo(codigo);
        return programaRepository.save(programa);
    }

    private ProgramaAcademico guardarInactivo(String nombre, String codigo) {
        ProgramaAcademico programa = guardar(nombre, codigo);
        programa.setEstado(EstadoRegistro.INACTIVO);
        return programaRepository.save(programa);
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

    private long contarReservados() {
        return programaRepository.findAll().stream()
                .filter(p -> p.getCodigo().startsWith(PREFIJO_CODIGO))
                .count();
    }

    private static String nombre(String sufijo) {
        return PREFIJO_NOMBRE + sufijo;
    }

    private static String json(String nombre, String codigo) {
        return "{\"nombre\":" + texto(nombre) + ",\"codigo\":" + texto(codigo) + "}";
    }

    private static String jsonNombre(String nombre) {
        return "{\"nombre\":" + texto(nombre) + "}";
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor.replace("\"", "\\\"") + "\"";
    }
}
