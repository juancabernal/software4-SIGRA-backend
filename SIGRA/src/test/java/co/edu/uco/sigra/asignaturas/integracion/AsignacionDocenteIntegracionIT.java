package co.edu.uco.sigra.asignaturas.integracion;

import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// INTEGRATION: contexto completo + PostgreSQL real (SIGRA/.env). Ejecutar con: .\gradlew.bat integrationTest
//
// Sin @Transactional: el servicio confirma de verdad y cada caso verifica la respuesta HTTP y la fila real.
//
// Seguridad de punta a punta, igual que ProfesorIntegracionIT: la cadena de filtros real corre (sin
// addFilters = false ni @WithMockUser) y cada petición lleva un JWT emitido por JwtService para un
// usuario real de la base. IT-01 a IT-07 usan el token de un ADMINISTRADOR; IT-08 prueba el 401 sin
// token o con token alterado y el 403 con tokens reales de PROFESOR y ESTUDIANTE.
//
// Aislamiento de datos: todos los datos propios usan el prefijo reservado "ITG-" (profesor, programa,
// asignatura y el nombre del tipo de documento creado para la prueba). @BeforeEach y @AfterEach limpian
// en cascada: primero las asignaciones, luego las asignaturas, luego los programas, luego los usuarios
// propios (profesor, administrador y estudiante, cada uno por su subtipo concreto) y por último el tipo
// de documento propio.
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class AsignacionDocenteIntegracionIT {

    private static final String URL = "/api/v1/asignaciones-docentes";
    private static final String PREFIJO_CORREO = "itg-asigdoc-";
    private static final String DOMINIO_CORREO = "@uco.net.co";
    private static final String PREFIJO_PROGRAMA = "ITG Programa AsigDoc ";
    private static final String PREFIJO_ASIGNATURA = "ITG-ASD-";
    private static final String TIPO_DOCUMENTO = "ITG Tipo AsigDoc";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private AsignacionDocenteRepository asignacionRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private ProgramaAcademicoRepository programaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired private EstudianteRepository estudianteRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private TransactionTemplate tx;
    private TipoDocumento tipoDocumento;
    private ProgramaAcademico programa;
    private int contador;

    /** JWT real de un ADMINISTRADOR creado en cada caso; es el que usan IT-01 a IT-07. */
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(transactionManager);
        limpiar();

        tx.executeWithoutResult(status -> {
            TipoDocumento tipo = new TipoDocumento();
            tipo.setNombre(TIPO_DOCUMENTO);
            tipoDocumento = tipoDocumentoRepository.save(tipo);

            ProgramaAcademico p = new ProgramaAcademico();
            p.setNombre(PREFIJO_PROGRAMA + UUID.randomUUID());
            p.setCodigo("ASD" + Math.abs(UUID.randomUUID().hashCode() % 100000));
            p.setEstado(EstadoRegistro.ACTIVO);
            programa = programaRepository.save(p);
        });

        tokenAdmin = token(crearUsuario(new Administrador(), "admin"));
    }

    @AfterEach
    void limpiar() {
        if (tx == null) {
            tx = new TransactionTemplate(transactionManager);
        }
        tx.executeWithoutResult(status -> {
            List<AsignacionDocente> asignacionesPropias = asignacionRepository.findAll().stream()
                    .filter(a -> esUsuarioPropio(a.getProfesor()) || esAsignaturaPropia(a.getAsignatura()))
                    .toList();
            asignacionRepository.deleteAll(asignacionesPropias);
            asignacionRepository.flush();

            List<Asignatura> asignaturasPropias = asignaturaRepository.findAll().stream()
                    .filter(this::esAsignaturaPropia)
                    .toList();
            asignaturaRepository.deleteAll(asignaturasPropias);
            asignaturaRepository.flush();

            List<ProgramaAcademico> programasPropios = programaRepository.findAll().stream()
                    .filter(p -> p.getNombre() != null && p.getNombre().startsWith(PREFIJO_PROGRAMA))
                    .toList();
            programaRepository.deleteAll(programasPropios);
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

    private boolean esAsignaturaPropia(Asignatura a) {
        return a.getCodigo() != null && a.getCodigo().startsWith(PREFIJO_ASIGNATURA);
    }

    /**
     * Las asignaciones propias, con profesor y asignatura ya resueltos (son LAZY): fuera de esta
     * transacción, acceder a esos proxies sin inicializar lanzaría LazyInitializationException.
     */
    private List<AsignacionDocente> asignacionesPropias() {
        return tx.execute(status -> {
            List<AsignacionDocente> propias = asignacionRepository.findAll().stream()
                    .filter(a -> esUsuarioPropio(a.getProfesor()))
                    .toList();
            propias.forEach(a -> {
                a.getProfesor().getNombreCompleto();
                a.getAsignatura().getCodigo();
            });
            return propias;
        });
    }

    // ---------------------------------------------------------------- IT-01

    @Test
    @DisplayName("IT-01 Asignar masivo: cada profesor con cada asignatura (cartesiano), guardado real en BD")
    void it01AsignarMasivo() throws Exception {
        Profesor p1 = crearProfesor();
        Profesor p2 = crearProfesor();
        Asignatura a1 = crearAsignaturaActiva();
        Asignatura a2 = crearAsignaturaActiva();

        MvcResult r = enviar(post(URL), jsonAsignacion(List.of(p1.getId(), p2.getId()), List.of(a1.getId(), a2.getId())));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        String cuerpo = cuerpo(r);
        List<Object> creadas = JsonPath.read(cuerpo, "$.creadas");
        assertThat(creadas).hasSize(4);
        assertThat((List<?>) JsonPath.read(cuerpo, "$.omitidas")).isEmpty();
        assertThat((List<?>) JsonPath.read(cuerpo, "$.rechazadas")).isEmpty();

        List<AsignacionDocente> enBase = asignacionesPropias();
        assertThat(enBase).hasSize(4);
        assertThat(enBase).allMatch(a -> a.getEstado() == EstadoRegistro.ACTIVO);
        assertThat(enBase).allMatch(a -> a.getFechaAsignacion().equals(LocalDate.now()));
    }

    // ---------------------------------------------------------------- IT-02

    @Test
    @DisplayName("IT-02 Omitir duplicados: el segundo intento con el mismo par se omite y lo informa, sin crear otra fila")
    void it02OmitirDuplicados() throws Exception {
        Profesor profesor = crearProfesor();
        Asignatura asignatura = crearAsignaturaActiva();

        MvcResult primera = enviar(post(URL), jsonAsignacion(List.of(profesor.getId()), List.of(asignatura.getId())));
        assertThat(primera.getResponse().getStatus()).isEqualTo(201);
        assertThat((List<?>) JsonPath.read(cuerpo(primera), "$.creadas")).hasSize(1);

        MvcResult segunda = enviar(post(URL), jsonAsignacion(List.of(profesor.getId()), List.of(asignatura.getId())));
        assertThat(segunda.getResponse().getStatus()).isEqualTo(201);
        assertThat((List<?>) JsonPath.read(cuerpo(segunda), "$.creadas")).isEmpty();
        List<Object> omitidas = JsonPath.read(cuerpo(segunda), "$.omitidas");
        assertThat(omitidas).hasSize(1);

        assertThat(asignacionesPropias()).hasSize(1);
    }

    // ---------------------------------------------------------------- IT-03

    @Test
    @DisplayName("IT-03 Rechazar profesor o asignatura inactivos: se informan y no se crea fila para ellos")
    void it03RechazarInactivos() throws Exception {
        Profesor profesorActivo = crearProfesor();
        Profesor profesorInactivo = crearProfesor();
        tx.executeWithoutResult(status -> {
            Profesor p = profesorRepository.findById(profesorInactivo.getId()).orElseThrow();
            p.setEstado(EstadoRegistro.INACTIVO);
            profesorRepository.save(p);
        });

        Asignatura asignaturaActiva = crearAsignaturaActiva();
        Asignatura asignaturaBorrador = crearAsignaturaBorrador();

        MvcResult r = enviar(post(URL), jsonAsignacion(
                List.of(profesorActivo.getId(), profesorInactivo.getId()),
                List.of(asignaturaActiva.getId(), asignaturaBorrador.getId())));
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        String cuerpo = cuerpo(r);

        // Solo el par (profesorActivo, asignaturaActiva) debe quedar creado.
        List<Object> creadas = JsonPath.read(cuerpo, "$.creadas");
        assertThat(creadas).hasSize(1);
        List<Object> rechazadas = JsonPath.read(cuerpo, "$.rechazadas");
        assertThat(rechazadas).hasSize(3);

        List<AsignacionDocente> enBase = asignacionesPropias();
        assertThat(enBase).hasSize(1);
        assertThat(enBase.get(0).getProfesor().getId()).isEqualTo(profesorActivo.getId());
        assertThat(enBase.get(0).getAsignatura().getId()).isEqualTo(asignaturaActiva.getId());
    }

    // ---------------------------------------------------------------- IT-04

    @Test
    @DisplayName("IT-04 Profesor o asignatura inexistentes: 404 y no se crea nada")
    void it04IdsInexistentes() throws Exception {
        Profesor profesor = crearProfesor();
        Asignatura asignatura = crearAsignaturaActiva();
        UUID idInexistente = UUID.randomUUID();

        MvcResult profesorInexistente = enviar(post(URL),
                jsonAsignacion(List.of(idInexistente), List.of(asignatura.getId())));
        assertThat(profesorInexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(profesorInexistente));

        MvcResult asignaturaInexistente = enviar(post(URL),
                jsonAsignacion(List.of(profesor.getId()), List.of(idInexistente)));
        assertThat(asignaturaInexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(asignaturaInexistente));

        assertThat(asignacionesPropias()).isEmpty();
    }

    // ---------------------------------------------------------------- IT-05

    @Test
    @DisplayName("IT-05 Desasignar: pasa a INACTIVO, es idempotente, y un id inexistente da 404")
    void it05Desasignar() throws Exception {
        Profesor profesor = crearProfesor();
        Asignatura asignatura = crearAsignaturaActiva();
        enviar(post(URL), jsonAsignacion(List.of(profesor.getId()), List.of(asignatura.getId())));
        AsignacionDocente creada = asignacionRepository.findByProfesor_IdAndAsignatura_Id(
                profesor.getId(), asignatura.getId()).orElseThrow();

        MvcResult desasignada = enviar(patch(URL + "/" + creada.getId() + "/desasignar"), null);
        assertThat(desasignada.getResponse().getStatus()).isEqualTo(204);
        assertThat(asignacionRepository.findById(creada.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoRegistro.INACTIVO);

        MvcResult repetida = enviar(patch(URL + "/" + creada.getId() + "/desasignar"), null);
        assertThat(repetida.getResponse().getStatus()).isEqualTo(204);

        MvcResult inexistente = enviar(patch(URL + "/" + UUID.randomUUID() + "/desasignar"), null);
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(inexistente));
    }

    // ---------------------------------------------------------------- IT-06

    @Test
    @DisplayName("IT-06 Reasignar: una fila INACTIVA se reactiva sin crear un duplicado")
    void it06ReactivarAsignacionInactiva() throws Exception {
        Profesor profesor = crearProfesor();
        Asignatura asignatura = crearAsignaturaActiva();
        enviar(post(URL), jsonAsignacion(List.of(profesor.getId()), List.of(asignatura.getId())));
        AsignacionDocente original = asignacionRepository.findByProfesor_IdAndAsignatura_Id(
                profesor.getId(), asignatura.getId()).orElseThrow();

        enviar(patch(URL + "/" + original.getId() + "/desasignar"), null);
        MvcResult reasignada = enviar(post(URL), jsonAsignacion(List.of(profesor.getId()), List.of(asignatura.getId())));

        assertThat(reasignada.getResponse().getStatus()).isEqualTo(201);
        assertThat((List<?>) JsonPath.read(cuerpo(reasignada), "$.creadas")).hasSize(1);
        AsignacionDocente finalizada = asignacionRepository.findByProfesor_IdAndAsignatura_Id(
                profesor.getId(), asignatura.getId()).orElseThrow();
        assertThat(finalizada.getId()).isEqualTo(original.getId());
        assertThat(finalizada.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
        assertThat(asignacionesPropias()).hasSize(1);
    }

    // ---------------------------------------------------------------- IT-07

    static Stream<Arguments> entradasInvalidas() {
        UUID idValido = UUID.randomUUID();
        return Stream.of(
                Arguments.of("listas vacías",
                        "{\"profesorIds\":[],\"asignaturaIds\":[]}"),
                Arguments.of("profesorIds vacío",
                        "{\"profesorIds\":[],\"asignaturaIds\":[\"" + idValido + "\"]}"),
                Arguments.of("asignaturaIds vacío",
                        "{\"profesorIds\":[\"" + idValido + "\"],\"asignaturaIds\":[]}"),
                Arguments.of("elemento nulo en profesorIds",
                        "{\"profesorIds\":[null],\"asignaturaIds\":[\"" + idValido + "\"]}"),
                Arguments.of("elemento nulo en asignaturaIds",
                        "{\"profesorIds\":[\"" + idValido + "\"],\"asignaturaIds\":[null]}"),
                Arguments.of("profesorIds ausente",
                        "{\"asignaturaIds\":[\"" + idValido + "\"]}"),
                Arguments.of("asignaturaIds ausente",
                        "{\"profesorIds\":[\"" + idValido + "\"]}"),
                Arguments.of("UUID mal formado en profesorIds",
                        "{\"profesorIds\":[\"no-es-uuid\"],\"asignaturaIds\":[\"" + idValido + "\"]}"),
                Arguments.of("cuerpo vacío", "{}")
        );
    }

    @ParameterizedTest(name = "IT-07 {0} -> 400")
    @MethodSource("entradasInvalidas")
    @DisplayName("IT-07 Entradas inválidas: 400 y no se crea ninguna asignación")
    void it07EntradasInvalidas(String caso, String cuerpoPeticion) throws Exception {
        long asignacionesAntes = asignacionRepository.count();

        MvcResult resultado = enviar(post(URL), cuerpoPeticion);

        assertThat(resultado.getResponse().getStatus()).as(caso).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(resultado));
        assertThat(asignacionRepository.count()).as(caso).isEqualTo(asignacionesAntes);
    }

    // ---------------------------------------------------------------- IT-08

    /** Una llamada a uno de los dos endpoints; el builder se crea de nuevo en cada envío. */
    private record Llamada(String nombre, Supplier<MockHttpServletRequestBuilder> peticion, String cuerpo) {
    }

    /** Los dos endpoints del módulo, apuntando al par profesor/asignatura de apoyo. */
    private List<Llamada> dosEndpoints(Profesor profesor, Asignatura asignatura) {
        return List.of(
                new Llamada("POST asignar", () -> post(URL),
                        jsonAsignacion(List.of(profesor.getId()), List.of(asignatura.getId()))),
                new Llamada("PATCH desasignar", () -> patch(URL + "/" + UUID.randomUUID() + "/desasignar"), null)
        );
    }

    @Test
    @DisplayName("IT-08a Sin token o con token alterado: 401 en los dos endpoints y la base no cambia")
    void it08aSinTokenOTokenInvalido() throws Exception {
        Profesor profesor = crearProfesor();
        Asignatura asignatura = crearAsignaturaActiva();
        long asignacionesAntes = asignacionRepository.count();

        // Token del administrador con la firma alterada: el filtro debe rechazarlo aunque los
        // claims (id, correo, rol) sean los de un usuario real y activo.
        int inicioFirma = tokenAdmin.lastIndexOf('.') + 1;
        char original = tokenAdmin.charAt(inicioFirma);
        String tokenAlterado = tokenAdmin.substring(0, inicioFirma)
                + (original == 'A' ? 'B' : 'A')
                + tokenAdmin.substring(inicioFirma + 1);

        for (Llamada llamada : dosEndpoints(profesor, asignatura)) {
            assertEstado(llamada.nombre() + " sin token", 401,
                    enviar(llamada.peticion().get(), null, llamada.cuerpo()));
            assertEstado(llamada.nombre() + " con token alterado", 401,
                    enviar(llamada.peticion().get(), tokenAlterado, llamada.cuerpo()));
        }

        assertThat(asignacionRepository.count()).isEqualTo(asignacionesAntes);
    }

    @Test
    @DisplayName("IT-08b Un PROFESOR con token real no puede usar los endpoints de asignación docente (403)")
    void it08bProfesorConTokenReal() throws Exception {
        Profesor duenoDelToken = crearProfesor();
        verificarRolSinAcceso(token(duenoDelToken));
    }

    @Test
    @DisplayName("IT-08c Un ESTUDIANTE con token real no puede usar los endpoints de asignación docente (403)")
    void it08cEstudianteConTokenReal() throws Exception {
        Estudiante estudiante = new Estudiante();
        // "ITGES" (5) + hasta 5 dígitos = máximo 10: numero_documento es varchar(10) y un
        // nanoTime() % 1_000_000 ocasional de 6 dígitos lo desbordaba de forma intermitente.
        estudiante.setNumeroDocumento("ITGES" + System.nanoTime() % 100000L);
        verificarRolSinAcceso(token(crearUsuario(estudiante, "est")));
    }

    /**
     * Con el token recibido llama a los dos endpoints: ambos deben dar 403 sin detalles técnicos y
     * ninguno debe modificar la base.
     */
    private void verificarRolSinAcceso(String token) throws Exception {
        Profesor profesor = crearProfesor();
        Asignatura asignatura = crearAsignaturaActiva();
        long asignacionesAntes = asignacionRepository.count();

        for (Llamada llamada : dosEndpoints(profesor, asignatura)) {
            assertEstado(llamada.nombre(), 403, enviar(llamada.peticion().get(), token, llamada.cuerpo()));
        }

        assertThat(asignacionRepository.count()).isEqualTo(asignacionesAntes);
    }

    private static void assertEstado(String caso, int esperado, MvcResult resultado) {
        assertThat(resultado.getResponse().getStatus()).as(caso).isEqualTo(esperado);
        assertSinDetallesTecnicos(cuerpo(resultado));
    }

    // ---------------------------------------------------------------- utilidades

    private String token(Usuario usuario) {
        return jwtService.generarToken(usuario);
    }

    /** Guarda un usuario de la prueba (correo itg-asigdoc-...) con el tipo de documento propio. */
    private <U extends Usuario> U crearUsuario(U usuario, String alias) {
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNombreCompleto("ITG Usuario " + alias);
        usuario.setCorreoInstitucional(PREFIJO_CORREO + alias + "-" + UUID.randomUUID() + DOMINIO_CORREO);
        usuario.setEstado(EstadoRegistro.ACTIVO);
        return usuarioRepository.save(usuario);
    }

    private Profesor crearProfesor() {
        contador++;
        return tx.execute(status -> {
            Profesor p = new Profesor();
            p.setTipoDocumento(tipoDocumento);
            p.setNumeroDocumento("AD" + System.nanoTime() % 100000000L);
            p.setNombreCompleto("Profesor ITG " + contador);
            p.setCorreoInstitucional(PREFIJO_CORREO + UUID.randomUUID() + DOMINIO_CORREO);
            p.setEstado(EstadoRegistro.ACTIVO);
            return profesorRepository.save(p);
        });
    }

    private Asignatura crearAsignaturaActiva() {
        Asignatura a = crearAsignaturaBorrador();
        return tx.execute(status -> {
            Asignatura enBase = asignaturaRepository.findById(a.getId()).orElseThrow();
            enBase.activar(Asignatura.MIN_RA_ACTIVOS);
            return asignaturaRepository.save(enBase);
        });
    }

    private Asignatura crearAsignaturaBorrador() {
        contador++;
        return tx.execute(status -> {
            String codigo = PREFIJO_ASIGNATURA + (System.nanoTime() % 1000000L);
            Asignatura a = new Asignatura(codigo, "Asignatura ITG " + contador, programa);
            return asignaturaRepository.save(a);
        });
    }

    /** Envía como ADMINISTRADOR (con su JWT real): es el caso de IT-01 a IT-07. */
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

    private static String jsonAsignacion(List<UUID> profesorIds, List<UUID> asignaturaIds) {
        return "{\"profesorIds\":" + uuids(profesorIds) + ",\"asignaturaIds\":" + uuids(asignaturaIds) + "}";
    }

    private static String uuids(List<UUID> ids) {
        return "[" + ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",")) + "]";
    }
}
