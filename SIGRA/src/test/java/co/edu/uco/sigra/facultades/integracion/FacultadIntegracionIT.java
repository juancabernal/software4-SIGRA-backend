package co.edu.uco.sigra.facultades.integracion;

import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.auth.entity.Administrador;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.repository.EstudianteRepository;
import co.edu.uco.sigra.facultades.entity.Facultad;
import co.edu.uco.sigra.facultades.repository.FacultadRepository;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.profesores.repository.ProfesorRepository;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.testsupport.NombresPersonaTestUtil;
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
// Seguridad de punta a punta, igual que ProfesorIntegracionIT y ProgramaIntegracionIT: la cadena de
// filtros real corre y cada petición lleva un JWT emitido por JwtService para un usuario real de la
// base. IT-01 a IT-06 usan el token de un ADMINISTRADOR; IT-07 prueba el 401 sin token o con token
// alterado y el 403 con tokens reales de PROFESOR y ESTUDIANTE.
//
// Aislamiento de datos: todas las facultades de prueba usan el prefijo reservado "ITF Facultad " en el
// nombre; los programas de apoyo, "ITF-" en el código e "ITF Programa " en el nombre; los usuarios,
// el correo "itf-fac-..." y el tipo de documento propio "ITF Tipo Facultad". @BeforeEach y @AfterEach
// limpian en cascada: primero los programas (apuntan a la facultad), luego las facultades, luego los
// usuarios propios y por último el tipo de documento.
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class FacultadIntegracionIT {

    private static final String URL = "/api/v1/facultades";
    private static final String URL_PROGRAMAS = "/api/v1/programas";
    private static final String URL_ASIGNATURAS = "/api/v1/asignaturas";
    private static final String URL_REPORTE_R1 = "/api/v1/reportes/rendimiento-ra";
    private static final String URL_REPORTE_R6 = "/api/v1/reportes/riesgo-academico";
    private static final String PREFIJO_NOMBRE = "ITF Facultad ";
    private static final String PREFIJO_CODIGO_PROGRAMA = "ITF-";
    private static final String PREFIJO_NOMBRE_PROGRAMA = "ITF Programa ";
    private static final String PREFIJO_CORREO = "itf-fac-";
    private static final String DOMINIO_CORREO = "@uco.net.co";
    private static final String TIPO_DOCUMENTO = "ITF Tipo Facultad";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private FacultadRepository facultadRepository;
    @Autowired private ProgramaAcademicoRepository programaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AsignacionDocenteRepository asignacionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private TransactionTemplate tx;
    private TipoDocumento tipoDocumento;

    /** JWT real de un ADMINISTRADOR creado en cada caso; es el que usan IT-01 a IT-06. */
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
            // Primero lo que apunta a los programas de apoyo: asignaciones docentes y asignaturas ITF-.
            asignacionRepository.deleteAll(asignacionRepository.findAll().stream()
                    .filter(ad -> ad.getAsignatura().getCodigo().startsWith(PREFIJO_CODIGO_PROGRAMA))
                    .toList());
            asignacionRepository.flush();
            asignaturaRepository.deleteAll(asignaturaRepository.findAll().stream()
                    .filter(a -> a.getCodigo().startsWith(PREFIJO_CODIGO_PROGRAMA))
                    .toList());
            asignaturaRepository.flush();

            programaRepository.deleteAll(programaRepository.findAll().stream()
                    .filter(p -> p.getCodigo().startsWith(PREFIJO_CODIGO_PROGRAMA))
                    .toList());
            programaRepository.flush();

            facultadRepository.deleteAll(facultadRepository.findAll().stream()
                    .filter(f -> f.getNombre().startsWith(PREFIJO_NOMBRE))
                    .toList());
            facultadRepository.flush();

            // Cada subtipo concreto se consulta por su propio repositorio (y Administrador por JPQL,
            // que no tiene uno): Usuario es abstracta (herencia JOINED).
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
    @DisplayName("IT-01 Registrar: se guarda ACTIVA y sin espacios sobrantes; nombre duplicado da 409 sin crear fila")
    void it01Registrar() throws Exception {
        MvcResult creada = enviar(post(URL), json("  " + nombre("Ingeniería") + "  "));
        String cuerpoCreada = cuerpo(creada);
        assertThat(creada.getResponse().getStatus()).isEqualTo(201);
        assertThat((String) JsonPath.read(cuerpoCreada, "$.nombre")).isEqualTo(nombre("Ingeniería"));
        assertThat((String) JsonPath.read(cuerpoCreada, "$.estado")).isEqualTo("ACTIVO");

        Facultad enBase = facultadRepository.findById(UUID.fromString(JsonPath.read(cuerpoCreada, "$.id")))
                .orElseThrow();
        assertThat(enBase.getNombre()).isEqualTo(nombre("Ingeniería"));
        assertThat(enBase.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);

        // Nombre duplicado con mayúsculas distintas: 409, no se crea otra fila.
        MvcResult duplicado = enviar(post(URL), json(nombre("Ingeniería").toUpperCase()));
        assertThat(duplicado.getResponse().getStatus()).isEqualTo(409);
        assertSinDetallesTecnicos(cuerpo(duplicado));
        assertThat(contarReservadas()).isEqualTo(1);
    }

    // ---------------------------------------------------------------- IT-02

    static Stream<Arguments> entradasInvalidas() {
        return Stream.of(
                Arguments.of("nombre vacío", json("")),
                Arguments.of("nombre nulo", json(null)),
                Arguments.of("nombre solo espacios", json("    ")),
                Arguments.of("nombre de 151 caracteres", json(PREFIJO_NOMBRE + "A".repeat(151 - PREFIJO_NOMBRE.length()))),
                Arguments.of("cuerpo vacío", "{}"),
                Arguments.of("JSON mal formado", "{\"nombre\": "));
    }

    @ParameterizedTest(name = "IT-02 {0} -> 400 sin guardar nada")
    @MethodSource("entradasInvalidas")
    @DisplayName("IT-02 Entradas inválidas: 400, sin detalles técnicos y no se guarda nada")
    void it02EntradasInvalidas(String caso, String cuerpoPeticion) throws Exception {
        long antes = facultadRepository.count();

        MvcResult r = enviar(post(URL), cuerpoPeticion);

        assertThat(r.getResponse().getStatus()).as(caso).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(r));
        assertThat(facultadRepository.count()).as(caso).isEqualTo(antes);
        assertThat(contarReservadas()).as(caso).isZero();
    }

    // ---------------------------------------------------------------- IT-03

    @Test
    @DisplayName("IT-03 Consultar: filtros por nombre (sin tildes) y estado contra datos reales; por id existente, inexistente e inválido")
    void it03Consultar() throws Exception {
        Facultad ingenieria = guardar(nombre("Ingeniería"));
        Facultad derecho = guardar(nombre("Derecho"));
        Facultad medicina = guardarInactiva(nombre("Medicina"));

        // Por nombre parcial, sin tildes ni mayúsculas. El texto incluye el prefijo reservado para no
        // depender de que la base esté libre de facultades reales del equipo.
        MvcResult porNombre = enviar(get(URL + "?nombre=itf facultad INGENIERIA"), null);
        assertThat(porNombre.getResponse().getStatus()).isEqualTo(200);
        List<String> nombres = JsonPath.read(cuerpo(porNombre), "$[*].nombre");
        assertThat(nombres).containsExactly(ingenieria.getNombre());

        // Todas las reservadas, ordenadas por nombre.
        MvcResult todas = enviar(get(URL + "?nombre=" + PREFIJO_NOMBRE.trim()), null);
        List<String> nombresTodas = JsonPath.read(cuerpo(todas), "$[*].nombre");
        assertThat(nombresTodas).containsExactly(derecho.getNombre(), ingenieria.getNombre(), medicina.getNombre());

        // Por estado: hay datos reales en la base, así que se compara solo lo reservado.
        MvcResult inactivas = enviar(get(URL + "?estado=INACTIVO"), null);
        List<String> nombresInactivas = JsonPath.read(cuerpo(inactivas), "$[*].nombre");
        assertThat(nombresInactivas.stream().filter(n -> n.startsWith(PREFIJO_NOMBRE)).toList())
                .containsExactly(medicina.getNombre());

        // Combinado nombre + estado, sin resultados.
        MvcResult combinado = enviar(get(URL + "?nombre=itf facultad derecho&estado=INACTIVO"), null);
        assertThat((List<?>) JsonPath.read(cuerpo(combinado), "$[*]")).isEmpty();

        // Estado inválido: 400.
        MvcResult estadoInvalido = enviar(get(URL + "?estado=BORRADO"), null);
        assertThat(estadoInvalido.getResponse().getStatus()).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(estadoInvalido));

        // Por id existente.
        MvcResult porId = enviar(get(URL + "/" + derecho.getId()), null);
        assertThat(porId.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(porId), "$.nombre")).isEqualTo(derecho.getNombre());

        // Por id inexistente.
        MvcResult inexistente = enviar(get(URL + "/" + UUID.randomUUID()), null);
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(inexistente));

        // Por id que no es UUID.
        MvcResult idInvalido = enviar(get(URL + "/no-es-uuid"), null);
        assertThat(idInvalido.getResponse().getStatus()).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(idInvalido));
    }

    // ---------------------------------------------------------------- IT-04

    @Test
    @DisplayName("IT-04 Modificar nombre: se guarda; nombre de otra facultad da 409; inválido da 400; id inexistente da 404")
    void it04ModificarNombre() throws Exception {
        Facultad uno = guardar(nombre("Uno"));
        Facultad dos = guardar(nombre("Dos"));

        MvcResult modificada = enviar(put(URL + "/" + uno.getId()), json(nombre("Uno Renombrada")));
        assertThat(modificada.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(modificada), "$.nombre")).isEqualTo(nombre("Uno Renombrada"));
        assertThat(facultadRepository.findById(uno.getId()).orElseThrow().getNombre())
                .isEqualTo(nombre("Uno Renombrada"));

        // Cambiar solo mayúsculas de su propio nombre está permitido.
        MvcResult mismasLetras = enviar(put(URL + "/" + uno.getId()), json(nombre("UNO RENOMBRADA")));
        assertThat(mismasLetras.getResponse().getStatus()).isEqualTo(200);

        // Nombre de otra facultad (con mayúsculas distintas): 409, sin cambios.
        MvcResult duplicado = enviar(put(URL + "/" + uno.getId()), json(dos.getNombre().toLowerCase()));
        assertThat(duplicado.getResponse().getStatus()).isEqualTo(409);
        assertSinDetallesTecnicos(cuerpo(duplicado));
        assertThat(facultadRepository.findById(uno.getId()).orElseThrow().getNombre())
                .isEqualTo(nombre("UNO RENOMBRADA"));

        // Nombre vacío: 400.
        MvcResult vacio = enviar(put(URL + "/" + uno.getId()), json(""));
        assertThat(vacio.getResponse().getStatus()).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(vacio));

        // Id inexistente: 404.
        MvcResult inexistente = enviar(put(URL + "/" + UUID.randomUUID()), json(nombre("Nueva")));
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(inexistente));
    }

    // ---------------------------------------------------------------- IT-05

    @Test
    @DisplayName("IT-05 Inactivar: pasa a INACTIVO; es idempotente; con programa ACTIVO da 409; con programas inactivos sí se puede; id inexistente da 404")
    void it05Inactivar() throws Exception {
        Facultad sinProgramas = guardar(nombre("Sin Programas"));

        MvcResult inactivada = enviar(patch(URL + "/" + sinProgramas.getId() + "/inactivar"), null);
        assertThat(inactivada.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(inactivada), "$.estado")).isEqualTo("INACTIVO");
        assertThat(facultadRepository.findById(sinProgramas.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoRegistro.INACTIVO);

        // Ya inactiva: idempotente, sigue en 200.
        MvcResult repetida = enviar(patch(URL + "/" + sinProgramas.getId() + "/inactivar"), null);
        assertThat(repetida.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(repetida), "$.estado")).isEqualTo("INACTIVO");

        // Con un programa ACTIVO: 409 y la facultad sigue ACTIVA.
        Facultad conProgramaActivo = guardar(nombre("Con Programa"));
        ProgramaAcademico programa = guardarPrograma("ITF-P01", conProgramaActivo);

        MvcResult conflicto = enviar(patch(URL + "/" + conProgramaActivo.getId() + "/inactivar"), null);
        assertThat(conflicto.getResponse().getStatus()).isEqualTo(409);
        assertSinDetallesTecnicos(cuerpo(conflicto));
        assertThat(facultadRepository.findById(conProgramaActivo.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoRegistro.ACTIVO);

        // Si el programa pasa a INACTIVO, la facultad ya se puede inactivar.
        programa.setEstado(EstadoRegistro.INACTIVO);
        programaRepository.save(programa);

        MvcResult ahoraSi = enviar(patch(URL + "/" + conProgramaActivo.getId() + "/inactivar"), null);
        assertThat(ahoraSi.getResponse().getStatus()).isEqualTo(200);
        assertThat(facultadRepository.findById(conProgramaActivo.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoRegistro.INACTIVO);

        // Id inexistente: 404.
        MvcResult inexistente = enviar(patch(URL + "/" + UUID.randomUUID() + "/inactivar"), null);
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(inexistente));
    }

    // ---------------------------------------------------------------- IT-06

    @Test
    @DisplayName("IT-06 Programas: se registran en una facultad ACTIVA; facultad INACTIVA da 409 e inexistente 404, sin crear programa")
    void it06RegistrarProgramaEnFacultad() throws Exception {
        Facultad activa = guardar(nombre("Activa"));
        Facultad inactiva = guardarInactiva(nombre("Inactiva"));

        MvcResult creado = enviar(post(URL_PROGRAMAS), jsonPrograma("Uno", "itf-u01", activa.getId()));
        String cuerpoCreado = cuerpo(creado);
        assertThat(creado.getResponse().getStatus()).isEqualTo(201);
        assertThat((String) JsonPath.read(cuerpoCreado, "$.codigo")).isEqualTo("ITF-U01");
        assertThat((String) JsonPath.read(cuerpoCreado, "$.facultadId")).isEqualTo(activa.getId().toString());
        assertThat((String) JsonPath.read(cuerpoCreado, "$.facultadNombre")).isEqualTo(activa.getNombre());

        ProgramaAcademico enBase = programaRepository.findById(UUID.fromString(JsonPath.read(cuerpoCreado, "$.id")))
                .orElseThrow();
        assertThat(enBase.getCodigo()).isEqualTo("ITF-U01");

        // La consulta de programas también expone la facultad.
        MvcResult consulta = enviar(get(URL_PROGRAMAS + "/" + enBase.getId()), null);
        assertThat((String) JsonPath.read(cuerpo(consulta), "$.facultadNombre")).isEqualTo(activa.getNombre());

        // Facultad inactiva: 409, no se crea el programa.
        MvcResult enInactiva = enviar(post(URL_PROGRAMAS), jsonPrograma("Dos", "ITF-U02", inactiva.getId()));
        assertThat(enInactiva.getResponse().getStatus()).isEqualTo(409);
        assertSinDetallesTecnicos(cuerpo(enInactiva));

        // Facultad inexistente: 404, no se crea el programa.
        MvcResult enInexistente = enviar(post(URL_PROGRAMAS), jsonPrograma("Tres", "ITF-U03", UUID.randomUUID()));
        assertThat(enInexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(enInexistente));

        // Sin facultad: 400.
        MvcResult sinFacultad = enviar(post(URL_PROGRAMAS),
                "{\"nombre\":\"" + PREFIJO_NOMBRE_PROGRAMA + "Cuatro\",\"codigo\":\"ITF-U04\"}");
        assertThat(sinFacultad.getResponse().getStatus()).isEqualTo(400);

        assertThat(programaRepository.findAll().stream()
                .filter(p -> p.getCodigo().startsWith(PREFIJO_CODIGO_PROGRAMA))
                .count()).isEqualTo(1);
    }

    // ---------------------------------------------------------------- IT-08

    @Test
    @DisplayName("IT-08 Filtrar programas por facultad y reasignarlos: solo a una facultad ACTIVA; reenviar la actual inactiva no falla")
    void it08FiltrarYReasignarProgramas() throws Exception {
        Facultad origen = guardar(nombre("Origen"));
        Facultad destino = guardar(nombre("Destino"));
        Facultad cerrada = guardarInactiva(nombre("Cerrada"));
        ProgramaAcademico uno = guardarPrograma("ITF-R01", origen);
        ProgramaAcademico dos = guardarPrograma("ITF-R02", origen);
        ProgramaAcademico otro = guardarPrograma("ITF-R03", destino);

        // Filtro por facultad: solo los programas de esa facultad.
        MvcResult deOrigen = enviar(get(URL_PROGRAMAS + "?facultadId=" + origen.getId()), null);
        assertThat(deOrigen.getResponse().getStatus()).isEqualTo(200);
        List<String> codigosOrigen = JsonPath.read(cuerpo(deOrigen), "$[*].codigo");
        assertThat(codigosOrigen).containsExactlyInAnyOrder(uno.getCodigo(), dos.getCodigo());

        // Combinado con estado y con una facultad sin programas.
        MvcResult inactivosDeOrigen = enviar(get(URL_PROGRAMAS + "?facultadId=" + origen.getId() + "&estado=INACTIVO"), null);
        assertThat((List<?>) JsonPath.read(cuerpo(inactivosDeOrigen), "$[*]")).isEmpty();
        MvcResult deCerrada = enviar(get(URL_PROGRAMAS + "?facultadId=" + cerrada.getId()), null);
        assertThat((List<?>) JsonPath.read(cuerpo(deCerrada), "$[*]")).isEmpty();

        // facultadId que no es UUID: 400.
        MvcResult facultadInvalida = enviar(get(URL_PROGRAMAS + "?facultadId=no-es-uuid"), null);
        assertThat(facultadInvalida.getResponse().getStatus()).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(facultadInvalida));

        // Reasignar "uno" a la facultad destino.
        MvcResult reasignado = enviar(put(URL_PROGRAMAS + "/" + uno.getId()),
                jsonActualizarPrograma(uno.getNombre(), destino.getId()));
        assertThat(reasignado.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(reasignado), "$.facultadId")).isEqualTo(destino.getId().toString());
        assertThat((String) JsonPath.read(cuerpo(reasignado), "$.codigo")).isEqualTo("ITF-R01");
        assertThat(programaRepository.findById(uno.getId()).orElseThrow().getFacultad().getId()).isEqualTo(destino.getId());

        MvcResult deDestino = enviar(get(URL_PROGRAMAS + "?facultadId=" + destino.getId()), null);
        List<String> codigosDestino = JsonPath.read(cuerpo(deDestino), "$[*].codigo");
        assertThat(codigosDestino).containsExactlyInAnyOrder(uno.getCodigo(), otro.getCodigo());

        // Hacia una facultad inactiva: 409 y no se mueve.
        MvcResult haciaCerrada = enviar(put(URL_PROGRAMAS + "/" + dos.getId()),
                jsonActualizarPrograma(dos.getNombre(), cerrada.getId()));
        assertThat(haciaCerrada.getResponse().getStatus()).isEqualTo(409);
        assertSinDetallesTecnicos(cuerpo(haciaCerrada));
        assertThat(programaRepository.findById(dos.getId()).orElseThrow().getFacultad().getId()).isEqualTo(origen.getId());

        // Hacia una facultad inexistente: 404 y no se mueve.
        MvcResult haciaInexistente = enviar(put(URL_PROGRAMAS + "/" + dos.getId()),
                jsonActualizarPrograma(dos.getNombre(), UUID.randomUUID()));
        assertThat(haciaInexistente.getResponse().getStatus()).isEqualTo(404);
        assertSinDetallesTecnicos(cuerpo(haciaInexistente));
        assertThat(programaRepository.findById(dos.getId()).orElseThrow().getFacultad().getId()).isEqualTo(origen.getId());

        // Sin facultadId solo cambia el nombre y conserva la facultad.
        MvcResult soloNombre = enviar(put(URL_PROGRAMAS + "/" + dos.getId()),
                "{\"nombre\":" + texto(PREFIJO_NOMBRE_PROGRAMA + "R02 Renombrado") + "}");
        assertThat(soloNombre.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(soloNombre), "$.facultadId")).isEqualTo(origen.getId().toString());

        // Si la facultad actual se inactiva, reenviarla al renombrar no falla.
        programaRepository.save(conEstado(dos, EstadoRegistro.INACTIVO));
        enviar(patch(URL + "/" + origen.getId() + "/inactivar"), null);
        MvcResult reenviaInactiva = enviar(put(URL_PROGRAMAS + "/" + dos.getId()),
                jsonActualizarPrograma(PREFIJO_NOMBRE_PROGRAMA + "R02 Otra Vez", origen.getId()));
        assertThat(reenviaInactiva.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(reenviaInactiva), "$.facultadId")).isEqualTo(origen.getId().toString());
    }

    // ---------------------------------------------------------------- IT-09

    @Test
    @DisplayName("IT-09 Facultad en asignaturas y reportes: campos nuevos y filtro facultadId respetando el alcance del profesor")
    void it09FacultadEnAsignaturasYReportes() throws Exception {
        Facultad ingenieria = guardar(nombre("Ingenieria A"));
        Facultad derecho = guardar(nombre("Derecho A"));
        ProgramaAcademico sistemas = guardarPrograma("ITF-S01", ingenieria);
        ProgramaAcademico leyes = guardarPrograma("ITF-S02", derecho);
        Asignatura programacion = asignaturaRepository.save(new Asignatura("ITF-A01", "ITF Programacion", sistemas));
        Asignatura civil = asignaturaRepository.save(new Asignatura("ITF-A02", "ITF Derecho Civil", leyes));
        Asignatura bases = asignaturaRepository.save(new Asignatura("ITF-A03", "ITF Bases de Datos", sistemas));

        Profesor profesor = crearProfesor();
        asignar(profesor, programacion);
        asignar(profesor, civil);
        String tokenProfesor = token(profesor);

        // El profesor ve la facultad de sus asignaturas y filtra dentro de su alcance.
        MvcResult todasProfesor = enviar(get(URL_ASIGNATURAS), tokenProfesor, null);
        assertThat(todasProfesor.getResponse().getStatus()).isEqualTo(200);
        List<String> facultadesProfesor = JsonPath.read(cuerpo(todasProfesor), "$[*].facultadNombre");
        assertThat(facultadesProfesor).containsExactlyInAnyOrder(ingenieria.getNombre(), derecho.getNombre());

        MvcResult profesorIngenieria = enviar(get(URL_ASIGNATURAS + "?facultadId=" + ingenieria.getId()), tokenProfesor, null);
        List<String> codigosProfesorIngenieria = JsonPath.read(cuerpo(profesorIngenieria), "$[*].codigo");
        assertThat(codigosProfesorIngenieria).containsExactly(programacion.getCodigo());

        MvcResult miasDerecho = enviar(get(URL_ASIGNATURAS + "/mias?facultadId=" + derecho.getId()), tokenProfesor, null);
        assertThat(miasDerecho.getResponse().getStatus()).isEqualTo(200);
        assertThat((List<String>) JsonPath.read(cuerpo(miasDerecho), "$[*].codigo")).containsExactly(civil.getCodigo());
        assertThat((String) JsonPath.read(cuerpo(miasDerecho), "$[0].facultadId")).isEqualTo(derecho.getId().toString());

        MvcResult miasOtra = enviar(get(URL_ASIGNATURAS + "/mias?facultadId=" + UUID.randomUUID()), tokenProfesor, null);
        assertThat((List<?>) JsonPath.read(cuerpo(miasOtra), "$[*]")).isEmpty();

        // El administrador filtra todo el catálogo por facultad, combinado con programa.
        MvcResult adminIngenieria = enviar(get(URL_ASIGNATURAS + "?facultadId=" + ingenieria.getId()), null);
        List<String> codigosAdmin = JsonPath.read(cuerpo(adminIngenieria), "$[*].codigo");
        assertThat(codigosAdmin).containsExactlyInAnyOrder(programacion.getCodigo(), bases.getCodigo());

        MvcResult programaDeOtraFacultad = enviar(get(URL_ASIGNATURAS + "?facultadId=" + ingenieria.getId()
                + "&programaId=" + leyes.getId()), null);
        assertThat(programaDeOtraFacultad.getResponse().getStatus()).isEqualTo(200);
        assertThat((List<?>) JsonPath.read(cuerpo(programaDeOtraFacultad), "$[*]")).isEmpty();

        MvcResult facultadInvalida = enviar(get(URL_ASIGNATURAS + "?facultadId=no-es-uuid"), null);
        assertThat(facultadInvalida.getResponse().getStatus()).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(facultadInvalida));

        // El reporte R1 acepta el filtro; lo que devuelva es de esa facultad.
        MvcResult reporte = enviar(get(URL_REPORTE_R1 + "?facultadId=" + ingenieria.getId()), tokenProfesor, null);
        assertThat(reporte.getResponse().getStatus()).isEqualTo(200);
        List<String> facultadesReporte = JsonPath.read(cuerpo(reporte), "$.asignaturas[*].facultadId");
        assertThat(facultadesReporte).allMatch(id -> id.equals(ingenieria.getId().toString()));
        MvcResult reporteInvalido = enviar(get(URL_REPORTE_R1 + "?facultadId=abc"), tokenProfesor, null);
        assertThat(reporteInvalido.getResponse().getStatus()).isEqualTo(400);

        // El reporte R6 (riesgo académico) también acepta el filtro.
        MvcResult riesgo = enviar(get(URL_REPORTE_R6 + "?facultadId=" + ingenieria.getId()), tokenProfesor, null);
        assertThat(riesgo.getResponse().getStatus()).isEqualTo(200);
        List<String> facultadesRiesgo = JsonPath.read(cuerpo(riesgo), "$.estudiantes[*].facultadId");
        assertThat(facultadesRiesgo).allMatch(id -> id.equals(ingenieria.getId().toString()));
        MvcResult riesgoInvalido = enviar(get(URL_REPORTE_R6 + "?facultadId=abc"), tokenProfesor, null);
        assertThat(riesgoInvalido.getResponse().getStatus()).isEqualTo(400);

        // La seguridad de programas y facultades no cambia: el profesor sigue sin acceso.
        assertEstado("GET /programas como profesor", 403, enviar(get(URL_PROGRAMAS), tokenProfesor, null));
        assertEstado("GET /facultades como profesor", 403, enviar(get(URL), tokenProfesor, null));
    }

    // ---------------------------------------------------------------- IT-07

    /** Una llamada a uno de los cinco endpoints; el builder se crea de nuevo en cada envío. */
    private record Llamada(String nombre, Supplier<MockHttpServletRequestBuilder> peticion, String cuerpo) {
    }

    /** Los cinco endpoints del módulo, apuntando a la facultad de apoyo {@code objetivo}. */
    private List<Llamada> cincoEndpoints(Facultad objetivo) {
        UUID id = objetivo.getId();
        return List.of(
                new Llamada("POST", () -> post(URL), json(nombre("Seguridad"))),
                new Llamada("GET lista", () -> get(URL), null),
                new Llamada("GET por id", () -> get(URL + "/" + id), null),
                new Llamada("PUT", () -> put(URL + "/" + id), json(nombre("Cambiada"))),
                new Llamada("PATCH inactivar", () -> patch(URL + "/" + id + "/inactivar"), null)
        );
    }

    @Test
    @DisplayName("IT-07a Sin token o con token alterado: 401 en los cinco endpoints y la base no cambia")
    void it07aSinTokenOTokenInvalido() throws Exception {
        Facultad objetivo = guardar(nombre("Apoyo"));
        long facultadesAntes = facultadRepository.count();

        // Token del administrador con la firma alterada: el filtro debe rechazarlo aunque los claims
        // sean los de un usuario real y activo.
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

        assertSinCambios(objetivo, facultadesAntes);
    }

    @Test
    @DisplayName("IT-07b Un PROFESOR con token real no puede usar ningún endpoint de facultades (403)")
    void it07bProfesorConTokenReal() throws Exception {
        verificarRolSinAcceso(token(crearProfesor()));
    }

    @Test
    @DisplayName("IT-07c Un ESTUDIANTE con token real no puede usar ningún endpoint de facultades (403)")
    void it07cEstudianteConTokenReal() throws Exception {
        Estudiante estudiante = new Estudiante();
        estudiante.setNumeroDocumento("ES" + System.nanoTime() % 100000000L);
        verificarRolSinAcceso(token(crearUsuario(estudiante, "est")));
    }

    /**
     * Con el token recibido llama a los cinco endpoints: todos deben dar 403 sin detalles técnicos y
     * ninguno debe modificar la base. La facultad de apoyo se crea por repositorio porque la API le
     * está vedada a estos roles.
     */
    private void verificarRolSinAcceso(String token) throws Exception {
        Facultad objetivo = guardar(nombre("Sin Acceso"));
        long facultadesAntes = facultadRepository.count();

        for (Llamada llamada : cincoEndpoints(objetivo)) {
            assertEstado(llamada.nombre(), 403, enviar(llamada.peticion().get(), token, llamada.cuerpo()));
        }

        assertSinCambios(objetivo, facultadesAntes);
    }

    private void assertSinCambios(Facultad objetivo, long facultadesAntes) {
        assertThat(facultadRepository.count()).isEqualTo(facultadesAntes);

        Facultad despues = facultadRepository.findById(objetivo.getId()).orElseThrow();
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

    /** Guarda un usuario de la prueba (correo itf-fac-...) con el tipo de documento propio. */
    private <U extends Usuario> U crearUsuario(U usuario, String alias) {
        usuario.setTipoDocumento(tipoDocumento);
        NombresPersonaTestUtil.aplicar(usuario, "ITF Usuario " + alias);
        usuario.setCorreoInstitucional(PREFIJO_CORREO + alias + "-" + UUID.randomUUID() + DOMINIO_CORREO);
        usuario.setEstado(EstadoRegistro.ACTIVO);
        return usuarioRepository.save(usuario);
    }

    private Profesor crearProfesor() {
        Profesor p = new Profesor();
        p.setTipoDocumento(tipoDocumento);
        p.setNumeroDocumento("PF" + System.nanoTime() % 100000000L);
        NombresPersonaTestUtil.aplicar(p, "ITF Profesor Facultad");
        p.setCorreoInstitucional(PREFIJO_CORREO + UUID.randomUUID() + DOMINIO_CORREO);
        p.setEstado(EstadoRegistro.ACTIVO);
        return profesorRepository.save(p);
    }

    private void asignar(Profesor profesor, Asignatura asignatura) {
        AsignacionDocente asignacion = new AsignacionDocente();
        asignacion.setProfesor(profesor);
        asignacion.setAsignatura(asignatura);
        asignacion.setEstado(EstadoRegistro.ACTIVO);
        asignacionRepository.save(asignacion);
    }

    private Facultad guardar(String nombre) {
        Facultad facultad = new Facultad();
        facultad.setNombre(nombre);
        facultad.setEstado(EstadoRegistro.ACTIVO);
        return facultadRepository.save(facultad);
    }

    private Facultad guardarInactiva(String nombre) {
        Facultad facultad = guardar(nombre);
        facultad.setEstado(EstadoRegistro.INACTIVO);
        return facultadRepository.save(facultad);
    }

    private ProgramaAcademico guardarPrograma(String codigo, Facultad facultad) {
        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setNombre(PREFIJO_NOMBRE_PROGRAMA + codigo);
        programa.setCodigo(codigo);
        programa.setFacultad(facultad);
        programa.setEstado(EstadoRegistro.ACTIVO);
        return programaRepository.save(programa);
    }

    /** Envía como ADMINISTRADOR (con su JWT real): es el caso de IT-01 a IT-06. */
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

    private long contarReservadas() {
        return facultadRepository.findAll().stream()
                .filter(f -> f.getNombre().startsWith(PREFIJO_NOMBRE))
                .count();
    }

    private static String nombre(String sufijo) {
        return PREFIJO_NOMBRE + sufijo;
    }

    private static String json(String nombre) {
        return "{\"nombre\":" + texto(nombre) + "}";
    }

    private static String jsonPrograma(String sufijo, String codigo, UUID facultadId) {
        return "{\"nombre\":" + texto(PREFIJO_NOMBRE_PROGRAMA + sufijo) + ",\"codigo\":" + texto(codigo)
                + ",\"facultadId\":\"" + facultadId + "\"}";
    }

    private static String jsonActualizarPrograma(String nombre, UUID facultadId) {
        return "{\"nombre\":" + texto(nombre) + ",\"facultadId\":\"" + facultadId + "\"}";
    }

    private ProgramaAcademico conEstado(ProgramaAcademico programa, EstadoRegistro estado) {
        ProgramaAcademico actual = programaRepository.findById(programa.getId()).orElseThrow();
        actual.setEstado(estado);
        return actual;
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor.replace("\"", "\\\"") + "\"";
    }
}
