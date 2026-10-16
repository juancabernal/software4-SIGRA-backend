package co.edu.uco.sigra.asignaturas.integracion;

import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.profesores.repository.ProfesorRepository;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// INTEGRATION: contexto completo + PostgreSQL real (SIGRA/.env). Ejecutar con: .\gradlew.bat integrationTest
//
// Sin @Transactional: el servicio confirma de verdad y cada caso verifica la respuesta HTTP y la fila real.
//
// Aislamiento de datos: todos los datos propios usan el prefijo reservado "ITG-" (profesor, programa,
// asignatura y el nombre del tipo de documento creado para la prueba). @BeforeEach y @AfterEach limpian
// en cascada: primero las asignaciones, luego las asignaturas, luego los programas, luego los profesores
// y por último el tipo de documento propio.
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMINISTRADOR")
class AsignacionDocenteIntegracionIT {

    private static final String URL = "/api/v1/asignaciones-docentes";
    private static final String PREFIJO_CORREO = "itg-asigdoc-";
    private static final String DOMINIO_CORREO = "@uco.net.co";
    private static final String PREFIJO_PROGRAMA = "ITG Programa AsigDoc ";
    private static final String PREFIJO_ASIGNATURA = "ITG-ASD-";
    private static final String TIPO_DOCUMENTO = "ITG Tipo AsigDoc";

    @Autowired private MockMvc mockMvc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private AsignacionDocenteRepository asignacionRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private ProgramaAcademicoRepository programaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepository;

    private TransactionTemplate tx;
    private TipoDocumento tipoDocumento;
    private ProgramaAcademico programa;
    private int contador;

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
    }

    @AfterEach
    void limpiar() {
        if (tx == null) {
            tx = new TransactionTemplate(transactionManager);
        }
        tx.executeWithoutResult(status -> {
            List<AsignacionDocente> asignacionesPropias = asignacionRepository.findAll().stream()
                    .filter(a -> esProfesorPropio(a.getProfesor()) || esAsignaturaPropia(a.getAsignatura()))
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

            List<Usuario> usuariosPropios = usuarioRepository.findAll().stream()
                    .filter(this::esProfesorPropio)
                    .toList();
            usuarioRepository.deleteAll(usuariosPropios);
            usuarioRepository.flush();

            tipoDocumentoRepository.deleteAll(tipoDocumentoRepository.findAll().stream()
                    .filter(t -> TIPO_DOCUMENTO.equals(t.getNombre()))
                    .toList());
            tipoDocumentoRepository.flush();
        });
    }

    private boolean esProfesorPropio(Usuario u) {
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
                    .filter(a -> esProfesorPropio(a.getProfesor()))
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

    // ---------------------------------------------------------------- utilidades

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

    private static String jsonAsignacion(List<UUID> profesorIds, List<UUID> asignaturaIds) {
        return "{\"profesorIds\":" + uuids(profesorIds) + ",\"asignaturaIds\":" + uuids(asignaturaIds) + "}";
    }

    private static String uuids(List<UUID> ids) {
        return "[" + ids.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",")) + "]";
    }
}
