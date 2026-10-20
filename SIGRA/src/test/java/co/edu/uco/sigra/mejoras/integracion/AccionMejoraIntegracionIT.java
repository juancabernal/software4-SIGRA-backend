package co.edu.uco.sigra.mejoras.integracion;

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
import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.estadisticas.repository.ConclusionRepository;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.evaluaciones.config.EvaluacionConfig;
import co.edu.uco.sigra.evaluaciones.entity.EstadoEvaluacion;
import co.edu.uco.sigra.evaluaciones.entity.Evaluacion;
import co.edu.uco.sigra.evaluaciones.repository.EvaluacionRepository;
import co.edu.uco.sigra.mejoras.entity.AccionMejora;
import co.edu.uco.sigra.mejoras.repository.AccionMejoraRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// INTEGRATION: contexto completo + PostgreSQL real (SIGRA/.env). Ejecutar con: .\gradlew.bat integrationTest
// Sin @Transactional: el servicio confirma de verdad. Todos los datos propios llevan el prefijo ITM / itm-
// y se borran en @AfterEach (y en @BeforeEach por si una corrida anterior se interrumpió).
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class AccionMejoraIntegracionIT {
    /** Hoy en la zona del servicio de evaluaciones (America/Bogota): el JVM de CI corre en UTC y el día puede diferir. */
    private static final java.util.function.Supplier<LocalDate> HOY_COLOMBIA =
            () -> LocalDate.now(EvaluacionConfig.ZONA_COLOMBIA);


    private static final String PREFIJO_CODIGO = "ITM-";
    private static final String PREFIJO_PROGRAMA = "ITM Programa ";
    private static final String PREFIJO_CORREO = "itm-";
    private static final String DOMINIO_CORREO = "@uco.net.co";
    private static final String PREFIJO_SEMESTRE = "ITM-S";
    private static final String TIPO_DOCUMENTO = "ITM Tipo";

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
    @Autowired private ConclusionRepository conclusionRepository;
    @Autowired private NivelLogroEstudianteRepository nivelLogroRepository;
    @Autowired private AccionMejoraRepository accionMejoraRepository;
    @Autowired private EvaluacionRepository evaluacionRepository;

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
        programa = crearPrograma();
        tokenAdmin = token(crearUsuario(new Administrador(), "admin"));
    }

    @AfterEach
    void limpiar() {
        tx.executeWithoutResult(s -> {
            Predicate<Asignatura> deItm = a -> a.getCodigo().startsWith(PREFIJO_CODIGO);
            Predicate<Usuario> usuarioItm = u -> u.getCorreoInstitucional().startsWith(PREFIJO_CORREO)
                    && u.getCorreoInstitucional().endsWith(DOMINIO_CORREO);

            evaluacionRepository.deleteAll(evaluacionRepository.findAll().stream()
                    .filter(e -> deItm.test(e.getResultadoAprendizaje().getAsignatura())).toList());
            evaluacionRepository.flush();
            accionMejoraRepository.deleteAll(accionMejoraRepository.findAll().stream()
                    .filter(a -> deItm.test(a.getResultadoAprendizaje().getAsignatura())).toList());
            accionMejoraRepository.flush();
            nivelLogroRepository.deleteAll(nivelLogroRepository.findAll().stream()
                    .filter(r -> deItm.test(r.getResultadoAprendizaje().getAsignatura())).toList());
            nivelLogroRepository.flush();
            conclusionRepository.deleteAll(conclusionRepository.findAll().stream()
                    .filter(c -> deItm.test(c.getResultadoAprendizaje().getAsignatura())).toList());
            conclusionRepository.flush();
            asignacionRepository.deleteAll(asignacionRepository.findAll().stream()
                    .filter(a -> deItm.test(a.getAsignatura()) || usuarioItm.test(a.getProfesor())).toList());
            asignacionRepository.flush();
            raRepository.deleteAll(raRepository.findAll().stream()
                    .filter(r -> deItm.test(r.getAsignatura())).toList());
            raRepository.flush();
            asignaturaRepository.deleteAll(asignaturaRepository.findAll().stream().filter(deItm).toList());
            asignaturaRepository.flush();
            programaRepository.deleteAll(programaRepository.findAll().stream()
                    .filter(p -> p.getNombre().startsWith(PREFIJO_PROGRAMA)).toList());
            programaRepository.flush();
            usuarioRepository.deleteAll(usuarioRepository.findAll().stream().filter(usuarioItm).toList());
            usuarioRepository.flush();
            semestreRepository.deleteAll(semestreRepository.findAll().stream()
                    .filter(se -> se.getCodigo().startsWith(PREFIJO_SEMESTRE)).toList());
            semestreRepository.flush();
            tipoDocumentoRepository.deleteAll(tipoDocumentoRepository.findAll().stream()
                    .filter(t -> TIPO_DOCUMENTO.equals(t.getNombre())).toList());
        });
    }

    // ---------------------------------------------------------------- IM-01

    @Test
    @DisplayName("IM-01 Registrar sobre una conclusión que no está VIGENTE: se rechaza")
    void im01ConclusionNoVigenteSeRechaza() throws Exception {
        Asignatura asignatura = crearAsignatura("ITM-101");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        Conclusion conclusionAtendida = tx.execute(s -> {
            Conclusion c = new Conclusion(ra, semestre, 50.0);
            c.marcarAtendida();
            return conclusionRepository.save(c);
        });

        MvcResult r = registrar(conclusionAtendida.getId(), "ITM Accion", "ITM Descripcion", tokenAdmin);
        assertThat(r.getResponse().getStatus()).isEqualTo(400);

        List<AccionMejora> acciones = tx.execute(s -> accionMejoraRepository.findAll().stream()
                .filter(a -> conclusionAtendida.getId().equals(a.getConclusionOrigen().getId())).toList());
        assertThat(acciones).isEmpty();
    }

    // ---------------------------------------------------------------- IM-02

    @Test
    @DisplayName("IM-02 Registrar con conclusión VIGENTE: crea la acción y la conclusión queda ATENDIDA")
    void im02RegistraYMarcaAtendida() throws Exception {
        Asignatura asignatura = crearAsignatura("ITM-201");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        Conclusion conclusionVigente = tx.execute(s -> conclusionRepository.save(new Conclusion(ra, semestre, 50.0)));

        Profesor profesorSinAsignacion = crearUsuario(profesorNuevo(), "profSin");
        MvcResult sinPermiso = registrar(conclusionVigente.getId(), "ITM Accion", "ITM Descripcion",
                token(profesorSinAsignacion));
        assertThat(sinPermiso.getResponse().getStatus()).isEqualTo(403);

        MvcResult r = registrar(conclusionVigente.getId(), "ITM Accion", "ITM Descripcion", tokenAdmin);
        String cuerpo = cuerpo(r);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo, "$.nombre")).isEqualTo("ITM Accion");
        UUID accionId = UUID.fromString(JsonPath.read(cuerpo, "$.id"));

        AccionMejora accionEnBase = accionMejoraRepository.findById(accionId).orElseThrow();
        assertThat(accionEnBase.getNombre()).isEqualTo("ITM Accion");
        assertThat(accionEnBase.estaConvertida()).isFalse();

        Conclusion conclusionEnBase = conclusionRepository.findById(conclusionVigente.getId()).orElseThrow();
        assertThat(conclusionEnBase.getEstado()).isEqualTo(EstadoConclusion.ATENDIDA);
    }

    // ---------------------------------------------------------------- IM-03

    @Test
    @DisplayName("IM-03 Convertir con un porcentaje que supera el 100% de las evaluaciones activas: se rechaza")
    void im03PorcentajeExcedeLimiteSeRechaza() throws Exception {
        Asignatura asignatura = crearAsignatura("ITM-301");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        AccionMejora accion = crearAccionSobreConclusionVigente(ra, semestre);
        crearEvaluacionActiva(ra, semestre, "ITM previa", "60");
        String tokenProfesor = token(profesorAsignadoA(asignatura));

        MvcResult r = convertir(accion.getId(), 50.0, tokenProfesor);
        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(cuerpo(r), "$.mensaje")).contains("Porcentaje disponible: 40%");

        AccionMejora accionEnBase = accionMejoraRepository.findById(accion.getId()).orElseThrow();
        assertThat(accionEnBase.estaConvertida()).isFalse();
        assertThat(sumaActivas(ra, semestre)).isEqualByComparingTo("60");
    }

    // ---------------------------------------------------------------- IM-04

    @Test
    @DisplayName("IM-04 Convertir una acción que ya fue convertida: se rechaza")
    void im04AccionYaConvertidaSeRechaza() throws Exception {
        Asignatura asignatura = crearAsignatura("ITM-401");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        AccionMejora accion = crearAccionSobreConclusionVigente(ra, semestre);
        String tokenProfesor = token(profesorAsignadoA(asignatura));

        MvcResult primera = convertir(accion.getId(), 30.0, tokenProfesor);
        assertThat(primera.getResponse().getStatus()).isEqualTo(200);
        UUID evaluacionGeneradaId = UUID.fromString(JsonPath.read(cuerpo(primera), "$.evaluacionGeneradaId"));

        MvcResult segunda = convertir(accion.getId(), 10.0, tokenProfesor);
        assertThat(segunda.getResponse().getStatus()).isEqualTo(409);

        AccionMejora accionEnBase = accionMejoraRepository.findById(accion.getId()).orElseThrow();
        assertThat(accionEnBase.getEvaluacionGeneradaId()).isEqualTo(evaluacionGeneradaId);
        assertThat(sumaActivas(ra, semestre)).isEqualByComparingTo("30");
    }

    // ---------------------------------------------------------------- IM-05

    @Test
    @DisplayName("IM-05 Flujo completo: generar conclusión, registrar acción y convertir en evaluación real")
    void im05FlujoCompleto() throws Exception {
        Asignatura asignatura = crearAsignatura("ITM-501");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        crearNiveles(ra, semestre, 5, 5);

        MvcResult generada = mockMvc.perform(post("/api/v1/resultados-aprendizaje/" + ra.getId() + "/semestres/"
                        + semestre.getId() + "/conclusiones")
                .header("Authorization", "Bearer " + tokenAdmin)).andReturn();
        assertThat(generada.getResponse().getStatus()).isEqualTo(200);
        UUID conclusionId = UUID.fromString(JsonPath.read(cuerpo(generada), "$.conclusion.id"));

        MvcResult registrada = registrar(conclusionId, "ITM Accion Flujo", "ITM Descripcion Flujo", tokenAdmin);
        assertThat(registrada.getResponse().getStatus()).isEqualTo(200);
        UUID accionId = UUID.fromString(JsonPath.read(cuerpo(registrada), "$.id"));

        Conclusion conclusionEnBase = conclusionRepository.findById(conclusionId).orElseThrow();
        assertThat(conclusionEnBase.getEstado()).isEqualTo(EstadoConclusion.ATENDIDA);

        MvcResult convertida = convertir(accionId, 40.0, token(profesorAsignadoA(asignatura)));
        assertThat(convertida.getResponse().getStatus()).isEqualTo(200);
        String cuerpoConvertida = cuerpo(convertida);
        assertThat((Boolean) JsonPath.read(cuerpoConvertida, "$.convertida")).isTrue();
        UUID evaluacionGeneradaId = UUID.fromString(JsonPath.read(cuerpoConvertida, "$.evaluacionGeneradaId"));

        AccionMejora accionEnBase = accionMejoraRepository.findById(accionId).orElseThrow();
        assertThat(accionEnBase.estaConvertida()).isTrue();
        assertThat(accionEnBase.getEvaluacionGeneradaId()).isEqualTo(evaluacionGeneradaId);

        // La evaluación queda en sigra.evaluacion con lo heredado de la acción y lo indicado por el profesor.
        Object[] evaluacionEnBase = tx.execute(s -> {
            Evaluacion e = evaluacionRepository.findById(evaluacionGeneradaId).orElseThrow();
            return new Object[] {e.getNombre(), e.getDescripcion(), e.getPorcentaje(), e.getFechaTentativa(),
                    e.getEstado(), e.getResultadoAprendizaje().getId(), e.getSemestre().getId()};
        });
        assertThat(evaluacionEnBase[0]).isEqualTo("ITM Accion Flujo");
        assertThat(evaluacionEnBase[1]).isEqualTo("ITM Descripcion Flujo");
        assertThat((BigDecimal) evaluacionEnBase[2]).isEqualByComparingTo("40");
        assertThat(evaluacionEnBase[3]).isEqualTo(HOY_COLOMBIA.get());
        assertThat(evaluacionEnBase[4]).isEqualTo(EstadoEvaluacion.ACTIVA);
        assertThat(evaluacionEnBase[5]).isEqualTo(ra.getId());
        assertThat(evaluacionEnBase[6]).isEqualTo(semestre.getId());
    }

    // ---------------------------------------------------------------- IM-06

    @Test
    @DisplayName("IM-06 El ADMINISTRADOR no puede convertir (RF-18 solo Profesor en la matriz RBAC)")
    void im06AdministradorNoConvierte() throws Exception {
        Asignatura asignatura = crearAsignatura("ITM-601");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        AccionMejora accion = crearAccionSobreConclusionVigente(ra, semestre);

        MvcResult r = convertir(accion.getId(), 30.0, tokenAdmin);
        assertThat(r.getResponse().getStatus()).isEqualTo(403);

        AccionMejora accionEnBase = accionMejoraRepository.findById(accion.getId()).orElseThrow();
        assertThat(accionEnBase.estaConvertida()).isFalse();
        assertThat(sumaActivas(ra, semestre)).isEqualByComparingTo("0");
    }

    // ---------------------------------------------------------------- utilidades

    private AccionMejora crearAccionSobreConclusionVigente(ResultadoAprendizaje ra, Semestre semestre) {
        return tx.execute(s -> {
            Conclusion conclusion = conclusionRepository.save(new Conclusion(ra, semestre, 50.0));
            conclusion.marcarAtendida();
            conclusionRepository.save(conclusion);
            return accionMejoraRepository.save(new AccionMejora("ITM Accion", "ITM Descripcion", ra, conclusion));
        });
    }

    private MvcResult registrar(UUID conclusionId, String nombre, String descripcion, String token) throws Exception {
        String json = "{\"nombre\":\"" + nombre + "\",\"descripcion\":\"" + descripcion + "\"}";
        return enviar(post("/api/v1/conclusiones/" + conclusionId + "/acciones-mejora"), token, json);
    }

    private MvcResult convertir(UUID accionId, double porcentaje, String token) throws Exception {
        String json = "{\"fechaTentativa\":\"" + HOY_COLOMBIA.get() + "\",\"porcentaje\":" + porcentaje + "}";
        return enviar(post("/api/v1/acciones-mejora/" + accionId + "/conversion"), token, json);
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

    private static String cuerpo(MvcResult r) {
        return new String(r.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private String token(Usuario usuario) {
        return jwtService.generarToken(usuario);
    }

    /** Profesor con asignación docente ACTIVA en la asignatura (requisito del servicio de evaluaciones). */
    private Profesor profesorAsignadoA(Asignatura asignatura) {
        Profesor profesor = crearUsuario(profesorNuevo(), "prof");
        AsignacionDocente asignacion = new AsignacionDocente();
        asignacion.setProfesor(profesor);
        asignacion.setAsignatura(asignatura);
        asignacion.setEstado(EstadoRegistro.ACTIVO);
        asignacionRepository.save(asignacion);
        return profesor;
    }

    private void crearEvaluacionActiva(ResultadoAprendizaje ra, Semestre semestre, String nombre, String porcentaje) {
        Evaluacion e = new Evaluacion();
        e.setResultadoAprendizaje(ra);
        e.setSemestre(semestre);
        e.setNombre(nombre);
        e.setFechaTentativa(HOY_COLOMBIA.get());
        e.setPorcentaje(new BigDecimal(porcentaje));
        e.setDescripcion(nombre);
        e.setEstado(EstadoEvaluacion.ACTIVA);
        evaluacionRepository.save(e);
    }

    private BigDecimal sumaActivas(ResultadoAprendizaje ra, Semestre semestre) {
        return evaluacionRepository.sumarPorcentajes(ra.getId(), semestre.getId(), EstadoEvaluacion.ACTIVA);
    }

    private ProgramaAcademico crearPrograma() {
        ProgramaAcademico p = new ProgramaAcademico();
        p.setNombre(PREFIJO_PROGRAMA + UUID.randomUUID().toString().substring(0, 8));
        p.setCodigo("ITM" + ThreadLocalRandom.current().nextInt(1_000_000, 10_000_000));
        return programaRepository.save(p);
    }

    private <U extends Usuario> U crearUsuario(U usuario, String alias) {
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNombreCompleto("ITM Usuario " + alias);
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
        s.setCodigo(PREFIJO_SEMESTRE + ThreadLocalRandom.current().nextInt(1000, 1_000_000));
        // Empieza hoy para que el servicio de evaluaciones lo tome como el semestre activo (el que contiene
        // hoy con la fecha de inicio más reciente), aunque la base local tenga otro semestre en curso.
        s.setFechaInicio(HOY_COLOMBIA.get());
        s.setFechaFin(HOY_COLOMBIA.get().plusDays(90));
        s.setEstado(EstadoSemestre.ACTIVO);
        return semestreRepository.save(s);
    }

    /** ACTIVA directamente (sin los 5 RA que exige activar()): crear evaluaciones exige asignatura ACTIVA. */
    private Asignatura crearAsignatura(String codigo) {
        Asignatura asignatura = new Asignatura(codigo, "ITM Asignatura " + codigo, programa);
        ReflectionTestUtils.setField(asignatura, "estado", EstadoAsignatura.ACTIVA);
        return asignaturaRepository.save(asignatura);
    }

    private ResultadoAprendizaje crearRa(Asignatura asignatura, String codigo) {
        ResultadoAprendizaje ra = new ResultadoAprendizaje();
        ra.setAsignatura(asignatura);
        ra.setCodigo(codigo);
        ra.setDescripcion("ITM resultado de aprendizaje " + codigo);
        ra.setEstado(EstadoRegistro.ACTIVO);
        return raRepository.save(ra);
    }

    private void crearNiveles(ResultadoAprendizaje ra, Semestre semestre, int cantidadInsuficiente,
                               int cantidadSuficiente) {
        List<NivelLogro> insuficientes = List.of(NivelLogro.BAJO, NivelLogro.MEDIO);
        List<NivelLogro> suficientes = List.of(NivelLogro.ALTO, NivelLogro.SOBRESALIENTE);
        Stream.iterate(0, i -> i + 1).limit(cantidadInsuficiente)
                .forEach(i -> crearNivel(ra, semestre, "ins" + i, insuficientes.get(i % 2)));
        Stream.iterate(0, i -> i + 1).limit(cantidadSuficiente)
                .forEach(i -> crearNivel(ra, semestre, "suf" + i, suficientes.get(i % 2)));
    }

    private void crearNivel(ResultadoAprendizaje ra, Semestre semestre, String alias, NivelLogro nivel) {
        Estudiante estudiante = crearUsuario(estudianteNuevo(), alias + "-" + UUID.randomUUID().toString().substring(0, 6));
        NivelLogroEstudiante registro = new NivelLogroEstudiante(ra, semestre, estudiante);
        registro.actualizar(3, 3.0, nivel, LocalDateTime.now());
        nivelLogroRepository.save(registro);
    }
}
