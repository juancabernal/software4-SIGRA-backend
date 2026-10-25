package co.edu.uco.sigra.programas.integracion;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.auth.entity.Administrador;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.repository.EstudianteRepository;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.profesores.repository.ProfesorRepository;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
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
// Seguridad de punta a punta, igual que ProfesorIntegracionIT y AsignacionDocenteIntegracionIT: la
// cadena de filtros real corre (sin addFilters = false ni @WithMockUser) y cada petición lleva un JWT
// emitido por JwtService para un usuario real de la base. IT-01 a IT-05 usan el token de un
// ADMINISTRADOR; IT-06 prueba el 401 sin token o con token alterado y el 403 con tokens reales de
// PROFESOR y ESTUDIANTE.
//
// Aislamiento de datos: todos los programas de prueba usan el prefijo reservado "ITG-" en el código
// y "ITG Programa " en el nombre (el mismo que usa AsignaturaIntegracionIT para sus programas de
// apoyo); las asignaturas propias usan "ITG-PRG-" y el tipo de documento propio es "ITG Tipo Programa".
// @BeforeEach y @AfterEach limpian en cascada: primero las asignaturas, luego los programas, luego los
// usuarios propios (profesor, administrador y estudiante, cada uno por su subtipo concreto) y por
// último el tipo de documento propio.
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class ProgramaIntegracionIT {

    private static final String URL = "/api/v1/programas";
    private static final String PREFIJO_CODIGO = "ITG-";
    private static final String PREFIJO_NOMBRE = "ITG Programa ";
    private static final String PREFIJO_ASIGNATURA = "ITG-PRG-";
    private static final String PREFIJO_CORREO = "itg-prog-";
    private static final String DOMINIO_CORREO = "@uco.net.co";
    private static final String TIPO_DOCUMENTO = "ITG Tipo Programa";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ProgramaAcademicoRepository programaRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private EstudianteRepository estudianteRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private TransactionTemplate tx;
    private TipoDocumento tipoDocumento;

    /** JWT real de un ADMINISTRADOR creado en cada caso; es el que usan IT-01 a IT-05. */
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(transactionManager);

        limpiar();

        tx.executeWithoutResult(status -> {
            TipoDocumento tipo = new TipoDocumento();
            tipo.setNombre(TIPO_DOCUMENTO);
            tipoDocumento = tipoDocumentoRepository.save(tipo);
        });

        tokenAdmin = token(crearUsuario(new Administrador(), "admin"));
    }

    @AfterEach
    void limpiar() {
        if (tx == null) {
            tx = new TransactionTemplate(transactionManager);
        }

        tx.executeWithoutResult(status -> {
            List<Asignatura> asignaturasPropias = asignaturaRepository.findAll().stream()
                    .filter(a -> a.getCodigo() != null && a.getCodigo().startsWith(PREFIJO_ASIGNATURA))
                    .toList();
            asignaturaRepository.deleteAll(asignaturasPropias);
            asignaturaRepository.flush();

            List<ProgramaAcademico> reservados = programaRepository.findAll().stream()
                    .filter(p -> p.getCodigo().startsWith(PREFIJO_CODIGO))
                    .toList();
            programaRepository.deleteAll(reservados);
            programaRepository.flush();

            // Cada subtipo concreto se consulta por su propio repositorio (y Administrador por JPQL,
            // que no tiene uno): Usuario es abstracta (herencia JOINED) y una fila de usuario sin fila
            // en profesor/administrador/estudiante impediría cargarla con usuarioRepository.findAll().
            profesorRepository.deleteAll(profesorRepository.findAll().stream()
                    .filter(this::esUsuarioPropio)
                    .toList());
            profesorRepository.flush();

            estudianteRepository.deleteAll(estudianteRepository.findAll().stream()
                    .filter(this::esUsuarioPropio)
                    .toList());
            estudianteRepository.flush();

            entityManager.createQuery(
                            "select a from Administrador a where a.correoInstitucional like :prefijo "
                                    + "or a.tipoDocumento.nombre = :tipo",
                            Administrador.class)
                    .setParameter("prefijo", PREFIJO_CORREO + "%")
                    .setParameter("tipo", TIPO_DOCUMENTO)
                    .getResultList()
                    .forEach(entityManager::remove);
            entityManager.flush();

            tipoDocumentoRepository.deleteAll(tipoDocumentoRepository.findAll().stream()
                    .filter(t -> TIPO_DOCUMENTO.equals(t.getNombre()))
                    .toList());
            tipoDocumentoRepository.flush();
        });
    }

    private boolean esUsuarioPropio(Usuario u) {
        return (u.getCorreoInstitucional() != null && u.getCorreoInstitucional().startsWith(PREFIJO_CORREO))
                || (u.getTipoDocumento() != null && TIPO_DOCUMENTO.equals(u.getTipoDocumento().getNombre()));
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
        Asignatura asignatura = new Asignatura(PREFIJO_ASIGNATURA + "1", "Asignatura de prueba", conAsignaturaActiva);
        asignatura.activar(Asignatura.MIN_RA_ACTIVOS);
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

    // ---------------------------------------------------------------- IT-06

    /** Una llamada a uno de los cinco endpoints; el builder se crea de nuevo en cada envío. */
    private record Llamada(String nombre, Supplier<MockHttpServletRequestBuilder> peticion, String cuerpo) {
    }

    /** Los cinco endpoints del módulo, apuntando al programa de apoyo {@code objetivo}. */
    private List<Llamada> cincoEndpoints(ProgramaAcademico objetivo) {
        UUID id = objetivo.getId();
        return List.of(
                new Llamada("POST", () -> post(URL), json(nombre("Seguridad"), "ITG-SEC")),
                new Llamada("GET lista", () -> get(URL), null),
                new Llamada("GET por id", () -> get(URL + "/" + id), null),
                new Llamada("PUT", () -> put(URL + "/" + id), jsonNombre(nombre("Cambiado"))),
                new Llamada("PATCH inactivar", () -> patch(URL + "/" + id + "/inactivar"), null)
        );
    }

    @Test
    @DisplayName("IT-06a Sin token o con token alterado: 401 en los cinco endpoints y la base no cambia")
    void it06aSinTokenOTokenInvalido() throws Exception {
        ProgramaAcademico objetivo = guardar(nombre("Apoyo"), "ITG-060");
        long programasAntes = programaRepository.count();

        // Token del administrador con la firma alterada: el filtro debe rechazarlo aunque los
        // claims (id, correo, rol) sean los de un usuario real y activo.
        int inicioFirma = tokenAdmin.lastIndexOf('.') + 1;
        char original = tokenAdmin.charAt(inicioFirma);
        String tokenAlterado = tokenAdmin.substring(0, inicioFirma)
                + (original == 'A' ? 'B' : 'A')
                + tokenAdmin.substring(inicioFirma + 1);

        for (Llamada llamada : cincoEndpoints(objetivo)) {
            assertEstado(llamada.nombre() + " sin token", 401,
                    enviar(llamada.peticion().get(), null, llamada.cuerpo()));
            assertEstado(llamada.nombre() + " con token alterado", 401,
                    enviar(llamada.peticion().get(), tokenAlterado, llamada.cuerpo()));
        }

        assertSinCambios(objetivo, programasAntes);
    }

    @Test
    @DisplayName("IT-06b Un PROFESOR con token real no puede usar ningún endpoint de programas (403)")
    void it06bProfesorConTokenReal() throws Exception {
        verificarRolSinAcceso(token(crearProfesor()));
    }

    @Test
    @DisplayName("IT-06c Un ESTUDIANTE con token real no puede usar ningún endpoint de programas (403)")
    void it06cEstudianteConTokenReal() throws Exception {
        Estudiante estudiante = new Estudiante();
        estudiante.setNumeroDocumento("ES" + System.nanoTime() % 100000000L);
        verificarRolSinAcceso(token(crearUsuario(estudiante, "est")));
    }

    /**
     * Con el token recibido llama a los cinco endpoints: todos deben dar 403 sin detalles técnicos
     * y ninguno debe modificar la base. El programa de apoyo se crea por repositorio porque la API
     * le está vedada a estos roles.
     */
    private void verificarRolSinAcceso(String token) throws Exception {
        ProgramaAcademico objetivo = guardar(nombre("Sin Acceso"), "ITG-050");
        long programasAntes = programaRepository.count();

        for (Llamada llamada : cincoEndpoints(objetivo)) {
            assertEstado(llamada.nombre(), 403, enviar(llamada.peticion().get(), token, llamada.cuerpo()));
        }

        assertSinCambios(objetivo, programasAntes);
    }

    private void assertSinCambios(ProgramaAcademico objetivo, long programasAntes) {
        assertThat(programaRepository.count()).isEqualTo(programasAntes);

        ProgramaAcademico despues = programaRepository.findById(objetivo.getId()).orElseThrow();
        assertThat(despues.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(despues.getNombre()).isEqualTo(objetivo.getNombre());
    }

    private static void assertEstado(String caso, int esperado, MvcResult resultado) throws Exception {
        assertThat(resultado.getResponse().getStatus()).as(caso).isEqualTo(esperado);
        assertSinDetallesTecnicos(cuerpo(resultado));
    }

    // ---------------------------------------------------------------- utilidades

    private String token(Usuario usuario) {
        return jwtService.generarToken(usuario);
    }

    /** Guarda un usuario de la prueba (correo itg-prog-...) con el tipo de documento propio. */
    private <U extends Usuario> U crearUsuario(U usuario, String alias) {
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNombreCompleto("ITG Usuario " + alias);
        usuario.setCorreoInstitucional(PREFIJO_CORREO + alias + "-" + UUID.randomUUID() + DOMINIO_CORREO);
        usuario.setEstado(EstadoRegistro.ACTIVO);
        return usuarioRepository.save(usuario);
    }

    private Profesor crearProfesor() {
        Profesor p = new Profesor();
        p.setTipoDocumento(tipoDocumento);
        p.setNumeroDocumento("PR" + System.nanoTime() % 100000000L);
        p.setNombreCompleto("ITG Profesor Programa");
        p.setCorreoInstitucional(PREFIJO_CORREO + UUID.randomUUID() + DOMINIO_CORREO);
        p.setEstado(EstadoRegistro.ACTIVO);
        return profesorRepository.save(p);
    }

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

    /** Envía como ADMINISTRADOR (con su JWT real): es el caso de IT-01 a IT-05. */
    private MvcResult enviar(MockHttpServletRequestBuilder peticion, String json) throws Exception {
        return enviar(peticion, tokenAdmin, json);
    }

    /** Envía con el token indicado; con {@code token} nulo la petición va sin cabecera Authorization. */
    private MvcResult enviar(MockHttpServletRequestBuilder peticion, String token, String json) throws Exception {
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
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
