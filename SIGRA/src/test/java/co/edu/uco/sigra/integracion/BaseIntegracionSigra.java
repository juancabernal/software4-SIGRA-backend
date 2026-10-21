package co.edu.uco.sigra.integracion;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * Base de las pruebas de integración de RA y evaluaciones: contexto completo + PostgreSQL real (SIGRA/.env),
 * sin {@code @Transactional} (los servicios confirman de verdad). Mismo patrón que
 * {@code NivelLogroIntegracionIT}: todos los datos propios llevan el prefijo de la subclase y se borran en
 * {@code @AfterEach} (y en {@code @BeforeEach}, por si una corrida anterior se interrumpió).
 * <p>
 * Cada subclase lleva su propia etiqueta {@code @Tag("integration")}. Se ejecutan con
 * {@code .\gradlew.bat integrationTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegracionSigra {

    private static final String DOMINIO_CORREO = "@uco.net.co";

    @Autowired protected MockMvc mockMvc;
    @Autowired protected JwtService jwtService;
    @Autowired protected PlatformTransactionManager transactionManager;
    @Autowired protected AsignaturaRepository asignaturaRepository;
    @Autowired protected ResultadoAprendizajeRepository raRepository;
    @Autowired protected ProgramaAcademicoRepository programaRepository;
    @Autowired protected TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected SemestreRepository semestreRepository;
    @Autowired protected AsignacionDocenteRepository asignacionRepository;
    @Autowired protected MatriculaRepository matriculaRepository;
    @Autowired protected ConclusionRepository conclusionRepository;
    @Autowired protected NivelLogroEstudianteRepository nivelLogroRepository;
    @Autowired protected CalificacionRepository calificacionRepository;
    @Autowired protected EvaluacionRepository evaluacionRepository;
    @Autowired protected JdbcTemplate jdbc;

    protected TransactionTemplate tx;
    protected TipoDocumento tipoDocumento;
    protected ProgramaAcademico programa;
    protected String tokenAdmin;
    private final Map<String, Matricula> matriculas = new HashMap<>();
    private int consecutivoEvaluacion;

    /** Prefijo corto y único de la subclase (por ejemplo {@code ITR}): códigos, nombres y correos de prueba. */
    protected abstract String prefijo();

    private String prefijoCodigo() {
        return prefijo() + "-";
    }

    @BeforeEach
    void prepararBase() {
        tx = new TransactionTemplate(transactionManager);
        matriculas.clear();
        limpiarBase();
        TipoDocumento tipo = new TipoDocumento();
        tipo.setNombre(prefijo() + " Tipo");
        tipoDocumento = tipoDocumentoRepository.save(tipo);
        programa = crearPrograma();
        tokenAdmin = token(crearUsuario(new Administrador(), "admin"));
    }

    @AfterEach
    void limpiarBase() {
        String correoPrefijo = prefijo().toLowerCase() + "-";
        tx.executeWithoutResult(s -> {
            Predicate<Asignatura> propia = a -> a.getCodigo().startsWith(prefijoCodigo());
            Predicate<Usuario> usuarioPropio = u -> u.getCorreoInstitucional().startsWith(correoPrefijo)
                    && u.getCorreoInstitucional().endsWith(DOMINIO_CORREO);

            nivelLogroRepository.deleteAll(nivelLogroRepository.findAll().stream()
                    .filter(r -> propia.test(r.getResultadoAprendizaje().getAsignatura())).toList());
            nivelLogroRepository.flush();
            String evaluacionesPropias = "select e.id from sigra.evaluacion e join sigra.resultado_aprendizaje r "
                    + "on r.id = e.resultado_aprendizaje_id join sigra.asignatura a on a.id = r.asignatura_id "
                    + "where a.codigo like '" + prefijoCodigo() + "%'";
            jdbc.update("delete from sigra.historial_calificacion where calificacion_id in "
                    + "(select id from sigra.calificacion where evaluacion_id in (" + evaluacionesPropias + "))");
            jdbc.update("delete from sigra.calificacion where evaluacion_id in (" + evaluacionesPropias + ")");
            jdbc.update("delete from sigra.evaluacion where id in (" + evaluacionesPropias + ")");
            conclusionRepository.deleteAll(conclusionRepository.findAll().stream()
                    .filter(c -> propia.test(c.getResultadoAprendizaje().getAsignatura())).toList());
            conclusionRepository.flush();
            matriculaRepository.deleteAll(matriculaRepository.findAll().stream()
                    .filter(m -> propia.test(m.getAsignatura())).toList());
            matriculaRepository.flush();
            asignacionRepository.deleteAll(asignacionRepository.findAll().stream()
                    .filter(a -> propia.test(a.getAsignatura()) || usuarioPropio.test(a.getProfesor())).toList());
            asignacionRepository.flush();
            raRepository.deleteAll(raRepository.findAll().stream()
                    .filter(r -> propia.test(r.getAsignatura())).toList());
            raRepository.flush();
            asignaturaRepository.deleteAll(asignaturaRepository.findAll().stream().filter(propia).toList());
            asignaturaRepository.flush();
            programaRepository.deleteAll(programaRepository.findAll().stream()
                    .filter(p -> p.getNombre().startsWith(prefijo() + " Programa ")).toList());
            programaRepository.flush();
            usuarioRepository.deleteAll(usuarioRepository.findAll().stream().filter(usuarioPropio).toList());
            usuarioRepository.flush();
            semestreRepository.deleteAll(semestreRepository.findAll().stream()
                    .filter(se -> se.getCodigo().startsWith(prefijo() + "-S")).toList());
            semestreRepository.flush();
            tipoDocumentoRepository.deleteAll(tipoDocumentoRepository.findAll().stream()
                    .filter(t -> (prefijo() + " Tipo").equals(t.getNombre())).toList());
        });
    }

    // ---------------------------------------------------------------- utilidades de datos

    protected String token(Usuario usuario) {
        return jwtService.generarToken(usuario);
    }

    protected ProgramaAcademico crearPrograma() {
        ProgramaAcademico p = new ProgramaAcademico();
        p.setNombre(prefijo() + " Programa " + UUID.randomUUID().toString().substring(0, 8));
        p.setCodigo(prefijo() + ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
        return programaRepository.save(p);
    }

    protected <U extends Usuario> U crearUsuario(U usuario, String alias) {
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNombreCompleto(prefijo() + " Usuario " + alias);
        usuario.setCorreoInstitucional(prefijo().toLowerCase() + "-" + alias + "-"
                + UUID.randomUUID().toString().substring(0, 8) + DOMINIO_CORREO);
        return usuarioRepository.save(usuario);
    }

    private static String documento() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    }

    protected Estudiante crearEstudiante(String alias) {
        Estudiante e = new Estudiante();
        e.setNumeroDocumento(documento());
        return crearUsuario(e, alias);
    }

    protected Profesor crearProfesor(String alias) {
        Profesor p = new Profesor();
        p.setNumeroDocumento(documento());
        return crearUsuario(p, alias);
    }

    protected void asignar(Profesor profesor, Asignatura asignatura) {
        AsignacionDocente ad = new AsignacionDocente();
        ad.setProfesor(profesor);
        ad.setAsignatura(asignatura);
        ad.setEstado(EstadoRegistro.ACTIVO);
        asignacionRepository.save(ad);
    }

    /** Semestre en curso (hoy ... hoy + 90): empieza hoy para ser el más reciente entre los que contienen hoy. */
    protected Semestre crearSemestreActivo() {
        return crearSemestre("A", LocalDate.now(), LocalDate.now().plusDays(90));
    }

    /** Semestre ya terminado: solo de consulta. */
    protected Semestre crearSemestrePasado() {
        return crearSemestre("P", LocalDate.now().minusDays(200), LocalDate.now().minusDays(100));
    }

    private Semestre crearSemestre(String tipo, LocalDate inicio, LocalDate fin) {
        Semestre s = new Semestre();
        s.setCodigo(prefijo() + "-S" + tipo + ThreadLocalRandom.current().nextInt(1000, 1_000_000));
        s.setFechaInicio(inicio);
        s.setFechaFin(fin);
        s.setEstado(EstadoSemestre.ACTIVO);
        return semestreRepository.save(s);
    }

    /** Asignatura en BORRADOR (sin reglas de mínimo de RA). */
    protected Asignatura crearAsignatura(String codigo) {
        return asignaturaRepository.save(new Asignatura(prefijoCodigo() + codigo,
                prefijo() + " Asignatura " + codigo, programa));
    }

    /** Asignatura ACTIVA: la exigen la creación y reactivación de evaluaciones. */
    protected Asignatura crearAsignaturaActiva(String codigo) {
        Asignatura a = new Asignatura(prefijoCodigo() + codigo, prefijo() + " Asignatura " + codigo, programa);
        a.activar(Asignatura.MIN_RA_ACTIVOS);
        return asignaturaRepository.save(a);
    }

    protected ResultadoAprendizaje crearRa(Asignatura asignatura, String codigo) {
        return crearRa(asignatura, codigo, EstadoRegistro.ACTIVO);
    }

    protected ResultadoAprendizaje crearRa(Asignatura asignatura, String codigo, EstadoRegistro estado) {
        ResultadoAprendizaje ra = new ResultadoAprendizaje();
        ra.setAsignatura(asignatura);
        ra.setCodigo(codigo);
        ra.setDescripcion(prefijo() + " resultado de aprendizaje " + codigo);
        ra.setEstado(estado);
        return raRepository.save(ra);
    }

    protected Evaluacion crearEvaluacion(ResultadoAprendizaje ra, Semestre semestre, double porcentaje,
                                         EstadoEvaluacion estado) {
        Evaluacion e = new Evaluacion();
        e.setResultadoAprendizaje(ra);
        e.setSemestre(semestre);
        e.setNombre(prefijo() + " Eval " + (++consecutivoEvaluacion));
        e.setFechaTentativa(LocalDate.now().plusDays(1));
        e.setPorcentaje(BigDecimal.valueOf(porcentaje));
        e.setDescripcion(prefijo() + " evaluacion de prueba");
        e.setEstado(estado);
        return evaluacionRepository.save(e);
    }

    /** Matrícula ACTIVA del estudiante (una por terna estudiante, asignatura, semestre). */
    protected Matricula matriculaDe(Estudiante estudiante, Asignatura asignatura, Semestre semestre) {
        return matriculas.computeIfAbsent(estudiante.getId() + "|" + asignatura.getId() + "|" + semestre.getId(),
                k -> crearMatricula(estudiante, asignatura, semestre, EstadoMatricula.ACTIVA));
    }

    protected Matricula crearMatricula(Estudiante estudiante, Asignatura asignatura, Semestre semestre,
                                       EstadoMatricula estado) {
        Matricula m = new Matricula();
        m.setEstudiante(estudiante);
        m.setAsignatura(asignatura);
        m.setSemestre(semestre);
        m.setFechaMatricula(LocalDate.now());
        m.setEstado(estado);
        return matriculaRepository.save(m);
    }

    protected Calificacion calificar(Evaluacion evaluacion, Matricula matricula, double nota) {
        Calificacion c = new Calificacion();
        c.setEvaluacion(evaluacion);
        c.setMatricula(matricula);
        c.setNota(BigDecimal.valueOf(nota));
        c.setObservacion(prefijo() + " observacion de prueba");
        c.setFechaRegistro(LocalDateTime.now());
        c.setFueraDeFecha(false);
        return calificacionRepository.save(c);
    }
}
