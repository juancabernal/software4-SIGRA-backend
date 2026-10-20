package co.edu.uco.sigra.estadisticas.integracion;

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
import co.edu.uco.sigra.estadisticas.entity.Conclusion;
import co.edu.uco.sigra.estadisticas.entity.EstadoConclusion;
import co.edu.uco.sigra.estadisticas.entity.NivelLogro;
import co.edu.uco.sigra.estadisticas.entity.NivelLogroEstudiante;
import co.edu.uco.sigra.estadisticas.repository.ConclusionRepository;
import co.edu.uco.sigra.estadisticas.repository.NivelLogroEstudianteRepository;
import co.edu.uco.sigra.estadisticas.service.impl.ConclusionServiceImpl;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// INTEGRATION: contexto completo + PostgreSQL real (SIGRA/.env). Ejecutar con: .\gradlew.bat integrationTest
// Sin @Transactional: el servicio confirma de verdad. Todos los datos propios llevan el prefijo ITC / itc-
// y se borran en @AfterEach (y en @BeforeEach por si una corrida anterior se interrumpió).
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class ConclusionIntegracionIT {

    private static final String PREFIJO_CODIGO = "ITC-";
    private static final String PREFIJO_PROGRAMA = "ITC Programa ";
    private static final String PREFIJO_CORREO = "itc-";
    private static final String DOMINIO_CORREO = "@uco.net.co";
    private static final String PREFIJO_SEMESTRE = "ITC-S";
    private static final String TIPO_DOCUMENTO = "ITC Tipo";

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
            Predicate<Asignatura> deItc = a -> a.getCodigo().startsWith(PREFIJO_CODIGO);
            Predicate<Usuario> usuarioItc = u -> u.getCorreoInstitucional().startsWith(PREFIJO_CORREO)
                    && u.getCorreoInstitucional().endsWith(DOMINIO_CORREO);

            nivelLogroRepository.deleteAll(nivelLogroRepository.findAll().stream()
                    .filter(r -> deItc.test(r.getResultadoAprendizaje().getAsignatura())).toList());
            nivelLogroRepository.flush();
            conclusionRepository.deleteAll(conclusionRepository.findAll().stream()
                    .filter(c -> deItc.test(c.getResultadoAprendizaje().getAsignatura())).toList());
            conclusionRepository.flush();
            asignacionRepository.deleteAll(asignacionRepository.findAll().stream()
                    .filter(a -> deItc.test(a.getAsignatura()) || usuarioItc.test(a.getProfesor())).toList());
            asignacionRepository.flush();
            raRepository.deleteAll(raRepository.findAll().stream()
                    .filter(r -> deItc.test(r.getAsignatura())).toList());
            raRepository.flush();
            asignaturaRepository.deleteAll(asignaturaRepository.findAll().stream().filter(deItc).toList());
            asignaturaRepository.flush();
            programaRepository.deleteAll(programaRepository.findAll().stream()
                    .filter(p -> p.getNombre().startsWith(PREFIJO_PROGRAMA)).toList());
            programaRepository.flush();
            usuarioRepository.deleteAll(usuarioRepository.findAll().stream().filter(usuarioItc).toList());
            usuarioRepository.flush();
            semestreRepository.deleteAll(semestreRepository.findAll().stream()
                    .filter(se -> se.getCodigo().startsWith(PREFIJO_SEMESTRE)).toList());
            semestreRepository.flush();
            tipoDocumentoRepository.deleteAll(tipoDocumentoRepository.findAll().stream()
                    .filter(t -> TIPO_DOCUMENTO.equals(t.getNombre())).toList());
        });
    }

    // ---------------------------------------------------------------- IC-01

    @Test
    @DisplayName("IC-01 RA con solo 2 calificaciones: no genera por datos insuficientes")
    void ic01DatosInsuficientes() throws Exception {
        Asignatura asignatura = crearAsignatura("ITC-101");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        crearNiveles(ra, semestre, 2, 0);

        MvcResult r = generar(ra.getId(), semestre.getId(), tokenAdmin);
        String cuerpo = cuerpo(r);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(cuerpo, "$.generada")).isFalse();
        assertThat((String) JsonPath.read(cuerpo, "$.motivo")).isEqualTo(ConclusionServiceImpl.MOTIVO_DATOS_INSUFICIENTES);

        assertThat(conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                ra.getId(), semestre.getId(), EstadoConclusion.VIGENTE)).isEmpty();
    }

    // ---------------------------------------------------------------- IC-02

    @Test
    @DisplayName("IC-02 10 estudiantes con 4 en Bajo o Medio (40%): no genera, el umbral es estricto")
    void ic02DentroDelUmbral() throws Exception {
        Asignatura asignatura = crearAsignatura("ITC-201");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        crearNiveles(ra, semestre, 4, 6);

        MvcResult r = generar(ra.getId(), semestre.getId(), tokenAdmin);
        String cuerpo = cuerpo(r);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(cuerpo, "$.generada")).isFalse();
        assertThat((String) JsonPath.read(cuerpo, "$.motivo")).isEqualTo(ConclusionServiceImpl.MOTIVO_DENTRO_DEL_UMBRAL);

        assertThat(conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                ra.getId(), semestre.getId(), EstadoConclusion.VIGENTE)).isEmpty();
    }

    // ---------------------------------------------------------------- IC-03

    @Test
    @DisplayName("IC-03 10 estudiantes con 5 en Bajo o Medio (50%): genera una conclusión VIGENTE con mensaje y porcentaje exactos")
    void ic03GeneraConclusionVigente() throws Exception {
        Asignatura asignatura = crearAsignatura("ITC-301");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        crearNiveles(ra, semestre, 5, 5);

        MvcResult r = generar(ra.getId(), semestre.getId(), tokenAdmin);
        String cuerpo = cuerpo(r);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(cuerpo, "$.generada")).isTrue();
        assertThat((String) JsonPath.read(cuerpo, "$.motivo")).isEqualTo(ConclusionServiceImpl.MOTIVO_GENERADA);
        assertThat((String) JsonPath.read(cuerpo, "$.conclusion.mensaje")).isEqualTo(Conclusion.MENSAJE_INSUFICIENTE);
        assertThat(((Number) JsonPath.read(cuerpo, "$.conclusion.porcentajeInsuficiente")).doubleValue()).isEqualTo(50.0);
        assertThat((String) JsonPath.read(cuerpo, "$.conclusion.estado")).isEqualTo("VIGENTE");

        Conclusion enBase = conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                ra.getId(), semestre.getId(), EstadoConclusion.VIGENTE).orElseThrow();
        assertThat(enBase.getMensaje()).isEqualTo(Conclusion.MENSAJE_INSUFICIENTE);
        assertThat(enBase.getPorcentajeInsuficiente()).isEqualTo(50.0);
    }

    // ---------------------------------------------------------------- IC-04

    @Test
    @DisplayName("IC-04 Repetir la generación con una VIGENTE existente para (RA, semestre): no duplica")
    void ic04NoDuplicaVigente() throws Exception {
        Asignatura asignatura = crearAsignatura("ITC-401");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestre = crearSemestre();
        crearNiveles(ra, semestre, 5, 5);

        generar(ra.getId(), semestre.getId(), tokenAdmin);

        MvcResult segunda = generar(ra.getId(), semestre.getId(), tokenAdmin);
        String cuerpo = cuerpo(segunda);
        assertThat(segunda.getResponse().getStatus()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(cuerpo, "$.generada")).isFalse();
        assertThat((String) JsonPath.read(cuerpo, "$.motivo")).isEqualTo(ConclusionServiceImpl.MOTIVO_YA_VIGENTE);

        long vigentesEnBase = tx.execute(s -> conclusionRepository.findAll().stream()
                .filter(c -> c.getResultadoAprendizaje().getId().equals(ra.getId()))
                .filter(c -> c.getSemestre().getId().equals(semestre.getId()))
                .filter(c -> c.getEstado() == EstadoConclusion.VIGENTE)
                .count());
        assertThat(vigentesEnBase).isEqualTo(1);
    }

    // ---------------------------------------------------------------- IC-05

    @Test
    @DisplayName("IC-05 Mismo RA en un semestre distinto con una ATENDIDA del semestre anterior: SÍ genera una nueva")
    void ic05GeneraEnSemestreDistintoConAtendidaPrevia() throws Exception {
        Asignatura asignatura = crearAsignatura("ITC-501");
        ResultadoAprendizaje ra = crearRa(asignatura, "RA1");
        Semestre semestreAnterior = crearSemestre();
        Semestre semestreActual = crearSemestre();
        crearNiveles(ra, semestreAnterior, 5, 5);

        generar(ra.getId(), semestreAnterior.getId(), tokenAdmin);
        tx.executeWithoutResult(s -> {
            Conclusion previa = conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                    ra.getId(), semestreAnterior.getId(), EstadoConclusion.VIGENTE).orElseThrow();
            previa.marcarAtendida();
            conclusionRepository.save(previa);
        });

        crearNiveles(ra, semestreActual, 5, 5);
        MvcResult r = generar(ra.getId(), semestreActual.getId(), tokenAdmin);
        String cuerpo = cuerpo(r);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(cuerpo, "$.generada")).isTrue();

        assertThat(conclusionRepository.findByResultadoAprendizaje_IdAndSemestre_IdAndEstado(
                ra.getId(), semestreActual.getId(), EstadoConclusion.VIGENTE)).isPresent();
    }

    // ---------------------------------------------------------------- utilidades

    private MvcResult generar(UUID raId, UUID semestreId, String token) throws Exception {
        var peticion = post("/api/v1/resultados-aprendizaje/" + raId + "/semestres/" + semestreId + "/conclusiones");
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(peticion).andReturn();
    }

    private static String cuerpo(MvcResult r) {
        return new String(r.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private String token(Usuario usuario) {
        return jwtService.generarToken(usuario);
    }

    private ProgramaAcademico crearPrograma() {
        ProgramaAcademico p = new ProgramaAcademico();
        p.setNombre(PREFIJO_PROGRAMA + UUID.randomUUID().toString().substring(0, 8));
        p.setCodigo("ITC" + ThreadLocalRandom.current().nextInt(1_000_000, 10_000_000));
        return programaRepository.save(p);
    }

    private <U extends Usuario> U crearUsuario(U usuario, String alias) {
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNombreCompleto("ITC Usuario " + alias);
        usuario.setCorreoInstitucional(PREFIJO_CORREO + alias + "-" + UUID.randomUUID().toString().substring(0, 8)
                + DOMINIO_CORREO);
        return usuarioRepository.save(usuario);
    }

    private static String documento() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    }

    private static Estudiante estudianteNuevo() {
        Estudiante e = new Estudiante();
        e.setNumeroDocumento(documento());
        return e;
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
        return asignaturaRepository.save(new Asignatura(codigo, "ITC Asignatura " + codigo, programa));
    }

    private ResultadoAprendizaje crearRa(Asignatura asignatura, String codigo) {
        ResultadoAprendizaje ra = new ResultadoAprendizaje();
        ra.setAsignatura(asignatura);
        ra.setCodigo(codigo);
        ra.setDescripcion("ITC resultado de aprendizaje " + codigo);
        ra.setEstado(EstadoRegistro.ACTIVO);
        return raRepository.save(ra);
    }

    /** Crea {@code cantidadInsuficiente} estudiantes en Bajo/Medio y {@code cantidadSuficiente} en Alto/Sobresaliente. */
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
