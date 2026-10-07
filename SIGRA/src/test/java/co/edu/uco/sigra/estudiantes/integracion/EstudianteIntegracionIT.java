package co.edu.uco.sigra.estudiantes.integracion;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.repository.EstudianteRepository;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

// INTEGRATION: contexto completo + PostgreSQL real (SIGRA/.env). Ejecutar con: .\gradlew.bat integrationTest
//
// Sin @Transactional: el servicio confirma de verdad y cada caso verifica la respuesta HTTP y la fila real,
// igual que SemestreIntegracionIT y AsignaturaIntegracionIT.
//
// Seguridad: los @PreAuthorize de EstudianteController y MatriculaController siguen comentados y
// SecurityConfig usa permitAll(), así que esta clase no cubre roles (a diferencia de AsignaturaIntegracionIT).
// Si se activa RF-05 para este módulo, hay que añadir un caso de 401/403 aquí.
//
// Aislamiento de datos: numeroDocumento/correo de estudiante usan el prefijo reservado 990000xxx / "ite-"
// (el patrón \d{6,10} de EstudianteRequestDTO no admite letras, por eso el documento es numérico). Las
// asignaturas y semestres de apoyo usan sus propios prefijos ITE-ASIG- / ITE-SEM-, distintos de los que ya
// reservan AsignaturaIntegracionIT (ITG-) y SemestreIntegracionIT (años 219X). @BeforeEach y @AfterEach
// borran SOLO filas con esos prefijos.
//
// La limpieza de matrículas recorre los estudiantes propios (vía MatriculaRepository.buscarPorEstudiante,
// que hace join fetch) en vez de filtrar por el código de asignatura/semestre en Matricula: en esta clase
// toda matrícula que se crea referencia siempre a uno de sus propios estudiantes, así que ese recorrido
// basta y evita inicializar relaciones LAZY fuera de una transacción.
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class EstudianteIntegracionIT {

    private static final String URL_ESTUDIANTES = "/api/v1/estudiantes";
    private static final String URL_MATRICULAS = "/api/v1/matriculas";
    private static final String PREFIJO_DOCUMENTO = "990000";
    private static final String PREFIJO_CORREO = "ite-";
    private static final String DOMINIO_CORREO = "@uco.net.co";
    private static final String PREFIJO_ASIGNATURA = "ITE-ASIG-";
    private static final String PREFIJO_SEMESTRE = "ITE-SEM-";
    private static final String PREFIJO_PROGRAMA = "ITE Programa";
    private static final String NOMBRE_TIPO_DOCUMENTO = "ITE Tipo";

    @Autowired private MockMvc mockMvc;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private SemestreRepository semestreRepository;
    @Autowired private ProgramaAcademicoRepository programaRepository;

    private TipoDocumento tipoDocumento;
    private ProgramaAcademico programa;
    private Asignatura asignaturaActiva;
    private Semestre semestre1;
    private Semestre semestre2;

    @BeforeEach
    void preparar() {
        limpiar();

        TipoDocumento tipo = new TipoDocumento();
        tipo.setNombre(NOMBRE_TIPO_DOCUMENTO);
        tipoDocumento = tipoDocumentoRepository.save(tipo);

        ProgramaAcademico p = new ProgramaAcademico();
        p.setNombre(PREFIJO_PROGRAMA + " " + UUID.randomUUID().toString().substring(0, 8));
        p.setCodigo("ITEP" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        programa = programaRepository.save(p);

        Asignatura asignatura = new Asignatura(PREFIJO_ASIGNATURA + "1", "ITE Asignatura Activa", programa);
        asignatura.activar(Asignatura.MIN_RA_ACTIVOS);
        asignaturaActiva = asignaturaRepository.save(asignatura);

        semestre1 = guardarSemestre(PREFIJO_SEMESTRE + "1", EstadoSemestre.ACTIVO);
        semestre2 = guardarSemestre(PREFIJO_SEMESTRE + "2", EstadoSemestre.ACTIVO);
    }

    @AfterEach
    void limpiar() {
        List<Estudiante> estudiantesPropios = estudianteRepository.findAll().stream()
                .filter(this::esPropio)
                .toList();
        for (Estudiante estudiante : estudiantesPropios) {
            matriculaRepository.deleteAll(matriculaRepository.buscarPorEstudiante(estudiante.getId()));
        }
        matriculaRepository.flush();

        estudianteRepository.deleteAll(estudiantesPropios);
        estudianteRepository.flush();

        asignaturaRepository.deleteAll(asignaturaRepository.findAll().stream()
                .filter(a -> a.getCodigo().startsWith(PREFIJO_ASIGNATURA))
                .toList());
        asignaturaRepository.flush();

        programaRepository.deleteAll(programaRepository.findAll().stream()
                .filter(p -> p.getNombre().startsWith(PREFIJO_PROGRAMA))
                .toList());
        programaRepository.flush();

        semestreRepository.deleteAll(semestreRepository.findAll().stream()
                .filter(s -> s.getCodigo().startsWith(PREFIJO_SEMESTRE))
                .toList());
        semestreRepository.flush();

        tipoDocumentoRepository.deleteAll(tipoDocumentoRepository.findAll().stream()
                .filter(t -> NOMBRE_TIPO_DOCUMENTO.equals(t.getNombre()))
                .toList());
    }

    private boolean esPropio(Estudiante estudiante) {
        return estudiante.getNumeroDocumento().startsWith(PREFIJO_DOCUMENTO)
                || estudiante.getCorreoInstitucional().startsWith(PREFIJO_CORREO);
    }

    // ---------------------------------------------------------------- IT-01

    @Test
    @DisplayName("IT-01 Registrar: documento duplicado (incluso de uno inactivo) y correo duplicado dan 409 sin fila nueva")
    void it01RegistrarYRechazarDuplicados() throws Exception {
        String numero1 = PREFIJO_DOCUMENTO + "001";
        String correo1 = correo("001");

        MvcResult creado = enviar(post(URL_ESTUDIANTES),
                jsonEstudiante(tipoDocumento.getId(), numero1, "ITE Estudiante Uno", correo1));
        String cuerpoCreado = cuerpo(creado);
        assertThat(creado.getResponse().getStatus()).isEqualTo(201);
        UUID id1 = UUID.fromString(JsonPath.read(cuerpoCreado, "$.id"));
        assertThat((String) JsonPath.read(cuerpoCreado, "$.numeroDocumento")).isEqualTo(numero1);
        assertThat((String) JsonPath.read(cuerpoCreado, "$.estado")).isEqualTo("ACTIVO");

        Estudiante enBase = estudianteRepository.findById(id1).orElseThrow();
        assertThat(enBase.getNumeroDocumento()).isEqualTo(numero1);
        assertThat(enBase.getCorreoInstitucional()).isEqualTo(correo1);
        assertThat(enBase.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
        // La unicidad del correo se valida contra UsuarioRepository (todos los subtipos), no solo Estudiante.
        assertThat(usuarioRepository.existsByCorreoInstitucionalIgnoreCase(correo1)).isTrue();

        // Documento repetido: 409, mensaje de EstudianteYaRegistradoException, sin segunda fila.
        MvcResult duplicado = enviar(post(URL_ESTUDIANTES),
                jsonEstudiante(tipoDocumento.getId(), numero1, "Otro Nombre", correo("901")));
        assertThat(duplicado.getResponse().getStatus()).isEqualTo(409);
        assertThat((String) JsonPath.read(cuerpo(duplicado), "$.mensaje"))
                .isEqualTo("El estudiante con el documento " + numero1 + " ya está registrado en el sistema.");
        assertSinDetallesTecnicos(cuerpo(duplicado));
        assertThat(contarPorDocumento(numero1)).isEqualTo(1);

        // Inactivar y repetir el documento: sigue 409 (el inactivo conserva su estado, no libera el documento).
        MvcResult inactivar = enviar(delete(URL_ESTUDIANTES + "/" + id1), null);
        assertThat(inactivar.getResponse().getStatus()).isEqualTo(204);
        assertThat(estudianteRepository.findById(id1).orElseThrow().getEstado()).isEqualTo(EstadoRegistro.INACTIVO);

        MvcResult duplicadoInactivo = enviar(post(URL_ESTUDIANTES),
                jsonEstudiante(tipoDocumento.getId(), numero1, "Otro Nombre", correo("902")));
        assertThat(duplicadoInactivo.getResponse().getStatus()).isEqualTo(409);
        assertThat((String) JsonPath.read(cuerpo(duplicadoInactivo), "$.mensaje"))
                .isEqualTo("El estudiante con el documento " + numero1 + " ya está registrado en el sistema.");
        assertSinDetallesTecnicos(cuerpo(duplicadoInactivo));
        assertThat(contarPorDocumento(numero1)).isEqualTo(1);

        // Segundo estudiante válido y un tercero con su correo: 409 por correo duplicado, sin fila nueva.
        String numero2 = PREFIJO_DOCUMENTO + "002";
        String correo2 = correo("002");
        MvcResult segundo = enviar(post(URL_ESTUDIANTES),
                jsonEstudiante(tipoDocumento.getId(), numero2, "ITE Estudiante Dos", correo2));
        assertThat(segundo.getResponse().getStatus()).isEqualTo(201);

        long antesTercero = estudianteRepository.count();
        MvcResult tercero = enviar(post(URL_ESTUDIANTES),
                jsonEstudiante(tipoDocumento.getId(), PREFIJO_DOCUMENTO + "003", "ITE Estudiante Tres", correo2));
        assertThat(tercero.getResponse().getStatus()).isEqualTo(409);
        assertThat((String) JsonPath.read(cuerpo(tercero), "$.mensaje"))
                .isEqualTo("Ya existe un estudiante registrado con el correo " + correo2 + ".");
        assertSinDetallesTecnicos(cuerpo(tercero));
        assertThat(estudianteRepository.count()).isEqualTo(antesTercero);
    }

    // ---------------------------------------------------------------- IT-02

    static Stream<Arguments> entradasInvalidas() {
        String numeroOk = "990000099";
        String nombreOk = "ITE Nombre Valido";
        String correoOk = "ite-099@uco.net.co";
        return Stream.of(
                Arguments.of("documento con letras", true, "99A000001", nombreOk, correoOk),
                Arguments.of("documento de 5 dígitos", true, "99000", nombreOk, correoOk),
                Arguments.of("documento de 11 dígitos", true, "99000000001", nombreOk, correoOk),
                Arguments.of("correo de otro dominio", true, numeroOk, nombreOk, "ite-099@gmail.com"),
                Arguments.of("tipoDocumentoId ausente", false, numeroOk, nombreOk, correoOk),
                Arguments.of("numeroDocumento ausente", true, null, nombreOk, correoOk),
                Arguments.of("nombreCompleto ausente", true, numeroOk, null, correoOk),
                Arguments.of("correoInstitucional ausente", true, numeroOk, nombreOk, null));
    }

    @ParameterizedTest(name = "IT-02 {0} -> 400 sin guardar nada")
    @MethodSource("entradasInvalidas")
    @DisplayName("IT-02 Entradas inválidas: 400, sin detalles técnicos y no se guarda nada")
    void it02EntradasInvalidas(String caso, boolean tipoDocumentoValido, String numeroDocumento,
                                String nombreCompleto, String correoInstitucional) throws Exception {
        long antes = estudianteRepository.count();
        UUID tipoDocumentoId = tipoDocumentoValido ? tipoDocumento.getId() : null;

        MvcResult r = enviar(post(URL_ESTUDIANTES),
                jsonEstudiante(tipoDocumentoId, numeroDocumento, nombreCompleto, correoInstitucional));

        assertThat(r.getResponse().getStatus()).as(caso).isEqualTo(400);
        assertSinDetallesTecnicos(cuerpo(r));
        assertThat(estudianteRepository.count()).as(caso).isEqualTo(antes);
    }

    // ---------------------------------------------------------------- IT-03

    @Test
    @DisplayName("IT-03 Modificar: nombre/correo se actualizan, el documento no se puede cambiar, "
            + "el correo duplicado da 409 y el id inexistente da 404")
    void it03Modificar() throws Exception {
        String numero1 = PREFIJO_DOCUMENTO + "010";
        String correoOriginal = correo("010");
        MvcResult creado = enviar(post(URL_ESTUDIANTES),
                jsonEstudiante(tipoDocumento.getId(), numero1, "ITE Nombre Original", correoOriginal));
        assertThat(creado.getResponse().getStatus()).isEqualTo(201);
        UUID id1 = UUID.fromString(JsonPath.read(cuerpo(creado), "$.id"));

        // Modificación válida: nombre y correo cambian, el documento no.
        String correoModificado = correo("011");
        MvcResult modificado = enviar(put(URL_ESTUDIANTES + "/" + id1),
                jsonEstudiante(tipoDocumento.getId(), numero1, "ITE Nombre Modificado", correoModificado));
        assertThat(modificado.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(modificado), "$.nombreCompleto")).isEqualTo("ITE Nombre Modificado");
        assertThat((String) JsonPath.read(cuerpo(modificado), "$.correoInstitucional")).isEqualTo(correoModificado);

        Estudiante trasModificar = estudianteRepository.findById(id1).orElseThrow();
        assertThat(trasModificar.getNombreCompleto()).isEqualTo("ITE Nombre Modificado");
        assertThat(trasModificar.getCorreoInstitucional()).isEqualTo(correoModificado);
        assertThat(trasModificar.getNumeroDocumento()).isEqualTo(numero1);

        // Intentar cambiar el documento: 400, mensaje de DocumentoNoModificableException, la fila no cambia.
        MvcResult cambioDocumento = enviar(put(URL_ESTUDIANTES + "/" + id1),
                jsonEstudiante(tipoDocumento.getId(), PREFIJO_DOCUMENTO + "012", "ITE Nombre Modificado", correoModificado));
        assertThat(cambioDocumento.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(cuerpo(cambioDocumento), "$.mensaje"))
                .isEqualTo("El tipo y el número de documento del estudiante no se pueden modificar después del registro.");
        assertThat(estudianteRepository.findById(id1).orElseThrow().getNumeroDocumento()).isEqualTo(numero1);

        // Correo ya usado por otro estudiante: 409, sin cambios.
        String correo2 = correo("013");
        MvcResult segundo = enviar(post(URL_ESTUDIANTES),
                jsonEstudiante(tipoDocumento.getId(), PREFIJO_DOCUMENTO + "013", "ITE Estudiante Dos", correo2));
        assertThat(segundo.getResponse().getStatus()).isEqualTo(201);

        MvcResult correoDuplicado = enviar(put(URL_ESTUDIANTES + "/" + id1),
                jsonEstudiante(tipoDocumento.getId(), numero1, "ITE Nombre Modificado", correo2));
        assertThat(correoDuplicado.getResponse().getStatus()).isEqualTo(409);
        assertThat((String) JsonPath.read(cuerpo(correoDuplicado), "$.mensaje"))
                .isEqualTo("Ya existe un estudiante registrado con el correo " + correo2 + ".");
        assertThat(estudianteRepository.findById(id1).orElseThrow().getCorreoInstitucional()).isEqualTo(correoModificado);

        // Id inexistente: 404.
        UUID inexistente = UUID.randomUUID();
        MvcResult noEncontrado = enviar(put(URL_ESTUDIANTES + "/" + inexistente),
                jsonEstudiante(tipoDocumento.getId(), PREFIJO_DOCUMENTO + "014", "ITE Nombre", correo("014")));
        assertThat(noEncontrado.getResponse().getStatus()).isEqualTo(404);
        assertThat((String) JsonPath.read(cuerpo(noEncontrado), "$.mensaje"))
                .isEqualTo("No existe un estudiante con id " + inexistente + ".");
    }

    // ---------------------------------------------------------------- IT-04

    @Test
    @DisplayName("IT-04 Inactivar: sigue apareciendo con estado INACTIVO y sus datos intactos; "
            + "la matrícula no se toca; id inexistente da 404")
    void it04Inactivar() throws Exception {
        String numero = PREFIJO_DOCUMENTO + "020";
        String correoEstudiante = correo("020");
        MvcResult creado = enviar(post(URL_ESTUDIANTES),
                jsonEstudiante(tipoDocumento.getId(), numero, "ITE Estudiante Historial", correoEstudiante));
        assertThat(creado.getResponse().getStatus()).isEqualTo(201);
        UUID id = UUID.fromString(JsonPath.read(cuerpo(creado), "$.id"));

        MvcResult matriculado = enviar(post(URL_MATRICULAS),
                jsonMatricula(id, asignaturaActiva.getId(), semestre1.getId()));
        assertThat(matriculado.getResponse().getStatus()).isEqualTo(201);
        UUID matriculaId = UUID.fromString(JsonPath.read(cuerpo(matriculado), "$.id"));

        MvcResult inactivado = enviar(delete(URL_ESTUDIANTES + "/" + id), null);
        assertThat(inactivado.getResponse().getStatus()).isEqualTo(204);

        MvcResult porId = enviar(get(URL_ESTUDIANTES + "/" + id), null);
        assertThat(porId.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(porId), "$.estado")).isEqualTo("INACTIVO");
        assertThat((String) JsonPath.read(cuerpo(porId), "$.numeroDocumento")).isEqualTo(numero);
        assertThat((String) JsonPath.read(cuerpo(porId), "$.correoInstitucional")).isEqualTo(correoEstudiante);

        MvcResult porFiltro = enviar(get(URL_ESTUDIANTES + "?filtro=" + numero), null);
        assertThat(porFiltro.getResponse().getStatus()).isEqualTo(200);
        List<String> documentos = JsonPath.read(cuerpo(porFiltro), "$[*].numeroDocumento");
        List<String> estados = JsonPath.read(cuerpo(porFiltro), "$[*].estado");
        assertThat(documentos).containsExactly(numero);
        assertThat(estados).containsExactly("INACTIVO");

        assertThat(matriculaRepository.findById(matriculaId).orElseThrow().getEstado()).isEqualTo(EstadoRegistro.ACTIVO);

        MvcResult noEncontrado = enviar(delete(URL_ESTUDIANTES + "/" + UUID.randomUUID()), null);
        assertThat(noEncontrado.getResponse().getStatus()).isEqualTo(404);
    }

    // ---------------------------------------------------------------- IT-05

    @Test
    @DisplayName("IT-05 Matricular y desvincular: terna única, repitencia, validaciones de estado "
            + "y reactivación sin duplicar")
    void it05MatricularYDesvincular() throws Exception {
        String numero = PREFIJO_DOCUMENTO + "030";
        MvcResult creado = enviar(post(URL_ESTUDIANTES),
                jsonEstudiante(tipoDocumento.getId(), numero, "ITE Estudiante Matricula", correo("030")));
        assertThat(creado.getResponse().getStatus()).isEqualTo(201);
        UUID estudianteId = UUID.fromString(JsonPath.read(cuerpo(creado), "$.id"));

        // Matricular: 201, fila real ACTIVO.
        MvcResult matricula = enviar(post(URL_MATRICULAS),
                jsonMatricula(estudianteId, asignaturaActiva.getId(), semestre1.getId()));
        assertThat(matricula.getResponse().getStatus()).isEqualTo(201);
        UUID matriculaId = UUID.fromString(JsonPath.read(cuerpo(matricula), "$.id"));
        assertThat(matriculaRepository.findById(matriculaId).orElseThrow().getEstado()).isEqualTo(EstadoRegistro.ACTIVO);

        // Terna duplicada (activa): 409, mensaje de MatriculaDuplicadaException, sin fila nueva.
        MvcResult duplicada = enviar(post(URL_MATRICULAS),
                jsonMatricula(estudianteId, asignaturaActiva.getId(), semestre1.getId()));
        assertThat(duplicada.getResponse().getStatus()).isEqualTo(409);
        assertThat((String) JsonPath.read(cuerpo(duplicada), "$.mensaje"))
                .isEqualTo("El estudiante " + estudianteId + " ya está matriculado en la asignatura "
                        + asignaturaActiva.getId() + " para el semestre " + semestre1.getId() + ".");
        Matricula ternaOriginal = matriculaRepository.buscarPorTerna(estudianteId, asignaturaActiva.getId(), semestre1.getId())
                .orElseThrow();
        assertThat(ternaOriginal.getId()).isEqualTo(matriculaId);
        assertThat(ternaOriginal.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);

        // Repitencia en otro semestre (terna distinta): 201.
        MvcResult repitencia = enviar(post(URL_MATRICULAS),
                jsonMatricula(estudianteId, asignaturaActiva.getId(), semestre2.getId()));
        assertThat(repitencia.getResponse().getStatus()).isEqualTo(201);
        UUID matriculaRepitenciaId = UUID.fromString(JsonPath.read(cuerpo(repitencia), "$.id"));
        assertThat(matriculaRepitenciaId).isNotEqualTo(matriculaId);

        // Estudiante inactivo: 400, sin fila.
        MvcResult estudianteParaInactivar = enviar(post(URL_ESTUDIANTES),
                jsonEstudiante(tipoDocumento.getId(), PREFIJO_DOCUMENTO + "031", "ITE Estudiante Inactivo", correo("031")));
        UUID estudianteInactivoId = UUID.fromString(JsonPath.read(cuerpo(estudianteParaInactivar), "$.id"));
        assertThat(enviar(delete(URL_ESTUDIANTES + "/" + estudianteInactivoId), null).getResponse().getStatus())
                .isEqualTo(204);

        MvcResult porEstudianteInactivo = enviar(post(URL_MATRICULAS),
                jsonMatricula(estudianteInactivoId, asignaturaActiva.getId(), semestre1.getId()));
        assertThat(porEstudianteInactivo.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(cuerpo(porEstudianteInactivo), "$.mensaje"))
                .isEqualTo("El estudiante con id " + estudianteInactivoId + " no está activo.");
        assertThat(matriculaRepository.buscarPorTerna(estudianteInactivoId, asignaturaActiva.getId(), semestre1.getId()))
                .isEmpty();

        // Asignatura no activa (BORRADOR): 400, sin fila.
        Asignatura asignaturaBorrador = asignaturaRepository.save(
                new Asignatura(PREFIJO_ASIGNATURA + "2", "ITE Asignatura Borrador", programa));
        MvcResult porAsignaturaNoActiva = enviar(post(URL_MATRICULAS),
                jsonMatricula(estudianteId, asignaturaBorrador.getId(), semestre1.getId()));
        assertThat(porAsignaturaNoActiva.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(cuerpo(porAsignaturaNoActiva), "$.mensaje"))
                .isEqualTo("La asignatura con id " + asignaturaBorrador.getId() + " no está activa.");
        assertThat(matriculaRepository.buscarPorTerna(estudianteId, asignaturaBorrador.getId(), semestre1.getId()))
                .isEmpty();

        // Semestre no activo (INACTIVO): 400, sin fila.
        Semestre semestreInactivo = guardarSemestre(PREFIJO_SEMESTRE + "3", EstadoSemestre.INACTIVO);
        MvcResult porSemestreNoActivo = enviar(post(URL_MATRICULAS),
                jsonMatricula(estudianteId, asignaturaActiva.getId(), semestreInactivo.getId()));
        assertThat(porSemestreNoActivo.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(cuerpo(porSemestreNoActivo), "$.mensaje"))
                .isEqualTo("El semestre con id " + semestreInactivo.getId() + " no está activo.");
        assertThat(matriculaRepository.buscarPorTerna(estudianteId, asignaturaActiva.getId(), semestreInactivo.getId()))
                .isEmpty();

        // Desvincular la matrícula original: 204, estado INACTIVO en la fila real.
        MvcResult desvinculada = enviar(delete(URL_MATRICULAS + "/" + matriculaId), null);
        assertThat(desvinculada.getResponse().getStatus()).isEqualTo(204);
        assertThat(matriculaRepository.findById(matriculaId).orElseThrow().getEstado()).isEqualTo(EstadoRegistro.INACTIVO);

        // Desvincularla otra vez: 409, mensaje de MatriculaYaInactivaException.
        MvcResult desvinculadaOtraVez = enviar(delete(URL_MATRICULAS + "/" + matriculaId), null);
        assertThat(desvinculadaOtraVez.getResponse().getStatus()).isEqualTo(409);
        assertThat((String) JsonPath.read(cuerpo(desvinculadaOtraVez), "$.mensaje"))
                .isEqualTo("La matrícula con id " + matriculaId + " ya no está activa.");

        // Volver a matricular la misma terna original: reactivación (200), mismo id, sin fila nueva.
        MvcResult reactivada = enviar(post(URL_MATRICULAS),
                jsonMatricula(estudianteId, asignaturaActiva.getId(), semestre1.getId()));
        assertThat(reactivada.getResponse().getStatus()).isEqualTo(200);
        UUID idReactivada = UUID.fromString(JsonPath.read(cuerpo(reactivada), "$.id"));
        assertThat(idReactivada).isEqualTo(matriculaId);

        Matricula ternaReactivada = matriculaRepository.buscarPorTerna(estudianteId, asignaturaActiva.getId(), semestre1.getId())
                .orElseThrow();
        assertThat(ternaReactivada.getId()).isEqualTo(matriculaId);
        assertThat(ternaReactivada.getEstado()).isEqualTo(EstadoRegistro.ACTIVO);
    }

    // ---------------------------------------------------------------- utilidades

    private Semestre guardarSemestre(String codigo, EstadoSemestre estado) {
        Semestre s = new Semestre();
        s.setCodigo(codigo);
        s.setFechaInicio(LocalDate.now().minusDays(10));
        s.setFechaFin(LocalDate.now().plusDays(90));
        s.setEstado(estado);
        return semestreRepository.save(s);
    }

    private long contarPorDocumento(String numeroDocumento) {
        return estudianteRepository.findAll().stream()
                .filter(e -> e.getNumeroDocumento().equals(numeroDocumento))
                .count();
    }

    private static String correo(String sufijo) {
        return PREFIJO_CORREO + sufijo + DOMINIO_CORREO;
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

    private static String jsonEstudiante(UUID tipoDocumentoId, String numeroDocumento, String nombreCompleto,
                                          String correoInstitucional) {
        return "{\"tipoDocumentoId\":" + uuidOrNull(tipoDocumentoId)
                + ",\"numeroDocumento\":" + texto(numeroDocumento)
                + ",\"nombreCompleto\":" + texto(nombreCompleto)
                + ",\"correoInstitucional\":" + texto(correoInstitucional) + "}";
    }

    private static String jsonMatricula(UUID estudianteId, UUID asignaturaId, UUID semestreId) {
        return "{\"estudianteId\":" + uuidOrNull(estudianteId)
                + ",\"asignaturaId\":" + uuidOrNull(asignaturaId)
                + ",\"semestreId\":" + uuidOrNull(semestreId) + "}";
    }

    private static String uuidOrNull(UUID id) {
        return id == null ? "null" : "\"" + id + "\"";
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }
}
