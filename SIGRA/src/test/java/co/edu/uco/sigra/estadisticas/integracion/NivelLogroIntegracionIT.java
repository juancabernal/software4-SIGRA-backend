package co.edu.uco.sigra.estadisticas.integracion;

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
import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.estadisticas.repository.ConclusionRepository;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// INTEGRATION: contexto completo + PostgreSQL real (SIGRA/.env). Ejecutar con: .\gradlew.bat integrationTest
// Mismo patrón que ConclusionIntegracionIT: sin @Transactional (el servicio confirma de verdad), todos los
// datos propios llevan el prefijo ITE / ite- y se borran en @AfterEach (y en @BeforeEach por si una corrida
// anterior se interrumpió). Las notas son calificaciones REALES (RF-12): cada una es una Calificacion sobre una
// Evaluacion del RA (RF-10) y una Matricula de la asignatura, que es lo que lee FuenteCalificacionesImpl.
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class NivelLogroIntegracionIT {

    private static final String PREFIJO_CODIGO = "ITE-";
    private static final String PREFIJO_PROGRAMA = "ITE Programa ";
    private static final String PREFIJO_CORREO = "ite-";
    private static final String DOMINIO_CORREO = "@uco.net.co";
    private static final String PREFIJO_SEMESTRE = "ITE-S";
    private static final String TIPO_DOCUMENTO = "ITE Tipo";

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
    @Autowired private ConclusionRepository conclusionRepository;
    @Autowired private NivelLogroEstudianteRepository nivelLogroRepository;
    @Autowired private CalificacionRepository calificacionRepository;
    @Autowired private EvaluacionRepository evaluacionRepository;
    @Autowired private JdbcTemplate jdbc;

    private TransactionTemplate tx;
    private TipoDocumento tipoDocumento;
    private ProgramaAcademico programa;
    private String tokenAdmin;
    /** Matrícula de cada (estudiante, asignatura, semestre) ya creada en la prueba: una por terna (restricción única). */
    private final Map<String, Matricula> matriculas = new HashMap<>();
    private int consecutivoEvaluacion;

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(transactionManager);
        limpiar();
        TipoDocumento tipo = new TipoDocumento();
        tipo.setNombre(TIPO_DOCUMENTO);
        tipoDocumento = tipoDocumentoRepository.save(tipo);
        programa = crearPrograma();
        tokenAdmin = token(crearUsuario(new Administrador(), "admin"));
    }

    @AfterEach
    void limpiar() {
        tx.executeWithoutResult(s -> {
            Predicate<Asignatura> deIte = a -> a.getCodigo().startsWith(PREFIJO_CODIGO);
            Predicate<Usuario> usuarioIte = u -> u.getCorreoInstitucional().startsWith(PREFIJO_CORREO)
                    && u.getCorreoInstitucional().endsWith(DOMINIO_CORREO);

            nivelLogroRepository.deleteAll(nivelLogroRepository.findAll().stream()
                    .filter(r -> deIte.test(r.getResultadoAprendizaje().getAsignatura())).toList());
            nivelLogroRepository.flush();
            // Calificaciones y evaluaciones de las asignaturas ITE (mismo patrón SQL que CalificacionIntegracionIT).
            String evaluacionesIte = "select e.id from sigra.evaluacion e join sigra.resultado_aprendizaje r "
                    + "on r.id = e.resultado_aprendizaje_id join sigra.asignatura a on a.id = r.asignatura_id "
                    + "where a.codigo like '" + PREFIJO_CODIGO + "%'";
            jdbc.update("delete from sigra.historial_calificacion where calificacion_id in "
                    + "(select id from sigra.calificacion where evaluacion_id in (" + evaluacionesIte + "))");
            jdbc.update("delete from sigra.calificacion where evaluacion_id in (" + evaluacionesIte + ")");
            jdbc.update("delete from sigra.evaluacion where id in (" + evaluacionesIte + ")");
            conclusionRepository.deleteAll(conclusionRepository.findAll().stream()
                    .filter(c -> deIte.test(c.getResultadoAprendizaje().getAsignatura())).toList());
            conclusionRepository.flush();
            matriculaRepository.deleteAll(matriculaRepository.findAll().stream()
                    .filter(m -> deIte.test(m.getAsignatura())).toList());
            matriculaRepository.flush();
            asignacionRepository.deleteAll(asignacionRepository.findAll().stream()
                    .filter(a -> deIte.test(a.getAsignatura()) || usuarioIte.test(a.getProfesor())).toList());
            asignacionRepository.flush();
            raRepository.deleteAll(raRepository.findAll().stream()
                    .filter(r -> deIte.test(r.getAsignatura())).toList());
            raRepository.flush();
            asignaturaRepository.deleteAll(asignaturaRepository.findAll().stream().filter(deIte).toList());
            asignaturaRepository.flush();
            programaRepository.deleteAll(programaRepository.findAll().stream()
                    .filter(p -> p.getNombre().startsWith(PREFIJO_PROGRAMA)).toList());
            programaRepository.flush();
            usuarioRepository.deleteAll(usuarioRepository.findAll().stream().filter(usuarioIte).toList());
            usuarioRepository.flush();
            semestreRepository.deleteAll(semestreRepository.findAll().stream()
                    .filter(se -> se.getCodigo().startsWith(PREFIJO_SEMESTRE)).toList());
            semestreRepository.flush();
            tipoDocumentoRepository.deleteAll(tipoDocumentoRepository.findAll().stream()
                    .filter(t -> TIPO_DOCUMENTO.equals(t.getNombre())).toList());
        });
    }

    // ---------------------------------------------------------------- IE-01

    @Test
    @DisplayName("IE-01 Recálculo: promedio ponderado normalizado y nivel por estudiante, guardados en la base")
    void ie01RecalculoPersisteNiveles() throws Exception {
        ResultadoAprendizaje ra = crearRa(crearAsignatura("ITE-101"), "RA1");
        Semestre semestre = crearSemestre();
        Estudiante ana = crearEstudiante("ana");
        Estudiante beto = crearEstudiante("beto");
        // Ana: (2.0·20 + 4.0·30 + 5.0·50) / 100 = 4.10 → ALTO
        calificar(ra, semestre, ana, 2.0, 20);
        calificar(ra, semestre, ana, 4.0, 30);
        calificar(ra, semestre, ana, 5.0, 50);
        // Beto: (3.0·10 + 3.0·10 + 4.5·20) / 40 = 3.75 → MEDIO (porcentajes que no suman 100: se normaliza)
        calificar(ra, semestre, beto, 3.0, 10);
        calificar(ra, semestre, beto, 3.0, 10);
        calificar(ra, semestre, beto, 4.5, 20);

        MvcResult r = mockMvc.perform(post(url(ra, semestre) + "/recalculo").header("Authorization", "Bearer " + tokenAdmin))
                .andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);

        Map<UUID, NivelLogroEstudiante> enBase = nivelesEnBase(ra, semestre);
        assertThat(enBase.get(ana.getId()).getPromedioPonderado()).isEqualTo(4.10);
        assertThat(enBase.get(ana.getId()).getNivelLogro()).isEqualTo(NivelLogro.ALTO);
        assertThat(enBase.get(beto.getId()).getPromedioPonderado()).isEqualTo(3.75);
        assertThat(enBase.get(beto.getId()).getNivelLogro()).isEqualTo(NivelLogro.MEDIO);

        String cuerpo = cuerpo(r);
        assertThat((Integer) JsonPath.read(cuerpo, "$.estadisticas.evaluados")).isEqualTo(2);
        assertThat(((Number) JsonPath.read(cuerpo, "$.estadisticas.promedioNota")).doubleValue()).isEqualTo(3.93);
    }

    // ---------------------------------------------------------------- IE-02

    @Test
    @DisplayName("IE-02 GET estadísticas: umbral de 3, «sin datos suficientes», «sin evaluar» y distribución de 4 niveles")
    void ie02EstadisticasConUmbral() throws Exception {
        Asignatura asignatura = crearAsignatura("ITE-201");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        Estudiante bajo = crearEstudiante("bajo");
        Estudiante sobresaliente = crearEstudiante("sobre");
        Estudiante conDos = crearEstudiante("dos");
        Estudiante sinNotas = crearEstudiante("sin");
        List.of(bajo, sobresaliente, conDos, sinNotas).forEach(e -> matricular(e, asignatura, semestre));
        tresNotas(ra, semestre, bajo, 1.5);
        tresNotas(ra, semestre, sobresaliente, 4.8);
        calificar(ra, semestre, conDos, 5.0, 10);
        calificar(ra, semestre, conDos, 5.0, 10);
        recalcular(ra, semestre);

        MvcResult r = mockMvc.perform(get(url(ra, semestre)).header("Authorization", "Bearer " + tokenAdmin)).andReturn();
        String cuerpo = cuerpo(r);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat((Integer) JsonPath.read(cuerpo, "$.totalEstudiantes")).isEqualTo(4);
        assertThat((Integer) JsonPath.read(cuerpo, "$.evaluados")).isEqualTo(2);
        assertThat((Integer) JsonPath.read(cuerpo, "$.sinDatosSuficientes")).isEqualTo(1);
        assertThat((Integer) JsonPath.read(cuerpo, "$.sinEvaluar")).isEqualTo(1);
        assertThat((List<String>) JsonPath.read(cuerpo, "$.distribucion[*].nivel"))
                .containsExactly("BAJO", "MEDIO", "ALTO", "SOBRESALIENTE");
        assertThat((List<Integer>) JsonPath.read(cuerpo, "$.distribucion[*].cantidad")).containsExactly(1, 0, 0, 1);

        NivelLogroEstudiante filaConDos = nivelesEnBase(ra, semestre).get(conDos.getId());
        assertThat(filaConDos.getCantidadCalificaciones()).isEqualTo(2);
        assertThat(filaConDos.getNivelLogro()).isNull();
        assertThat(filaConDos.getPromedioPonderado()).isNull();
    }

    // ---------------------------------------------------------------- IE-03

    @Test
    @DisplayName("IE-03 Recálculo dispara RF-16: más del 40% en Bajo/Medio genera una conclusión VIGENTE, sin duplicarla")
    void ie03DisparaConclusionRf16() throws Exception {
        ResultadoAprendizaje ra = crearRa(crearAsignatura("ITE-301"), "RA1");
        Semestre semestre = crearSemestre();
        // 3 de 5 (60%) en Bajo o Medio.
        tresNotas(ra, semestre, crearEstudiante("b1"), 2.0);
        tresNotas(ra, semestre, crearEstudiante("m1"), 3.5);
        tresNotas(ra, semestre, crearEstudiante("m2"), 3.0);
        tresNotas(ra, semestre, crearEstudiante("a1"), 4.2);
        tresNotas(ra, semestre, crearEstudiante("s1"), 5.0);

        String primera = cuerpo(recalcular(ra, semestre));
        assertThat((Boolean) JsonPath.read(primera, "$.conclusion.generada")).isTrue();
        assertThat(((Number) JsonPath.read(primera, "$.conclusion.conclusion.porcentajeInsuficiente")).doubleValue())
                .isEqualTo(60.0);

        String segunda = cuerpo(recalcular(ra, semestre));
        assertThat((Boolean) JsonPath.read(segunda, "$.conclusion.generada")).isFalse();

        long vigentes = tx.execute(s -> conclusionRepository.findAll().stream()
                .filter(c -> c.getResultadoAprendizaje().getId().equals(ra.getId()))
                .filter(c -> c.getSemestre().getId().equals(semestre.getId()))
                .filter(c -> c.getEstado() == EstadoConclusion.VIGENTE)
                .count());
        assertThat(vigentes).isEqualTo(1);
        Conclusion enBase = conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                ra.getId(), semestre.getId(), EstadoConclusion.VIGENTE).orElseThrow();
        assertThat(enBase.getMensaje()).isEqualTo(Conclusion.generarMensajeInsuficiente("RA1"));
    }

    // ---------------------------------------------------------------- IE-04

    @Test
    @DisplayName("IE-04 Alcance por rol: PROFESOR asignado 200, PROFESOR no asignado 403, ESTUDIANTE 403, sin token 401")
    void ie04AlcancePorRol() throws Exception {
        Asignatura asignatura = crearAsignatura("ITE-401");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        Profesor asignado = crearUsuario(profesorNuevo(), "prof-a");
        Profesor ajeno = crearUsuario(profesorNuevo(), "prof-b");
        asignar(asignado, asignatura);
        Estudiante estudiante = crearEstudiante("est");

        assertThat(estado(get(url(ra, semestre)), token(asignado))).isEqualTo(200);
        assertThat(estado(get(url(ra, semestre)), token(ajeno))).isEqualTo(403);
        assertThat(estado(post(url(ra, semestre) + "/recalculo"), token(ajeno))).isEqualTo(403);
        assertThat(estado(get(url(ra, semestre)), token(estudiante))).isEqualTo(403);
        assertThat(estado(get(url(ra, semestre)), null)).isEqualTo(401);
    }

    // ---------------------------------------------------------------- IE-05

    @Test
    @DisplayName("IE-05 RA o semestre inexistentes: 404 sin detalles técnicos")
    void ie05NoEncontrados() throws Exception {
        ResultadoAprendizaje ra = crearRa(crearAsignatura("ITE-501"), "RA1");
        Semestre semestre = crearSemestre();
        String raInexistente = "/api/v1/resultados-aprendizaje/" + UUID.randomUUID() + "/semestres/"
                + semestre.getId() + "/estadisticas";
        String semestreInexistente = "/api/v1/resultados-aprendizaje/" + ra.getId() + "/semestres/"
                + UUID.randomUUID() + "/estadisticas";

        MvcResult r = mockMvc.perform(get(raInexistente).header("Authorization", "Bearer " + tokenAdmin)).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(404);
        assertThat(cuerpo(r)).doesNotContain("Exception").doesNotContain("at co.edu");
        assertThat(estado(get(semestreInexistente), tokenAdmin)).isEqualTo(404);
        assertThat(estado(post(raInexistente + "/recalculo"), tokenAdmin)).isEqualTo(404);
    }

    // ---------------------------------------------------------------- IE-06

    @Test
    @DisplayName("IE-06 Solo cuentan evaluaciones ACTIVA y matrículas ACTIVA; al inactivar o desvincular, el nivel se recalcula")
    void ie06FiltrosDeCalificacionesReales() throws Exception {
        Asignatura asignatura = crearAsignatura("ITE-601");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        Estudiante activa = crearEstudiante("activa");
        Estudiante desvinculada = crearEstudiante("desv");
        Matricula matriculaActiva = matriculaDe(activa, asignatura, semestre);
        Matricula matriculaDesvinculada = crearMatricula(desvinculada, asignatura, semestre,
                EstadoMatricula.DESVINCULADA);

        Evaluacion e1 = crearEvaluacion(ra, semestre, 25, EstadoEvaluacion.ACTIVA);
        Evaluacion e2 = crearEvaluacion(ra, semestre, 25, EstadoEvaluacion.ACTIVA);
        Evaluacion e3 = crearEvaluacion(ra, semestre, 25, EstadoEvaluacion.ACTIVA);
        Evaluacion inactiva = crearEvaluacion(ra, semestre, 25, EstadoEvaluacion.INACTIVA);
        // La activa: 4.0 en tres evaluaciones ACTIVA y 1.0 en una INACTIVA (no debe contar).
        calificar(e1, matriculaActiva, 4.0);
        calificar(e2, matriculaActiva, 4.0);
        calificar(e3, matriculaActiva, 4.0);
        calificar(inactiva, matriculaActiva, 1.0);
        // La desvinculada: tres notas en evaluaciones ACTIVA (no deben contar).
        calificar(e1, matriculaDesvinculada, 2.0);
        calificar(e2, matriculaDesvinculada, 2.0);
        calificar(e3, matriculaDesvinculada, 2.0);

        recalcular(ra, semestre);
        Map<UUID, NivelLogroEstudiante> enBase = nivelesEnBase(ra, semestre);
        assertThat(enBase.get(activa.getId()).getCantidadCalificaciones()).isEqualTo(3);
        assertThat(enBase.get(activa.getId()).getPromedioPonderado()).isEqualTo(4.0);
        assertThat(enBase.get(activa.getId()).getNivelLogro()).isEqualTo(NivelLogro.ALTO);
        assertThat(enBase).doesNotContainKey(desvinculada.getId());

        // Al inactivar una de sus evaluaciones queda con 2 calificaciones: sin datos suficientes.
        tx.executeWithoutResult(st -> {
            Evaluacion e = evaluacionRepository.findById(e3.getId()).orElseThrow();
            e.setEstado(EstadoEvaluacion.INACTIVA);
            evaluacionRepository.save(e);
        });
        recalcular(ra, semestre);
        NivelLogroEstudiante trasInactivar = nivelesEnBase(ra, semestre).get(activa.getId());
        assertThat(trasInactivar.getCantidadCalificaciones()).isEqualTo(2);
        assertThat(trasInactivar.getNivelLogro()).isNull();
        assertThat(trasInactivar.getPromedioPonderado()).isNull();

        // Al desvincularla, sus notas dejan de contar: la fila se conserva en 0, sin nivel ni promedio.
        tx.executeWithoutResult(st -> {
            Matricula m = matriculaRepository.findById(matriculaActiva.getId()).orElseThrow();
            m.setEstado(EstadoMatricula.DESVINCULADA);
            matriculaRepository.save(m);
        });
        recalcular(ra, semestre);
        NivelLogroEstudiante trasDesvincular = nivelesEnBase(ra, semestre).get(activa.getId());
        assertThat(trasDesvincular.getCantidadCalificaciones()).isZero();
        assertThat(trasDesvincular.getNivelLogro()).isNull();
    }

    // ---------------------------------------------------------------- utilidades

    private static String url(ResultadoAprendizaje ra, Semestre semestre) {
        return "/api/v1/resultados-aprendizaje/" + ra.getId() + "/semestres/" + semestre.getId() + "/estadisticas";
    }

    private MvcResult recalcular(ResultadoAprendizaje ra, Semestre semestre) throws Exception {
        MvcResult r = mockMvc.perform(post(url(ra, semestre) + "/recalculo")
                .header("Authorization", "Bearer " + tokenAdmin)).andReturn();
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return r;
    }

    private int estado(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion,
                       String token) throws Exception {
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(peticion).andReturn().getResponse().getStatus();
    }

    private Map<UUID, NivelLogroEstudiante> nivelesEnBase(ResultadoAprendizaje ra, Semestre semestre) {
        return tx.execute(s -> nivelLogroRepository.findByResultadoAprendizaje_IdAndSemestre_Id(ra.getId(), semestre.getId())
                .stream().collect(Collectors.toMap(NivelLogroEstudiante::getEstudianteId, f -> f)));
    }

    private static String cuerpo(MvcResult r) {
        return new String(r.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private String token(Usuario usuario) {
        return jwtService.generarToken(usuario);
    }

    private void tresNotas(ResultadoAprendizaje ra, Semestre semestre, Estudiante estudiante, double nota) {
        calificar(ra, semestre, estudiante, nota, 30);
        calificar(ra, semestre, estudiante, nota, 30);
        calificar(ra, semestre, estudiante, nota, 40);
    }

    /**
     * Registra una calificación real: crea una evaluación ACTIVA del RA con ese porcentaje y le pone la nota al
     * estudiante sobre su matrícula ACTIVA en la asignatura (la crea si no existe). Una evaluación por nota permite
     * dar a cada estudiante sus propios porcentajes; la regla de suma ≤ 100 % la aplica el servicio de evaluaciones,
     * que esta prueba no usa porque lo que se verifica es el cálculo del nivel de logro.
     */
    private Calificacion calificar(ResultadoAprendizaje ra, Semestre semestre, Estudiante estudiante, double nota,
                                   double porcentaje) {
        return calificar(crearEvaluacion(ra, semestre, porcentaje, EstadoEvaluacion.ACTIVA),
                matriculaDe(estudiante, ra.getAsignatura(), semestre), nota);
    }

    private Calificacion calificar(Evaluacion evaluacion, Matricula matricula, double nota) {
        Calificacion c = new Calificacion();
        c.setEvaluacion(evaluacion);
        c.setMatricula(matricula);
        c.setNota(BigDecimal.valueOf(nota));
        c.setObservacion("ITE observacion de prueba");
        c.setFechaRegistro(LocalDateTime.now());
        c.setFueraDeFecha(false);
        return calificacionRepository.save(c);
    }

    private Evaluacion crearEvaluacion(ResultadoAprendizaje ra, Semestre semestre, double porcentaje,
                                       EstadoEvaluacion estado) {
        Evaluacion e = new Evaluacion();
        e.setResultadoAprendizaje(ra);
        e.setSemestre(semestre);
        e.setNombre("ITE Eval " + (++consecutivoEvaluacion));
        e.setFechaTentativa(LocalDate.now());
        e.setPorcentaje(BigDecimal.valueOf(porcentaje));
        e.setDescripcion("ITE evaluacion de prueba");
        e.setEstado(estado);
        return evaluacionRepository.save(e);
    }

    private Matricula matriculaDe(Estudiante estudiante, Asignatura asignatura, Semestre semestre) {
        return matriculas.computeIfAbsent(estudiante.getId() + "|" + asignatura.getId() + "|" + semestre.getId(),
                k -> crearMatricula(estudiante, asignatura, semestre, EstadoMatricula.ACTIVA));
    }

    private ProgramaAcademico crearPrograma() {
        ProgramaAcademico p = new ProgramaAcademico();
        p.setNombre(PREFIJO_PROGRAMA + UUID.randomUUID().toString().substring(0, 8));
        p.setCodigo("ITE" + ThreadLocalRandom.current().nextInt(1_000_000, 10_000_000));
        return programaRepository.save(p);
    }

    private <U extends Usuario> U crearUsuario(U usuario, String alias) {
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNombreCompleto("ITE Usuario " + alias);
        usuario.setCorreoInstitucional(PREFIJO_CORREO + alias + "-" + UUID.randomUUID().toString().substring(0, 8)
                + DOMINIO_CORREO);
        return usuarioRepository.save(usuario);
    }

    private static String documento() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    }

    private Estudiante crearEstudiante(String alias) {
        Estudiante e = new Estudiante();
        e.setNumeroDocumento(documento());
        return crearUsuario(e, alias);
    }

    private static Profesor profesorNuevo() {
        Profesor p = new Profesor();
        p.setNumeroDocumento(documento());
        return p;
    }

    private void asignar(Profesor profesor, Asignatura asignatura) {
        AsignacionDocente ad = new AsignacionDocente();
        ad.setProfesor(profesor);
        ad.setAsignatura(asignatura);
        ad.setEstado(EstadoRegistro.ACTIVO);
        asignacionRepository.save(ad);
    }

    private void matricular(Estudiante estudiante, Asignatura asignatura, Semestre semestre) {
        matriculaDe(estudiante, asignatura, semestre);
    }

    private Matricula crearMatricula(Estudiante estudiante, Asignatura asignatura, Semestre semestre,
                                     EstadoMatricula estado) {
        Matricula m = new Matricula();
        m.setEstudiante(estudiante);
        m.setAsignatura(asignatura);
        m.setSemestre(semestre);
        m.setFechaMatricula(LocalDate.now());
        m.setEstado(estado);
        return matriculaRepository.save(m);
    }

    private Semestre crearSemestre() {
        Semestre s = new Semestre();
        s.setCodigo(PREFIJO_SEMESTRE + ThreadLocalRandom.current().nextInt(1000, 1_000_000));
        s.setFechaInicio(LocalDate.now().minusDays(10));
        s.setFechaFin(LocalDate.now().plusDays(90));
        s.setEstado(EstadoSemestre.ACTIVO);
        return semestreRepository.save(s);
    }

    private Asignatura crearAsignatura(String codigo) {
        return asignaturaRepository.save(new Asignatura(codigo, "ITE Asignatura " + codigo, programa));
    }

    private ResultadoAprendizaje crearRa(Asignatura asignatura, String codigo) {
        ResultadoAprendizaje ra = new ResultadoAprendizaje();
        ra.setAsignatura(asignatura);
        ra.setCodigo(codigo);
        ra.setDescripcion("ITE resultado de aprendizaje " + codigo);
        ra.setEstado(EstadoRegistro.ACTIVO);
        return raRepository.save(ra);
    }
}
