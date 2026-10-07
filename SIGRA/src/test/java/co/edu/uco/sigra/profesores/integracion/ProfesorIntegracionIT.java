package co.edu.uco.sigra.profesores.integracion;

import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.profesores.repository.ProfesorRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Pruebas de integración del módulo Profesores.
 *
 * Levanta el contexto completo de Spring Boot contra PostgreSQL real y utiliza
 * MockMvc para llamar a los endpoints.
 *
 * No utiliza @Transactional en la prueba: los servicios confirman realmente
 * los cambios en la base de datos.
 *
 * Todos los datos propios utilizan prefijos ITP / itp- y son eliminados
 * después de cada caso.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class ProfesorIntegracionIT {

    private static final String URL = "/api/v1/profesores";

    private static final String PREFIJO_CORREO = "itp-";
    private static final String DOMINIO_CORREO = "@uco.net.co";
    private static final String PREFIJO_PROGRAMA = "ITP Programa ";
    private static final String PREFIJO_ASIGNATURA = "ITP-ASIG-";
    private static final String TIPO_DOCUMENTO = "ITP Tipo";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Autowired
    private ProgramaAcademicoRepository programaRepository;

    @Autowired
    private AsignaturaRepository asignaturaRepository;

    @Autowired
    private AsignacionDocenteRepository asignacionRepository;

    private TransactionTemplate tx;

    private TipoDocumento tipoDocumento;

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(transactionManager);

        limpiar();

        TipoDocumento tipo = new TipoDocumento();
        tipo.setNombre(TIPO_DOCUMENTO);
        tipoDocumento = tipoDocumentoRepository.save(tipo);
    }

    @AfterEach
    void limpiar() {
        if (tx == null) {
            tx = new TransactionTemplate(transactionManager);
        }

        tx.executeWithoutResult(status -> {
            Predicate<Asignatura> asignaturaItp =
                    a -> a.getCodigo() != null
                            && a.getCodigo().startsWith(PREFIJO_ASIGNATURA);

            // Un usuario es "de la prueba" si su correo lleva el prefijo ITP o si usa el
            // tipo de documento creado por la prueba. El segundo criterio evita que un
            // registro con otro correo deje referenciado el tipo y rompa la limpieza.
            Predicate<Usuario> usuarioItp = u ->
                    (u.getCorreoInstitucional() != null
                            && u.getCorreoInstitucional().startsWith(PREFIJO_CORREO)
                            && u.getCorreoInstitucional().endsWith(DOMINIO_CORREO))
                            || (u.getTipoDocumento() != null
                            && TIPO_DOCUMENTO.equals(u.getTipoDocumento().getNombre()));

            // Primero las asignaciones porque tienen FK hacia profesor y asignatura.
            List<AsignacionDocente> asignacionesPropias =
                    asignacionRepository.findAll().stream()
                            .filter(a ->
                                    usuarioItp.test(a.getProfesor())
                                            || asignaturaItp.test(a.getAsignatura()))
                            .toList();

            asignacionRepository.deleteAll(asignacionesPropias);
            asignacionRepository.flush();

            // Después las asignaturas propias.
            List<Asignatura> asignaturasPropias =
                    asignaturaRepository.findAll().stream()
                            .filter(asignaturaItp)
                            .toList();

            asignaturaRepository.deleteAll(asignaturasPropias);
            asignaturaRepository.flush();

            // Después los programas de apoyo.
            List<ProgramaAcademico> programasPropios =
                    programaRepository.findAll().stream()
                            .filter(p -> p.getNombre() != null
                                    && p.getNombre().startsWith(PREFIJO_PROGRAMA))
                            .toList();

            programaRepository.deleteAll(programasPropios);
            programaRepository.flush();

            // Finalmente los usuarios/profesores propios.
            List<co.edu.uco.sigra.auth.entity.Usuario> usuariosPropios =
                    usuarioRepository.findAll().stream()
                            .filter(usuarioItp)
                            .toList();

            usuarioRepository.deleteAll(usuariosPropios);
            usuarioRepository.flush();

            // Y el tipo de documento creado exclusivamente para estas pruebas.
            tipoDocumentoRepository.deleteAll(
                    tipoDocumentoRepository.findAll().stream()
                            .filter(t -> TIPO_DOCUMENTO.equals(t.getNombre()))
                            .toList()
            );

            tipoDocumentoRepository.flush();
        });
    }

    // -------------------------------------------------------------------------
    // IT-01
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("IT-01 Registrar profesor, normalizar correo y rechazar duplicados")
    void it01RegistrarProfesorYRechazarDuplicados() throws Exception {

        MvcResult creado = enviar(
                post(URL),
                json(
                        tipoDocumento.getId(),
                        "990000001",
                        "ITP Profesor Uno",
                        "  ITP-Uno@UCO.NET.CO "
                )
        );

        assertThat(creado.getResponse().getStatus()).isEqualTo(201);

        String cuerpoCreado = cuerpo(creado);

        UUID profesorId = UUID.fromString(
                JsonPath.read(cuerpoCreado, "$.id")
        );

        assertThat((String) JsonPath.read(cuerpoCreado, "$.numeroDocumento"))
                .isEqualTo("990000001");

        assertThat((String) JsonPath.read(cuerpoCreado, "$.nombreCompleto"))
                .isEqualTo("ITP Profesor Uno");

        assertThat((String) JsonPath.read(cuerpoCreado, "$.correoInstitucional"))
                .isEqualTo("itp-uno@uco.net.co");

        assertThat((String) JsonPath.read(cuerpoCreado, "$.estado"))
                .isEqualTo("ACTIVO");

        assertThat((Boolean) JsonPath.read(cuerpoCreado, "$.tieneAsignacionesActivas"))
                .isFalse();

        Profesor enBase = profesorRepository.findById(profesorId).orElseThrow();

        assertThat(enBase.getNumeroDocumento())
                .isEqualTo("990000001");

        assertThat(enBase.getNombreCompleto())
                .isEqualTo("ITP Profesor Uno");

        assertThat(enBase.getCorreoInstitucional())
                .isEqualTo("itp-uno@uco.net.co");

        assertThat(enBase.getEstado())
                .isEqualTo(EstadoRegistro.ACTIVO);

        // Documento duplicado.
        MvcResult documentoDuplicado = enviar(
                post(URL),
                json(
                        tipoDocumento.getId(),
                        "990000001",
                        "ITP Profesor Duplicado",
                        "itp-duplicado@uco.net.co"
                )
        );

        assertThat(documentoDuplicado.getResponse().getStatus())
                .isEqualTo(409);

        assertSinDetallesTecnicos(cuerpo(documentoDuplicado));

        // El documento no debe haber creado una segunda fila.
        assertThat(
                profesorRepository.findAll().stream()
                        .filter(p -> "990000001".equals(p.getNumeroDocumento()))
                        .count()
        ).isEqualTo(1);

        // Correo duplicado.
        MvcResult correoDuplicado = enviar(
                post(URL),
                json(
                        tipoDocumento.getId(),
                        "990000002",
                        "ITP Profesor Correo Duplicado",
                        "ITP-UNO@UCO.NET.CO"
                )
        );

        assertThat(correoDuplicado.getResponse().getStatus())
                .isEqualTo(409);

        assertSinDetallesTecnicos(cuerpo(correoDuplicado));

        // Tampoco se crea una fila adicional por el correo duplicado.
        assertThat(
                profesorRepository.findAll().stream()
                        .filter(p -> "990000002".equals(p.getNumeroDocumento()))
                        .count()
        ).isZero();
    }

    // -------------------------------------------------------------------------
    // IT-02
    // -------------------------------------------------------------------------

    static Stream<Arguments> entradasInvalidas() {

        UUID tipoDoc = UUID.randomUUID();

        return Stream.of(
                Arguments.of(
                        "número con letras",
                        json(tipoDoc, "ABC12345", "ITP Profesor", "itp-valido@uco.net.co")
                ),
                Arguments.of(
                        "número demasiado corto",
                        json(tipoDoc, "12345", "ITP Profesor", "itp-valido@uco.net.co")
                ),
                Arguments.of(
                        "número demasiado largo",
                        json(tipoDoc, "12345678901", "ITP Profesor", "itp-valido@uco.net.co")
                ),
                Arguments.of(
                        "correo de dominio externo",
                        json(tipoDoc, "990000010", "ITP Profesor", "profesor@gmail.com")
                ),
                Arguments.of(
                        "correo inválido",
                        json(tipoDoc, "990000011", "ITP Profesor", "correo-invalido")
                ),
                Arguments.of(
                        "tipoDocumentoId nulo",
                        "{\"tipoDocumentoId\":null,\"numeroDocumento\":\"990000012\","
                                + "\"nombreCompleto\":\"ITP Profesor\","
                                + "\"correoInstitucional\":\"itp-012@uco.net.co\"}"
                ),
                Arguments.of(
                        "número de documento nulo",
                        "{\"tipoDocumentoId\":\"" + tipoDoc + "\","
                                + "\"numeroDocumento\":null,"
                                + "\"nombreCompleto\":\"ITP Profesor\","
                                + "\"correoInstitucional\":\"itp-013@uco.net.co\"}"
                ),
                Arguments.of(
                        "nombre vacío",
                        json(tipoDoc, "990000014", "", "itp-014@uco.net.co")
                ),
                Arguments.of(
                        "correo nulo",
                        "{\"tipoDocumentoId\":\"" + tipoDoc + "\","
                                + "\"numeroDocumento\":\"990000015\","
                                + "\"nombreCompleto\":\"ITP Profesor\","
                                + "\"correoInstitucional\":null}"
                )
        );
    }

    @ParameterizedTest(name = "IT-02 {0} -> 400")
    @MethodSource("entradasInvalidas")
    @DisplayName("IT-02 Entradas inválidas: 400 y no se crean profesores")
    void it02EntradasInvalidas(String caso, String cuerpoPeticion) throws Exception {

        long profesoresAntes = profesorRepository.count();

        MvcResult resultado = enviar(
                post(URL),
                cuerpoPeticion
        );

        assertThat(resultado.getResponse().getStatus())
                .as(caso)
                .isEqualTo(400);

        assertSinDetallesTecnicos(cuerpo(resultado));

        assertThat(profesorRepository.count())
                .as(caso)
                .isEqualTo(profesoresAntes);
    }

    // -------------------------------------------------------------------------
    // IT-03
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("IT-03 Modificar profesor: permite nombre/correo y protege documento")
    void it03ModificarProfesor() throws Exception {

        MvcResult creado = enviar(
                post(URL),
                json(
                        tipoDocumento.getId(),
                        "990000020",
                        "ITP Profesor Original",
                        "itp-020@uco.net.co"
                )
        );

        assertThat(creado.getResponse().getStatus()).isEqualTo(201);

        UUID profesorId = UUID.fromString(
                JsonPath.read(cuerpo(creado), "$.id")
        );

        // Modificación válida.
        MvcResult modificado = enviar(
                put(URL + "/" + profesorId),
                json(
                        tipoDocumento.getId(),
                        "990000020",
                        "ITP Profesor Modificado",
                        "  ITP-020-NUEVO@UCO.NET.CO "
                )
        );

        assertThat(modificado.getResponse().getStatus())
                .isEqualTo(200);

        String cuerpoModificado = cuerpo(modificado);

        assertThat((String) JsonPath.read(cuerpoModificado, "$.nombreCompleto"))
                .isEqualTo("ITP Profesor Modificado");

        assertThat((String) JsonPath.read(cuerpoModificado, "$.correoInstitucional"))
                .isEqualTo("itp-020-nuevo@uco.net.co");

        assertThat((String) JsonPath.read(cuerpoModificado, "$.numeroDocumento"))
                .isEqualTo("990000020");

        Profesor enBase = profesorRepository.findById(profesorId).orElseThrow();

        assertThat(enBase.getNombreCompleto())
                .isEqualTo("ITP Profesor Modificado");

        assertThat(enBase.getCorreoInstitucional())
                .isEqualTo("itp-020-nuevo@uco.net.co");

        assertThat(enBase.getNumeroDocumento())
                .isEqualTo("990000020");

        // Intentar modificar el documento.
        MvcResult documentoNoModificable = enviar(
                put(URL + "/" + profesorId),
                json(
                        tipoDocumento.getId(),
                        "990000021",
                        "ITP Cambio Documento",
                        "itp-020-cambio@uco.net.co"
                )
        );

        assertThat(documentoNoModificable.getResponse().getStatus())
                .isEqualTo(400);

        assertSinDetallesTecnicos(cuerpo(documentoNoModificable));

        // El rechazo no debe modificar la fila.
        Profesor despuesDelRechazo =
                profesorRepository.findById(profesorId).orElseThrow();

        assertThat(despuesDelRechazo.getNumeroDocumento())
                .isEqualTo("990000020");

        assertThat(despuesDelRechazo.getNombreCompleto())
                .isEqualTo("ITP Profesor Modificado");

        assertThat(despuesDelRechazo.getCorreoInstitucional())
                .isEqualTo("itp-020-nuevo@uco.net.co");

        // Crear un segundo profesor para probar correo duplicado.
        MvcResult segundo = enviar(
                post(URL),
                json(
                        tipoDocumento.getId(),
                        "990000022",
                        "ITP Segundo Profesor",
                        "itp-022@uco.net.co"
                )
        );

        assertThat(segundo.getResponse().getStatus())
                .isEqualTo(201);

        UUID segundoId = UUID.fromString(
                JsonPath.read(cuerpo(segundo), "$.id")
        );

        // Intentar modificar el primero usando el correo del segundo.
        MvcResult correoDuplicado = enviar(
                put(URL + "/" + profesorId),
                json(
                        tipoDocumento.getId(),
                        "990000020",
                        "ITP Profesor Modificado",
                        "ITP-022@UCO.NET.CO"
                )
        );

        assertThat(correoDuplicado.getResponse().getStatus())
                .isEqualTo(409);

        assertSinDetallesTecnicos(cuerpo(correoDuplicado));

        // El correo del primero debe permanecer intacto.
        Profesor primeroDespues =
                profesorRepository.findById(profesorId).orElseThrow();

        assertThat(primeroDespues.getCorreoInstitucional())
                .isEqualTo("itp-020-nuevo@uco.net.co");

        // El segundo tampoco debe haber sido modificado.
        Profesor segundoEnBase =
                profesorRepository.findById(segundoId).orElseThrow();

        assertThat(segundoEnBase.getCorreoInstitucional())
                .isEqualTo("itp-022@uco.net.co");
    }

    // -------------------------------------------------------------------------
    // IT-04
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("IT-04 Inactivar profesor: rechaza si tiene asignaciones activas y permite hacerlo cuando no las tiene")
    void it04InactivarProfesor() throws Exception {

        // Crear profesor.
        MvcResult creado = enviar(
                post(URL),
                json(
                        tipoDocumento.getId(),
                        "990000030",
                        "ITP Profesor Asignado",
                        "itp-030@uco.net.co"
                )
        );

        assertThat(creado.getResponse().getStatus())
                .isEqualTo(201);

        UUID profesorId = UUID.fromString(
                JsonPath.read(cuerpo(creado), "$.id")
        );

        // Crear asignatura y programa de apoyo directamente por repositorio.
        ProgramaAcademico programa = crearPrograma("Principal");

        Asignatura asignatura =
                new Asignatura(
                        PREFIJO_ASIGNATURA + "1",
                        "ITP Asignatura Integracion",
                        programa
                );

        asignatura = asignaturaRepository.save(asignatura);

        // Crear una asignación docente ACTIVA.
        AsignacionDocente asignacion = new AsignacionDocente();
        asignacion.setProfesor(profesorRepository.findById(profesorId).orElseThrow());
        asignacion.setAsignatura(asignatura);
        asignacion.setEstado(EstadoRegistro.ACTIVO);

        asignacion = asignacionRepository.save(asignacion);

        // No debe permitir inactivar mientras exista una asignación activa.
        MvcResult rechazo = enviar(
                delete(URL + "/" + profesorId),
                null
        );

        assertThat(rechazo.getResponse().getStatus())
                .isEqualTo(400);

        assertSinDetallesTecnicos(cuerpo(rechazo));

        Profesor profesorTrasRechazo =
                profesorRepository.findById(profesorId).orElseThrow();

        assertThat(profesorTrasRechazo.getEstado())
                .isEqualTo(EstadoRegistro.ACTIVO);

        // La asignación continúa intacta.
        AsignacionDocente asignacionTrasRechazo =
                asignacionRepository.findById(asignacion.getId()).orElseThrow();

        assertThat(asignacionTrasRechazo.getEstado())
                .isEqualTo(EstadoRegistro.ACTIVO);

        // Desactivar la asignación manualmente.
        asignacionTrasRechazo.setEstado(EstadoRegistro.INACTIVO);
        asignacionRepository.save(asignacionTrasRechazo);
        asignacionRepository.flush();

        // Ahora sí debe poder inactivarse el profesor.
        MvcResult inactivado = enviar(
                delete(URL + "/" + profesorId),
                null
        );

        assertThat(inactivado.getResponse().getStatus())
                .isEqualTo(204);

        Profesor profesorInactivado =
                profesorRepository.findById(profesorId).orElseThrow();

        assertThat(profesorInactivado.getEstado())
                .isEqualTo(EstadoRegistro.INACTIVO);

        // La asignación no debe haberse eliminado.
        AsignacionDocente asignacionFinal =
                asignacionRepository.findById(asignacion.getId()).orElseThrow();

        assertThat(asignacionFinal.getEstado())
                .isEqualTo(EstadoRegistro.INACTIVO);

        // ID inexistente.
        UUID idInexistente = UUID.randomUUID();

        MvcResult inexistente = enviar(
                delete(URL + "/" + idInexistente),
                null
        );

        assertThat(inexistente.getResponse().getStatus())
                .isEqualTo(404);

        assertSinDetallesTecnicos(cuerpo(inexistente));
    }

    // -------------------------------------------------------------------------
    // IT-05
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("IT-05 Consultar profesores, filtrar y consultar asignaturas por estado")
    void it05ConsultarProfesoresYAsignaturas() throws Exception {

        // Crear dos profesores.
        MvcResult creadoUno = enviar(
                post(URL),
                json(
                        tipoDocumento.getId(),
                        "990000040",
                        "ITP Profesor Consulta Uno",
                        "itp-040@uco.net.co"
                )
        );

        assertThat(creadoUno.getResponse().getStatus())
                .isEqualTo(201);

        UUID profesorUnoId = UUID.fromString(
                JsonPath.read(cuerpo(creadoUno), "$.id")
        );

        MvcResult creadoDos = enviar(
                post(URL),
                json(
                        tipoDocumento.getId(),
                        "990000041",
                        "ITP Profesor Consulta Dos",
                        "itp-041@uco.net.co"
                )
        );

        assertThat(creadoDos.getResponse().getStatus())
                .isEqualTo(201);

        UUID profesorDosId = UUID.fromString(
                JsonPath.read(cuerpo(creadoDos), "$.id")
        );

        // Consulta general.
        MvcResult todos = enviar(
                get(URL),
                null
        );

        assertThat(todos.getResponse().getStatus())
                .isEqualTo(200);

        String cuerpoTodos = cuerpo(todos);

        List<?> profesores = JsonPath.read(cuerpoTodos, "$");

        assertThat(profesores)
                .anyMatch(item ->
                        ((String) ((java.util.Map<?, ?>) item)
                                .get("numeroDocumento"))
                                .equals("990000040"));

        assertThat(profesores)
                .anyMatch(item ->
                        ((String) ((java.util.Map<?, ?>) item)
                                .get("numeroDocumento"))
                                .equals("990000041"));

        // Filtro por documento.
        MvcResult filtroDocumento = enviar(
                get(URL + "?filtro=990000040"),
                null
        );

        assertThat(filtroDocumento.getResponse().getStatus())
                .isEqualTo(200);

        List<?> resultadoDocumento =
                JsonPath.read(cuerpo(filtroDocumento), "$");

        assertThat(resultadoDocumento)
                .hasSize(1);

        assertThat(
                ((java.util.Map<?, ?>) resultadoDocumento.get(0))
                        .get("numeroDocumento")
        ).isEqualTo("990000040");

        // Filtro por nombre.
        MvcResult filtroNombre = enviar(
                get(URL + "?filtro=Consulta Uno"),
                null
        );

        assertThat(filtroNombre.getResponse().getStatus())
                .isEqualTo(200);

        List<?> resultadoNombre =
                JsonPath.read(cuerpo(filtroNombre), "$");

        assertThat(resultadoNombre)
                .hasSize(1);

        assertThat(
                ((java.util.Map<?, ?>) resultadoNombre.get(0))
                        .get("numeroDocumento")
        ).isEqualTo("990000040");

        // Crear programa y dos asignaturas.
        ProgramaAcademico programa = crearPrograma("Consulta");

        Asignatura asignaturaUno = asignaturaRepository.save(
                new Asignatura(
                        PREFIJO_ASIGNATURA + "1",
                        "ITP Asignatura Uno",
                        programa
                )
        );

        Asignatura asignaturaDos = asignaturaRepository.save(
                new Asignatura(
                        PREFIJO_ASIGNATURA + "2",
                        "ITP Asignatura Dos",
                        programa
                )
        );

        Profesor profesorUno =
                profesorRepository.findById(profesorUnoId).orElseThrow();

        // Asignación ACTIVA.
        AsignacionDocente activa = new AsignacionDocente();
        activa.setProfesor(profesorUno);
        activa.setAsignatura(asignaturaUno);
        activa.setEstado(EstadoRegistro.ACTIVO);
        activa = asignacionRepository.save(activa);

        // Asignación INACTIVA.
        AsignacionDocente inactiva = new AsignacionDocente();
        inactiva.setProfesor(profesorUno);
        inactiva.setAsignatura(asignaturaDos);
        inactiva.setEstado(EstadoRegistro.INACTIVO);
        inactiva = asignacionRepository.save(inactiva);

        // Consulta todas las asignaturas del profesor.
        MvcResult todasLasAsignaturas = enviar(
                get(URL + "/" + profesorUnoId + "/asignaturas"),
                null
        );

        assertThat(todasLasAsignaturas.getResponse().getStatus())
                .isEqualTo(200);

        List<?> asignaturas =
                JsonPath.read(cuerpo(todasLasAsignaturas), "$");

        assertThat(asignaturas)
                .hasSize(2);

        assertThat(cuerpo(todasLasAsignaturas))
                .contains(PREFIJO_ASIGNATURA + "1")
                .contains(PREFIJO_ASIGNATURA + "2");

        // Solo asignaturas ACTIVAS.
        MvcResult soloActivas = enviar(
                get(URL + "/" + profesorUnoId + "/asignaturas?estado=ACTIVO"),
                null
        );

        assertThat(soloActivas.getResponse().getStatus())
                .isEqualTo(200);

        List<?> asignaturasActivas =
                JsonPath.read(cuerpo(soloActivas), "$");

        assertThat(asignaturasActivas)
                .hasSize(1);

        assertThat(cuerpo(soloActivas))
                .contains(PREFIJO_ASIGNATURA + "1")
                .doesNotContain(PREFIJO_ASIGNATURA + "2");

        // Solo asignaturas INACTIVAS.
        MvcResult soloInactivas = enviar(
                get(URL + "/" + profesorUnoId + "/asignaturas?estado=INACTIVO"),
                null
        );

        assertThat(soloInactivas.getResponse().getStatus())
                .isEqualTo(200);

        List<?> asignaturasInactivas =
                JsonPath.read(cuerpo(soloInactivas), "$");

        assertThat(asignaturasInactivas)
                .hasSize(1);

        assertThat(cuerpo(soloInactivas))
                .contains(PREFIJO_ASIGNATURA + "2")
                .doesNotContain(PREFIJO_ASIGNATURA + "1");

        // Profesor sin asignaturas.
        MvcResult profesorSinAsignaturas = enviar(
                get(URL + "/" + profesorDosId + "/asignaturas"),
                null
        );

        assertThat(profesorSinAsignaturas.getResponse().getStatus())
                .isEqualTo(200);

        List<?> sinAsignaturas =
                JsonPath.read(cuerpo(profesorSinAsignaturas), "$");

        assertThat(sinAsignaturas).isEmpty();

        // Profesor inexistente.
        UUID idInexistente = UUID.randomUUID();

        MvcResult inexistente = enviar(
                get(URL + "/" + idInexistente + "/asignaturas"),
                null
        );

        assertThat(inexistente.getResponse().getStatus())
                .isEqualTo(404);

        assertSinDetallesTecnicos(cuerpo(inexistente));
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    private ProgramaAcademico crearPrograma(String sufijo) {

        ProgramaAcademico programa = new ProgramaAcademico();

        programa.setNombre(PREFIJO_PROGRAMA + sufijo);
        programa.setCodigo("ITP-" + UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase());

        programa.setEstado(EstadoRegistro.ACTIVO);

        return programaRepository.save(programa);
    }

    private MvcResult enviar(
            MockHttpServletRequestBuilder peticion,
            String json
    ) throws Exception {

        if (json != null) {
            peticion
                    .contentType(MediaType.APPLICATION_JSON)
                    .characterEncoding(StandardCharsets.UTF_8)
                    .content(json);
        }

        return mockMvc
                .perform(peticion)
                .andReturn();
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return new String(
                resultado.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8
        );
    }

    private static void assertSinDetallesTecnicos(String cuerpo) {
        assertThat(cuerpo)
                .doesNotContain(
                        "Exception",
                        "org.",
                        "java.",
                        "SQL",
                        "trace",
                        "constraint"
                );
    }

    private static String json(
            UUID tipoDocumentoId,
            String numeroDocumento,
            String nombreCompleto,
            String correo
    ) {
        return "{"
                + "\"tipoDocumentoId\":\"" + tipoDocumentoId + "\","
                + "\"numeroDocumento\":" + texto(numeroDocumento) + ","
                + "\"nombreCompleto\":" + texto(nombreCompleto) + ","
                + "\"correoInstitucional\":" + texto(correo)
                + "}";
    }

    private static String texto(String valor) {
        if (valor == null) {
            return "null";
        }

        return "\"" + valor
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}