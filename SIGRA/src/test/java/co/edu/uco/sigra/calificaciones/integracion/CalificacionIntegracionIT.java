package co.edu.uco.sigra.calificaciones.integracion;

import co.edu.uco.sigra.asignaturas.entity.AsignacionDocente;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignacionDocenteRepository;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.auth.entity.Administrador;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.calificaciones.entity.Calificacion;
import co.edu.uco.sigra.calificaciones.repository.CalificacionRepository;
import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.evaluaciones.repository.EvaluacionRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

// INTEGRATION: contexto completo + PostgreSQL real (SIGRA/.env). Ejecutar con: .\gradlew.bat integrationTest
// Sin @Transactional: el servicio confirma de verdad. Seguridad real (filtro JWT + @PreAuthorize) con tokens
// emitidos por JwtService, como ProfesorIntegracionIT.
// Todos los datos propios llevan el prefijo ITN / itn- y se borran en @AfterEach (y en @BeforeEach por si
// una corrida anterior se interrumpió). El historial se borra con SQL porque su repositorio no permite borrar.
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class CalificacionIntegracionIT {

    private static final String PREFIJO_CODIGO = "ITN-";
    private static final String PREFIJO_PROGRAMA = "ITN Programa ";
    private static final String PREFIJO_CORREO = "itn-";
    private static final String DOMINIO_CORREO = "@uco.net.co";
    private static final String PREFIJO_SEMESTRE = "ITN-S";
    private static final String TIPO_DOCUMENTO = "ITN Tipo";
    private static final String OBSERVACION = "Buen desarrollo del parcial";
    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProgramaAcademicoRepository programaRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private ResultadoAprendizajeRepository raRepository;
    @Autowired private SemestreRepository semestreRepository;
    @Autowired private AsignacionDocenteRepository asignacionRepository;
    @Autowired private EvaluacionRepository evaluacionRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private CalificacionRepository calificacionRepository;

    private LocalDate hoy;
    private TipoDocumento tipoDocumento;
    private Semestre semestre;
    private Semestre semestreAnterior;
    private Asignatura asignatura;
    private ResultadoAprendizaje ra;
    private Profesor profesor;
    private String tokenProfesor;
    private Evaluacion evaluacionATiempo;
    private Evaluacion evaluacionVencida;
    private Matricula matriculaAna;
    private Matricula matriculaBeto;

    @BeforeEach
    void preparar() {
        limpiar();
        hoy = LocalDate.now(ZONA_COLOMBIA);

        TipoDocumento tipo = new TipoDocumento();
        tipo.setNombre(TIPO_DOCUMENTO);
        tipoDocumento = tipoDocumentoRepository.save(tipo);

        semestre = crearSemestre(hoy.minusDays(1), hoy.plusDays(90));
        semestreAnterior = crearSemestre(hoy.minusDays(400), hoy.minusDays(300));

        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setNombre(PREFIJO_PROGRAMA + UUID.randomUUID().toString().substring(0, 8));
        programa.setCodigo("ITN" + ThreadLocalRandom.current().nextInt(1_000_000, 10_000_000));
        programa = programaRepository.save(programa);

        asignatura = asignaturaRepository.save(new Asignatura(PREFIJO_CODIGO + "101", "ITN Asignatura", programa));
        ra = new ResultadoAprendizaje();
        ra.setAsignatura(asignatura);
        ra.setCodigo("RA1");
        ra.setDescripcion("ITN resultado de aprendizaje");
        ra.setEstado(EstadoRegistro.ACTIVO);
        ra = raRepository.save(ra);

        Profesor nuevo = new Profesor();
        nuevo.setNumeroDocumento(documento());
        profesor = crearUsuario(nuevo, "prof");
        AsignacionDocente asignacion = new AsignacionDocente();
        asignacion.setProfesor(profesor);
        asignacion.setAsignatura(asignatura);
        asignacion.setEstado(EstadoRegistro.ACTIVO);
        asignacionRepository.save(asignacion);
        tokenProfesor = jwtService.generarToken(profesor);

        evaluacionATiempo = crearEvaluacion("Parcial 1", hoy.plusDays(10), EstadoEvaluacion.ACTIVA);
        evaluacionVencida = crearEvaluacion("Quiz 1", hoy.minusDays(1), EstadoEvaluacion.ACTIVA);


        matriculaAna = crearMatricula("Ana", semestre, EstadoMatricula.ACTIVA);
        matriculaBeto = crearMatricula("Beto", semestre, EstadoMatricula.DESVINCULADA);
    }

    @AfterEach
    void limpiar() {
        String asignaturasItn = "select id from sigra.asignatura where codigo like 'ITN-%'";
        String evaluacionesItn = "select e.id from sigra.evaluacion e join sigra.resultado_aprendizaje r "
                + "on r.id = e.resultado_aprendizaje_id where r.asignatura_id in (" + asignaturasItn + ")";
        String usuariosItn = "select id from sigra.usuario where correo_institucional like 'itn-%' "
                + "or tipo_documento_id in (select id from sigra.tipo_documento where nombre = 'ITN Tipo')";

        jdbc.update("delete from sigra.historial_calificacion where calificacion_id in "
                + "(select id from sigra.calificacion where evaluacion_id in (" + evaluacionesItn + "))");
        jdbc.update("delete from sigra.calificacion where evaluacion_id in (" + evaluacionesItn + ")");
        jdbc.update("delete from sigra.matricula where asignatura_id in (" + asignaturasItn + ")");
        jdbc.update("delete from sigra.evaluacion where id in (" + evaluacionesItn + ")");
        jdbc.update("delete from sigra.asignacion_docente where asignatura_id in (" + asignaturasItn + ") "
                + "or profesor_id in (" + usuariosItn + ")");
        jdbc.update("delete from sigra.resultado_aprendizaje where asignatura_id in (" + asignaturasItn + ")");
        jdbc.update("delete from sigra.asignatura where codigo like 'ITN-%'");
        jdbc.update("delete from sigra.programa_academico where nombre like 'ITN Programa %'");
        jdbc.update("delete from sigra.profesor where id in (" + usuariosItn + ")");
        jdbc.update("delete from sigra.estudiante where id in (" + usuariosItn + ")");
        jdbc.update("delete from sigra.administrador where id in (" + usuariosItn + ")");
        jdbc.update("delete from sigra.usuario where id in (" + usuariosItn + ")");
        jdbc.update("delete from sigra.semestre where codigo like 'ITN-S%'");
        jdbc.update("delete from sigra.tipo_documento where nombre = 'ITN Tipo'");
    }

    // ---------------------------------------------------------------- IT-01

    @Test
    @DisplayName("IT-01 Registrar a tiempo y fuera de fecha; la planilla muestra a cada matriculado")
    void it01RegistrarYPlanilla() throws Exception {
        MvcResult evaluaciones = enviar(get("/api/v1/calificaciones/evaluaciones")
                .param("resultadoAprendizajeId", ra.getId().toString())
                .param("semestreId", semestre.getId().toString()), tokenProfesor, null);
        assertThat(evaluaciones.getResponse().getStatus()).isEqualTo(200);
        List<String> nombresEvaluaciones = JsonPath.read(cuerpo(evaluaciones), "$[*].nombre");
        assertThat(nombresEvaluaciones).containsExactly("Quiz 1", "Parcial 1");

        MvcResult aTiempo = enviar(post(urlPlanilla(evaluacionATiempo)), tokenProfesor,
                registro(matriculaAna, "4.5", OBSERVACION));
        assertThat(aTiempo.getResponse().getStatus()).isEqualTo(201);
        assertThat((Boolean) JsonPath.read(cuerpo(aTiempo), "$.fueraDeFecha")).isFalse();

        MvcResult tarde = enviar(post(urlPlanilla(evaluacionVencida)), tokenProfesor,
                registro(matriculaAna, "3.0", OBSERVACION));
        assertThat(tarde.getResponse().getStatus()).isEqualTo(201);
        assertThat((Boolean) JsonPath.read(cuerpo(tarde), "$.fueraDeFecha")).isTrue();

        Calificacion enBase = calificacionRepository.findById(UUID.fromString(JsonPath.read(cuerpo(aTiempo), "$.id")))
                .orElseThrow();
        assertThat(enBase.getNota()).isEqualByComparingTo("4.5");
        assertThat(enBase.getObservacion()).isEqualTo(OBSERVACION);
        assertThat(enBase.isFueraDeFecha()).isFalse();

        // Una matrícula desvinculada sin nota no aparece en la planilla.
        crearMatricula("Carla", semestre, EstadoMatricula.DESVINCULADA);

        MvcResult planilla = enviar(get(urlPlanilla(evaluacionATiempo)), tokenProfesor, null);
        assertThat(planilla.getResponse().getStatus()).isEqualTo(200);
        String json = cuerpo(planilla);
        assertThat((Integer) JsonPath.read(json, "$.totalEstudiantes")).isEqualTo(2);
        assertThat((Integer) JsonPath.read(json, "$.totalCalificados")).isEqualTo(1);
        List<String> nombres = JsonPath.read(json, "$.filas[*].nombreCompleto");
        assertThat(nombres).containsExactly("ITN Usuario Ana", "ITN Usuario Beto");
        assertThat(JsonPath.read(json, "$.filas[0].nota").toString()).isEqualTo("4.5");
        assertThat((Object) JsonPath.read(json, "$.filas[1].calificacionId")).isNull();
    }

    // ---------------------------------------------------------------- IT-02

    @Test
    @DisplayName("IT-02 Reglas: rango de nota, observación, matrícula, semestre, duplicado y evaluación inactiva")
    void it02Reglas() throws Exception {
        Matricula desvinculada = crearMatricula("Dora", semestre, EstadoMatricula.DESVINCULADA);
        Matricula deOtroSemestre = crearMatricula("Elsa", semestreAnterior, EstadoMatricula.ACTIVA);
        Evaluacion inactiva = crearEvaluacion("Taller viejo", hoy.plusDays(5), EstadoEvaluacion.INACTIVA);

        assertEstado(400, enviar(post(urlPlanilla(evaluacionATiempo)), tokenProfesor,
                registro(matriculaAna, "5.5", OBSERVACION)));
        assertEstado(400, enviar(post(urlPlanilla(evaluacionATiempo)), tokenProfesor,
                registro(matriculaAna, "4.0", "Corta")));
        assertEstado(400, enviar(post(urlPlanilla(evaluacionATiempo)), tokenProfesor,
                registro(desvinculada, "4.0", OBSERVACION)));
        assertEstado(400, enviar(post(urlPlanilla(evaluacionATiempo)), tokenProfesor,
                registro(deOtroSemestre, "4.0", OBSERVACION)));
        assertEstado(400, enviar(post(urlPlanilla(inactiva)), tokenProfesor,
                registro(matriculaAna, "4.0", OBSERVACION)));
        assertEstado(404, enviar(post("/api/v1/evaluaciones/" + UUID.randomUUID() + "/calificaciones"),
                tokenProfesor, registro(matriculaAna, "4.0", OBSERVACION)));
        assertThat(calificacionRepository.existsByEvaluacion_Id(evaluacionATiempo.getId())).isFalse();

        assertEstado(201, enviar(post(urlPlanilla(evaluacionATiempo)), tokenProfesor,
                registro(matriculaAna, "4.0", OBSERVACION)));
        assertEstado(409, enviar(post(urlPlanilla(evaluacionATiempo)), tokenProfesor,
                registro(matriculaAna, "2.0", OBSERVACION)));
        assertThat(calificacionRepository.countByMatricula_Id(matriculaAna.getId())).isEqualTo(1);
    }

    // ---------------------------------------------------------------- IT-03

    @Test
    @DisplayName("IT-03 Modificar deja el antes y el después en el historial; sin cambios no agrega eventos")
    void it03ModificarEHistorial() throws Exception {
        String id = registrarNota(matriculaAna, "3.5");

        MvcResult modificada = enviar(put("/api/v1/calificaciones/" + id), tokenProfesor,
                modificacion("4.0", "Ajuste después de la revisión"));
        assertThat(modificada.getResponse().getStatus()).isEqualTo(200);
        assertThat(calificacionRepository.findById(UUID.fromString(id)).orElseThrow().getNota())
                .isEqualByComparingTo("4.0");

        // Mismos valores: 200 y ningún evento nuevo.
        assertEstado(200, enviar(put("/api/v1/calificaciones/" + id), tokenProfesor,
                modificacion("4.0", "Ajuste después de la revisión")));

        MvcResult historial = enviar(get("/api/v1/calificaciones/" + id + "/historial"), tokenProfesor, null);
        assertThat(historial.getResponse().getStatus()).isEqualTo(200);
        String json = cuerpo(historial);
        List<String> eventos = JsonPath.read(json, "$[*].tipoEvento");
        assertThat(eventos).containsExactly("REGISTRO", "MODIFICACION");
        assertThat(JsonPath.read(json, "$[1].notaAnterior").toString()).isEqualTo("3.5");
        assertThat(JsonPath.read(json, "$[1].notaNueva").toString()).isEqualTo("4.0");
        assertThat((String) JsonPath.read(json, "$[1].observacionNueva")).isEqualTo("Ajuste después de la revisión");
        assertThat((String) JsonPath.read(json, "$[1].usuarioId")).isEqualTo(profesor.getId().toString());
        assertThat((String) JsonPath.read(json, "$[1].rolUsuario")).isEqualTo("PROFESOR");
    }

    // ---------------------------------------------------------------- IT-04

    @Test
    @DisplayName("IT-04 Seguridad: 401 sin token y 403 para estudiante, administrador y profesor ajeno")
    void it04Seguridad() throws Exception {
        String id = registrarNota(matriculaAna, "3.5");

        Estudiante estudiante = new Estudiante();
        estudiante.setNumeroDocumento(documento());
        String tokenEstudiante = jwtService.generarToken(crearUsuario(estudiante, "est"));
        String tokenAdmin = jwtService.generarToken(crearUsuario(new Administrador(), "admin"));
        Profesor otro = new Profesor();
        otro.setNumeroDocumento(documento());
        String tokenProfesorAjeno = jwtService.generarToken(crearUsuario(otro, "ajeno"));

        for (String token : new String[]{null, tokenEstudiante, tokenAdmin, tokenProfesorAjeno}) {
            int esperado = token == null ? 401 : 403;
            assertEstado(esperado, enviar(get(urlPlanilla(evaluacionATiempo)), token, null));
            assertEstado(esperado, enviar(post(urlPlanilla(evaluacionATiempo)), token,
                    registro(matriculaBeto, "4.0", OBSERVACION)));
            assertEstado(esperado, enviar(put("/api/v1/calificaciones/" + id), token,
                    modificacion("1.0", OBSERVACION)));
            assertEstado(esperado, enviar(get("/api/v1/calificaciones/" + id + "/historial"), token, null));
            assertEstado(esperado, enviar(get("/api/v1/calificaciones/evaluaciones")
                    .param("resultadoAprendizajeId", ra.getId().toString())
                    .param("semestreId", semestre.getId().toString()), token, null));
        }

        assertThat(calificacionRepository.countByMatricula_Id(matriculaBeto.getId())).isZero();
        assertThat(calificacionRepository.findById(UUID.fromString(id)).orElseThrow().getNota())
                .isEqualByComparingTo("3.5");
    }

    // ---------------------------------------------------------------- IT-05

    @Test
    @DisplayName("IT-05 Al desvincular la matrícula la nota se conserva en la planilla, pero ya no se modifica")
    void it05MatriculaDesvinculada() throws Exception {
        String id = registrarNota(matriculaAna, "3.5");

        matriculaAna.setEstado(EstadoMatricula.DESVINCULADA);
        matriculaRepository.save(matriculaAna);

        String json = cuerpo(enviar(get(urlPlanilla(evaluacionATiempo)), tokenProfesor, null));
        List<Boolean> activas = JsonPath.read(json, "$.filas[?(@.nombreCompleto == 'ITN Usuario Ana')].matriculaActiva");
        assertThat(activas).containsExactly(false);
        assertThat((Integer) JsonPath.read(json, "$.totalCalificados")).isEqualTo(1);

        assertEstado(400, enviar(put("/api/v1/calificaciones/" + id), tokenProfesor,
                modificacion("4.0", OBSERVACION)));
        assertThat(calificacionRepository.findById(UUID.fromString(id)).orElseThrow().getNota())
                .isEqualByComparingTo("3.5");
    }

    // ---------------------------------------------------------------- Utilidades

    private String registrarNota(Matricula matricula, String nota) throws Exception {
        MvcResult creada = enviar(post(urlPlanilla(evaluacionATiempo)), tokenProfesor,
                registro(matricula, nota, OBSERVACION));
        assertThat(creada.getResponse().getStatus()).isEqualTo(201);
        return JsonPath.read(cuerpo(creada), "$.id");
    }

    private static String urlPlanilla(Evaluacion evaluacion) {
        return "/api/v1/evaluaciones/" + evaluacion.getId() + "/calificaciones";
    }

    private static String registro(Matricula matricula, String nota, String observacion) {
        return """
                {"matriculaId":"%s","nota":%s,"observacion":"%s"}
                """.formatted(matricula.getId(), nota, observacion);
    }

    private static String modificacion(String nota, String observacion) {
        return """
                {"nota":%s,"observacion":"%s"}
                """.formatted(nota, observacion);
    }

    private MvcResult enviar(MockHttpServletRequestBuilder peticion, String token, String json) throws Exception {
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).characterEncoding(StandardCharsets.UTF_8).content(json);
        }
        return mockMvc.perform(peticion).andReturn();
    }

    private static String cuerpo(MvcResult resultado) {
        return new String(resultado.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private static void assertEstado(int esperado, MvcResult resultado) {
        String json = cuerpo(resultado);
        assertThat(resultado.getResponse().getStatus()).as(json).isEqualTo(esperado);
        assertThat(json).doesNotContain("Exception", "org.", "java.", "SQL", "constraint");
    }

    private <U extends Usuario> U crearUsuario(U usuario, String alias) {
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNombreCompleto("ITN Usuario " + alias);
        usuario.setCorreoInstitucional(PREFIJO_CORREO + alias.toLowerCase() + "-"
                + UUID.randomUUID().toString().substring(0, 8) + DOMINIO_CORREO);
        usuario.setEstado(EstadoRegistro.ACTIVO);
        return usuarioRepository.save(usuario);
    }

    private static String documento() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    }

    private Semestre crearSemestre(LocalDate inicio, LocalDate fin) {
        Semestre s = new Semestre();
        s.setCodigo(PREFIJO_SEMESTRE + ThreadLocalRandom.current().nextInt(1000, 1_000_000));
        s.setFechaInicio(inicio);
        s.setFechaFin(fin);
        s.setEstado(EstadoSemestre.ACTIVO);
        return semestreRepository.save(s);
    }

    private Evaluacion crearEvaluacion(String nombre, LocalDate fechaTentativa, EstadoEvaluacion estado) {
        Evaluacion e = new Evaluacion();
        e.setResultadoAprendizaje(ra);
        e.setSemestre(semestre);
        e.setNombre(nombre);
        e.setFechaTentativa(fechaTentativa);
        e.setPorcentaje(new BigDecimal("20"));
        e.setDescripcion("Primer parcial");
        e.setEstado(estado);
        return evaluacionRepository.save(e);
    }

    private Matricula crearMatricula(String nombre, Semestre semestreMatricula, EstadoMatricula estado) {
        Estudiante estudiante = new Estudiante();
        estudiante.setNumeroDocumento(documento());
        Matricula m = new Matricula();
        m.setEstudiante(crearUsuario(estudiante, nombre));
        m.setAsignatura(asignatura);
        m.setSemestre(semestreMatricula);
        m.setFechaMatricula(hoy);
        m.setEstado(estado);
        return matriculaRepository.save(m);
    }
}