package co.edu.uco.sigra.asignaturas.integracion;

import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.auth.entity.Administrador;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// INTEGRATION: contexto completo + PostgreSQL real (SIGRA/.env). Ejecutar con: .\gradlew.bat integrationTest
// Sin @Transactional: el servicio confirma de verdad. Todos los datos propios llevan el prefijo ITG / it-
// y se borran en @AfterEach (y en @BeforeEach por si una corrida anterior se interrumpió).
@Tag("integration")
@SpringBootTest(properties = "sigra.seguridad.roles.habilitado=true")
@AutoConfigureMockMvc
class AsignaturaIntegracionIT {

    private static final String URL = "/api/v1/asignaturas";
    private static final String PREFIJO_CODIGO = "ITG-";
    private static final String PREFIJO_PROGRAMA = "ITG Programa ";
    private static final String PREFIJO_CORREO = "it-";
    private static final String DOMINIO_CORREO = "@uco.net.co";
    private static final String PREFIJO_SEMESTRE = "ITG-S";
    private static final String TIPO_DOCUMENTO = "ITG Tipo";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private ResultadoAprendizajeRepository raRepository;
    @Autowired private ProgramaAcademicoRepository programaRepository;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private SemestreRepository semestreRepository;
    @Autowired private AsignacionDocenteRepository asignacionRepository;
    @Autowired private MatriculaRepository matriculaRepository;

    private TransactionTemplate tx;
    private TipoDocumento tipoDocumento;
    private ProgramaAcademico programa;
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(transactionManager);
        limpiar();
        TipoDocumento tipo = new TipoDocumento();
        tipo.setNombre(TIPO_DOCUMENTO);
        tipoDocumento = tipoDocumentoRepository.save(tipo);
        programa = crearPrograma("Principal");
        tokenAdmin = token(crearUsuario(new Administrador(), "admin"));
    }

    @AfterEach
    void limpiar() {
        tx.executeWithoutResult(s -> {
            Predicate<Asignatura> deItg = a -> a.getCodigo().startsWith(PREFIJO_CODIGO);
            Predicate<Usuario> usuarioItg = u -> u.getCorreoInstitucional().startsWith(PREFIJO_CORREO)
                    && u.getCorreoInstitucional().endsWith(DOMINIO_CORREO);

            matriculaRepository.deleteAll(matriculaRepository.findAll().stream()
                    .filter(m -> deItg.test(m.getAsignatura()) || usuarioItg.test(m.getEstudiante())).toList());
            matriculaRepository.flush();
            asignacionRepository.deleteAll(asignacionRepository.findAll().stream()
                    .filter(a -> deItg.test(a.getAsignatura()) || usuarioItg.test(a.getProfesor())).toList());
            asignacionRepository.flush();
            raRepository.deleteAll(raRepository.findAll().stream()
                    .filter(r -> deItg.test(r.getAsignatura())).toList());
            raRepository.flush();
            asignaturaRepository.deleteAll(asignaturaRepository.findAll().stream().filter(deItg).toList());
            asignaturaRepository.flush();
            programaRepository.deleteAll(programaRepository.findAll().stream()
                    .filter(p -> p.getNombre().startsWith(PREFIJO_PROGRAMA)).toList());
            programaRepository.flush();
            usuarioRepository.deleteAll(usuarioRepository.findAll().stream().filter(usuarioItg).toList());
            usuarioRepository.flush();
            semestreRepository.deleteAll(semestreRepository.findAll().stream()
                    .filter(se -> se.getCodigo().startsWith(PREFIJO_SEMESTRE)).toList());
            semestreRepository.flush();
            tipoDocumentoRepository.deleteAll(tipoDocumentoRepository.findAll().stream()
                    .filter(t -> TIPO_DOCUMENTO.equals(t.getNombre())).toList());
        });
    }

    // ---------------------------------------------------------------- IT-01

    @Test
    @DisplayName("IT-01 Registrar con datos sucios: se normaliza, queda en BORRADOR y el duplicado da 409")
    void it01RegistrarConDatosSucios() throws Exception {
        MvcResult creado = enviar(post(URL), tokenAdmin, json(" itg-101 ", "  Cálculo   Integral  ", programa.getId()));
        String cuerpo = cuerpo(creado);
        assertThat(creado.getResponse().getStatus()).isEqualTo(201);
        assertThat((String) JsonPath.read(cuerpo, "$.codigo")).isEqualTo("ITG-101");
        assertThat((String) JsonPath.read(cuerpo, "$.nombre")).isEqualTo("Cálculo Integral");
        assertThat((String) JsonPath.read(cuerpo, "$.estado")).isEqualTo("BORRADOR");
        assertThat(((Number) JsonPath.read(cuerpo, "$.cantidadRa")).longValue()).isZero();
        assertThat((String) JsonPath.read(cuerpo, "$.programaId")).isEqualTo(programa.getId().toString());

        Asignatura enBase = asignaturaRepository.findById(UUID.fromString(JsonPath.read(cuerpo, "$.id"))).orElseThrow();
        assertThat(enBase.getCodigo()).isEqualTo("ITG-101");
        assertThat(enBase.getNombre()).isEqualTo("Cálculo Integral");
        assertThat(enBase.getEstado()).isEqualTo(EstadoAsignatura.BORRADOR);
        assertThat(programaIdDe(enBase.getId())).isEqualTo(programa.getId());
        assertThat(raDe(enBase.getId())).isEmpty();

        MvcResult duplicado = enviar(post(URL), tokenAdmin, json("Itg-101", "Otro Nombre", programa.getId()));
        assertThat(duplicado.getResponse().getStatus()).isEqualTo(409);
        assertSinDetallesTecnicos(cuerpo(duplicado));
        assertThat(asignaturaRepository.findAll().stream()
                .filter(a -> a.getCodigo().equalsIgnoreCase("ITG-101")).count()).isEqualTo(1);
    }

    // ---------------------------------------------------------------- IT-02

    static Stream<Arguments> entradasInvalidas() {
        String nombreOk = "ITG Nombre Valido";
        String codigoOk = "ITG-201";
        return Stream.of(
                Arguments.of("código demasiado corto", "AB", nombreOk, true, "AB"),
                Arguments.of("código de 21 caracteres", "A".repeat(21), nombreOk, true, "A".repeat(21)),
                Arguments.of("código con espacio interno", "AB C", nombreOk, true, "AB C"),
                Arguments.of("código que inicia con guion", "-AB", nombreOk, true, "-AB"),
                Arguments.of("código con guiones dobles", "A--B", nombreOk, true, "A--B"),
                Arguments.of("nombre demasiado corto", codigoOk, "AB", true, "AB"),
                Arguments.of("nombre de 101 caracteres", codigoOk, "A".repeat(101), true, "A".repeat(101)),
                Arguments.of("nombre con etiqueta HTML", codigoOk, "<script>x</script>", true, "<script>"),
                Arguments.of("nombre con carácter de control", codigoOk, "ITG Nombre\u0007Malo", true, "Nombre\u0007Malo"),
                Arguments.of("programaId nulo", codigoOk, nombreOk, false, null));
    }

    @ParameterizedTest(name = "IT-02 {0} -> 400 sin guardar nada")
    @MethodSource("entradasInvalidas")
    @DisplayName("IT-02 Entradas inválidas: 400, no se guarda nada y el mensaje no repite el valor enviado")
    void it02EntradasInvalidas(String caso, String codigo, String nombre, boolean conPrograma, String valorEnviado)
            throws Exception {
        long antes = asignaturaRepository.count();

        MvcResult r = enviar(post(URL), tokenAdmin, json(codigo, nombre, conPrograma ? programa.getId() : null));
        String cuerpo = cuerpo(r);

        assertThat(r.getResponse().getStatus()).as(caso).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo);
        if (valorEnviado != null) {
            assertThat(cuerpo).as(caso).doesNotContain(valorEnviado);
        }
        assertThat(asignaturaRepository.count()).as(caso).isEqualTo(antes);
    }

    // ---------------------------------------------------------------- IT-03

    @Test
    @DisplayName("IT-03 Activar según RA activos: 4 -> 400, 5 activos (+2 inactivos) -> 200, 8 -> 400")
    void it03ActivarSegunRaActivos() throws Exception {
        Asignatura asignatura = guardarAsignatura("ITG-301", EstadoAsignatura.BORRADOR);
        crearRas(asignatura, "RA", 4, EstadoRegistro.ACTIVO, false);

        MvcResult con4 = enviar(patch(URL + "/" + asignatura.getId() + "/activar"), tokenAdmin, null);
        assertThat(con4.getResponse().getStatus()).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(con4));
        assertThat(estadoEnBase(asignatura.getId())).isEqualTo(EstadoAsignatura.BORRADOR);

        crearRas(asignatura, "RB", 1, EstadoRegistro.ACTIVO, false);
        crearRas(asignatura, "RX", 2, EstadoRegistro.INACTIVO, false);
        MvcResult con5 = enviar(patch(URL + "/" + asignatura.getId() + "/activar"), tokenAdmin, null);
        assertThat(con5.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(con5), "$.estado")).isEqualTo("ACTIVA");
        assertThat(((Number) JsonPath.read(cuerpo(con5), "$.cantidadRa")).longValue()).isEqualTo(5);
        assertThat(estadoEnBase(asignatura.getId())).isEqualTo(EstadoAsignatura.ACTIVA);

        Asignatura conOcho = guardarAsignatura("ITG-302", EstadoAsignatura.BORRADOR);
        crearRas(conOcho, "RA", 8, EstadoRegistro.ACTIVO, false);
        MvcResult con8 = enviar(patch(URL + "/" + conOcho.getId() + "/activar"), tokenAdmin, null);
        assertThat(con8.getResponse().getStatus()).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(con8));
        assertThat(estadoEnBase(conOcho.getId())).isEqualTo(EstadoAsignatura.BORRADOR);
    }

    // ---------------------------------------------------------------- IT-04

    @Test
    @DisplayName("IT-04a Inactivar arrastra solo sus RA y reactivar restaura solo esos, sin tocar matrícula ni asignación")
    void it04aInactivarYReactivarEnCascada() throws Exception {
        Asignatura asignatura = guardarAsignatura("ITG-401", EstadoAsignatura.ACTIVA);
        List<UUID> activos = crearRas(asignatura, "RA", 5, EstadoRegistro.ACTIVO, false);
        UUID manual = crearRas(asignatura, "RM", 1, EstadoRegistro.INACTIVO, false).get(0);
        Profesor profesor = crearUsuario(profesorNuevo(), "prof4");
        Estudiante estudiante = crearUsuario(estudianteNuevo(), "est4");
        UUID asignacion = asignar(profesor, asignatura).getId();
        UUID matricula = matricular(estudiante, asignatura, crearSemestre()).getId();

        MvcResult inactivar = enviar(patch(URL + "/" + asignatura.getId() + "/inactivar"), tokenAdmin, null);
        assertThat(inactivar.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(inactivar), "$.estado")).isEqualTo("INACTIVA");
        assertThat(estadoEnBase(asignatura.getId())).isEqualTo(EstadoAsignatura.INACTIVA);
        for (UUID id : activos) {
            ResultadoAprendizaje ra = raRepository.findById(id).orElseThrow();
            assertThat(ra.getEstado()).isEqualTo(EstadoRegistro.INACTIVO);
            assertThat(ra.isInactivadoConAsignatura()).isTrue();
        }
        ResultadoAprendizaje raManual = raRepository.findById(manual).orElseThrow();
        assertThat(raManual.getEstado()).isEqualTo(EstadoRegistro.INACTIVO);
        assertThat(raManual.isInactivadoConAsignatura()).isFalse();
        assertThat(matriculaRepository.findById(matricula).orElseThrow().getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(asignacionRepository.findById(asignacion).orElseThrow().getEstado()).isEqualTo(EstadoRegistro.ACTIVO);

        MvcResult reactivar = enviar(patch(URL + "/" + asignatura.getId() + "/activar"), tokenAdmin, null);
        assertThat(reactivar.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(reactivar), "$.estado")).isEqualTo("ACTIVA");
        assertThat(((Number) JsonPath.read(cuerpo(reactivar), "$.cantidadRa")).longValue()).isEqualTo(5);
        assertThat(estadoEnBase(asignatura.getId())).isEqualTo(EstadoAsignatura.ACTIVA);
        for (UUID id : activos) {
            assertThat(raRepository.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
        }
        assertThat(raRepository.findById(manual).orElseThrow().getEstado()).isEqualTo(EstadoRegistro.INACTIVO);
        assertThat(matriculaRepository.findById(matricula).orElseThrow().getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(asignacionRepository.findById(asignacion).orElseThrow().getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
    }

    @Test
    @DisplayName("IT-04b Reactivar con solo 4 RA marcados da 400 y la base queda intacta (rollback real)")
    void it04bReactivarConPocosRaHaceRollback() throws Exception {
        Asignatura asignatura = guardarAsignatura("ITG-402", EstadoAsignatura.INACTIVA);
        List<UUID> marcados = crearRas(asignatura, "RA", 4, EstadoRegistro.INACTIVO, true);

        MvcResult r = enviar(patch(URL + "/" + asignatura.getId() + "/activar"), tokenAdmin, null);
        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(r));

        assertThat(estadoEnBase(asignatura.getId())).isEqualTo(EstadoAsignatura.INACTIVA);
        for (UUID id : marcados) {
            ResultadoAprendizaje ra = raRepository.findById(id).orElseThrow();
            assertThat(ra.getEstado()).isEqualTo(EstadoRegistro.INACTIVO);
            assertThat(ra.isInactivadoConAsignatura()).isTrue();
        }
    }

    // ---------------------------------------------------------------- IT-05

    @Test
    @DisplayName("IT-05 Roles y alcance con tokens reales: 401, 403 y cada usuario ve solo lo suyo")
    void it05RolesYAlcance() throws Exception {
        Asignatura a = guardarAsignatura("ITG-501", EstadoAsignatura.BORRADOR);
        Asignatura b = guardarAsignatura("ITG-502", EstadoAsignatura.BORRADOR);
        Asignatura c = guardarAsignatura("ITG-503", EstadoAsignatura.BORRADOR);
        Profesor profesor = crearUsuario(profesorNuevo(), "prof5");
        Estudiante estudiante = crearUsuario(estudianteNuevo(), "est5");
        asignar(profesor, a);
        asignar(profesor, b);
        matricular(estudiante, a, crearSemestre());
        String tokenProfesor = token(profesor);
        String tokenEstudiante = token(estudiante);
        String nueva = json("ITG-599", "ITG No Debe Existir", programa.getId());
        long antes = asignaturaRepository.count();

        assertThat(enviar(get(URL), null, null).getResponse().getStatus()).isEqualTo(401);
        MvcResult estPost = enviar(post(URL), tokenEstudiante, nueva);
        assertThat(estPost.getResponse().getStatus()).isEqualTo(403);
        assertSinDetallesTecnicos(cuerpo(estPost));
        assertThat(enviar(post(URL), tokenProfesor, nueva).getResponse().getStatus()).isEqualTo(403);
        assertThat(asignaturaRepository.count()).isEqualTo(antes);

        MvcResult lista = enviar(get(URL), tokenProfesor, null);
        assertThat(lista.getResponse().getStatus()).isEqualTo(200);
        assertThat(codigos(lista)).containsExactlyInAnyOrder("ITG-501", "ITG-502");

        assertThat(enviar(get(URL + "/" + a.getId()), tokenProfesor, null).getResponse().getStatus()).isEqualTo(200);
        MvcResult ajena = enviar(get(URL + "/" + c.getId()), tokenProfesor, null);
        assertThat(ajena.getResponse().getStatus()).isEqualTo(403);
        assertSinDetallesTecnicos(cuerpo(ajena));

        MvcResult miasProfesor = enviar(get(URL + "/mias"), tokenProfesor, null);
        assertThat(miasProfesor.getResponse().getStatus()).isEqualTo(200);
        assertThat(codigos(miasProfesor)).containsExactlyInAnyOrder("ITG-501", "ITG-502");

        MvcResult miasEstudiante = enviar(get(URL + "/mias"), tokenEstudiante, null);
        assertThat(miasEstudiante.getResponse().getStatus()).isEqualTo(200);
        assertThat(codigos(miasEstudiante)).containsExactly("ITG-501");

        assertThat(enviar(get(URL + "/mias"), tokenAdmin, null).getResponse().getStatus()).isEqualTo(403);
    }

    // ---------------------------------------------------------------- utilidades

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

    private static List<String> codigos(MvcResult r) {
        return JsonPath.read(cuerpo(r), "$[*].codigo");
    }

    private static void assertSinDetallesTecnicos(String cuerpo) {
        assertThat(cuerpo).doesNotContain("Exception", "org.", "java.", "SQL", "trace", "constraint");
    }

    private static String json(String codigo, String nombre, UUID programaId) {
        return "{\"codigo\":" + texto(codigo) + ",\"nombre\":" + texto(nombre)
                + ",\"programaId\":" + (programaId == null ? "null" : "\"" + programaId + "\"") + "}";
    }

    private static String texto(String valor) {
        StringBuilder sb = new StringBuilder("\"");
        for (char ch : valor.toCharArray()) {
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private String token(Usuario usuario) {
        return jwtService.generarToken(usuario);
    }

    private ProgramaAcademico crearPrograma(String sufijo) {
        ProgramaAcademico p = new ProgramaAcademico();
        p.setNombre(PREFIJO_PROGRAMA + sufijo + " " + UUID.randomUUID().toString().substring(0, 8));
        p.setCodigo("ITG" + ThreadLocalRandom.current().nextInt(1_000_000, 10_000_000));
        return programaRepository.save(p);
    }

    private <U extends Usuario> U crearUsuario(U usuario, String alias) {
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNombreCompleto("ITG Usuario " + alias);
        usuario.setCorreoInstitucional(PREFIJO_CORREO + alias + "-" + UUID.randomUUID().toString().substring(0, 8)
                + DOMINIO_CORREO);
        return usuarioRepository.save(usuario);
    }

    private static String documento() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    }

    private static Profesor profesorNuevo() {
        Profesor p = new Profesor();
        p.setNumeroDocumento(documento());
        return p;
    }

    private static Estudiante estudianteNuevo() {
        Estudiante e = new Estudiante();
        e.setNumeroDocumento(documento());
        return e;
    }

    private Semestre crearSemestre() {
        Semestre s = new Semestre();
        s.setCodigo(PREFIJO_SEMESTRE + ThreadLocalRandom.current().nextInt(1000, 10000));
        s.setFechaInicio(LocalDate.now().minusDays(10));
        s.setFechaFin(LocalDate.now().plusDays(90));
        s.setEstado(EstadoSemestre.ACTIVO);
        return semestreRepository.save(s);
    }

    private Asignatura guardarAsignatura(String codigo, EstadoAsignatura estado) {
        Asignatura a = new Asignatura(codigo, "ITG Asignatura " + codigo, programa);
        if (estado != EstadoAsignatura.BORRADOR) {
            a.activar(Asignatura.MIN_RA_ACTIVOS);
        }
        if (estado == EstadoAsignatura.INACTIVA) {
            a.inactivar();
        }
        return asignaturaRepository.save(a);
    }

    private List<UUID> crearRas(Asignatura asignatura, String prefijo, int cantidad, EstadoRegistro estado,
                                boolean marcado) {
        return Stream.iterate(1, i -> i + 1).limit(cantidad).map(i -> {
            ResultadoAprendizaje ra = new ResultadoAprendizaje();
            ra.setAsignatura(asignatura);
            ra.setCodigo(prefijo + i);
            ra.setDescripcion("ITG resultado de aprendizaje " + prefijo + i);
            ra.setEstado(estado);
            ra.setInactivadoConAsignatura(marcado);
            return raRepository.save(ra).getId();
        }).toList();
    }

    private AsignacionDocente asignar(Profesor profesor, Asignatura asignatura) {
        AsignacionDocente ad = new AsignacionDocente();
        ad.setProfesor(profesor);
        ad.setAsignatura(asignatura);
        ad.setEstado(EstadoRegistro.ACTIVO);
        return asignacionRepository.save(ad);
    }

    private Matricula matricular(Estudiante estudiante, Asignatura asignatura, Semestre semestre) {
        Matricula m = new Matricula();
        m.setEstudiante(estudiante);
        m.setAsignatura(asignatura);
        m.setSemestre(semestre);
        m.setEstado(EstadoRegistro.ACTIVO);
        return matriculaRepository.save(m);
    }

    private EstadoAsignatura estadoEnBase(UUID id) {
        return asignaturaRepository.findById(id).orElseThrow().getEstado();
    }

    private UUID programaIdDe(UUID asignaturaId) {
        return tx.execute(s -> asignaturaRepository.findById(asignaturaId).orElseThrow().getPrograma().getId());
    }

    private List<ResultadoAprendizaje> raDe(UUID asignaturaId) {
        return tx.execute(s -> raRepository.findAll().stream()
                .filter(r -> r.getAsignatura().getId().equals(asignaturaId)).toList());
    }
}
