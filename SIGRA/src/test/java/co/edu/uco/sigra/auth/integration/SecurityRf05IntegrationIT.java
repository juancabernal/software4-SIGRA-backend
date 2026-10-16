package co.edu.uco.sigra.auth.integration;

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
import co.edu.uco.sigra.estudiantes.entity.Matricula;
import co.edu.uco.sigra.estudiantes.entity.EstadoMatricula;
import co.edu.uco.sigra.estudiantes.repository.EstudianteRepository;
import co.edu.uco.sigra.estudiantes.repository.MatriculaRepository;
import co.edu.uco.sigra.profesores.entity.Profesor;
import co.edu.uco.sigra.profesores.repository.ProfesorRepository;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integración RF-05 con la cadena HTTP real y PostgreSQL. Verifica BOLA/IDOR con dos profesores,
 * dos estudiantes y dos asignaturas, además del alcance de matrícula por asignación docente.
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRf05IntegrationIT {

    private static final String PREFIX = "r5-";
    private static final String DOMAIN = "@uco.net.co";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignacionDocenteRepository asignacionRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private ProgramaAcademicoRepository programaRepository;
    @Autowired private SemestreRepository semestreRepository;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepository;

    private TipoDocumento tipoDocumento;
    private ProgramaAcademico programa;
    private Semestre semestre;
    private Asignatura asignaturaA;
    private Asignatura asignaturaB;
    private Profesor profesorA;
    private Profesor profesorB;
    private Estudiante estudianteA;
    private Estudiante estudianteB;
    private Administrador administrador;

    @BeforeEach
    void preparar() {
        limpiar();

        tipoDocumento = new TipoDocumento();
        tipoDocumento.setNombre(PREFIX + "tipo-" + UUID.randomUUID());
        tipoDocumento = tipoDocumentoRepository.save(tipoDocumento);

        programa = new ProgramaAcademico();
        programa.setNombre(PREFIX + "programa-" + UUID.randomUUID());
        programa.setCodigo("R5" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        programa = programaRepository.save(programa);

        asignaturaA = asignatura("A");
        asignaturaB = asignatura("B");
        asignaturaA = asignaturaRepository.save(asignaturaA);
        asignaturaB = asignaturaRepository.save(asignaturaB);

        semestre = new Semestre();
        semestre.setCodigo(PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        semestre.setFechaInicio(LocalDate.of(2099, 1, 1));
        semestre.setFechaFin(LocalDate.of(2099, 6, 30));
        semestre = semestreRepository.save(semestre);

        profesorA = usuario(new Profesor(), "profesor-a");
        profesorA.setNumeroDocumento("990000001");
        profesorA = (Profesor) usuarioRepository.save(profesorA);
        profesorB = usuario(new Profesor(), "profesor-b");
        profesorB.setNumeroDocumento("990000002");
        profesorB = (Profesor) usuarioRepository.save(profesorB);

        estudianteA = usuario(new Estudiante(), "estudiante-a");
        estudianteA.setNumeroDocumento("990000011");
        estudianteA = (Estudiante) usuarioRepository.save(estudianteA);
        estudianteB = usuario(new Estudiante(), "estudiante-b");
        estudianteB.setNumeroDocumento("990000012");
        estudianteB = (Estudiante) usuarioRepository.save(estudianteB);

        administrador = usuario(new Administrador(), "admin");
        administrador = (Administrador) usuarioRepository.save(administrador);

        asignar(profesorA, asignaturaA);
        asignar(profesorB, asignaturaB);
        matricular(estudianteA, asignaturaA);
        matricular(estudianteA, asignaturaB);
        matricular(estudianteB, asignaturaB);
    }

    @AfterEach
    void limpiarDespues() {
        limpiar();
    }

    @Test
    @DisplayName("La cadena real impide anónimo, IDOR entre profesores/estudiantes y matrícula fuera del alcance")
    void cadenaRealAplicaRolesYAlcancePorRecurso() throws Exception {
        String tokenAdmin = token(administrador);
        String tokenProfesorA = token(profesorA);
        String tokenProfesorB = token(profesorB);
        String tokenEstudianteA = token(estudianteA);

        assertThat(matriculaRepository.countActiveByStudentAndProfessor(
                estudianteA.getId(), profesorA.getId(), EstadoMatricula.ACTIVA,
                EstadoRegistro.ACTIVO, EstadoRegistro.ACTIVO))
                .as("la fixture debe dejar estudianteA visible para profesorA")
                .isEqualTo(1);

        assertThat(enviar(get("/api/v1/estudiantes/" + estudianteA.getId()), null)
                .getResponse().getStatus()).isEqualTo(401);
        assertThat(enviar(get("/api/v1/estudiantes/" + estudianteA.getId()), "token-manipulado")
                .getResponse().getStatus()).isEqualTo(401);

        MvcResult estudianteAParaProfesorA = enviar(
                get("/api/v1/estudiantes/" + estudianteA.getId()), tokenProfesorA);
        assertThat(estudianteAParaProfesorA.getResponse().getStatus())
                .as(estudianteAParaProfesorA.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(enviar(get("/api/v1/estudiantes/" + estudianteB.getId()), tokenProfesorA)
                .getResponse().getStatus()).isEqualTo(403);
        assertThat(enviar(get("/api/v1/estudiantes/" + estudianteB.getId()), tokenProfesorB)
                .getResponse().getStatus()).isEqualTo(200);

        MvcResult listaProfesorA = enviar(get("/api/v1/estudiantes"), tokenProfesorA);
        assertThat(listaProfesorA.getResponse().getStatus()).isEqualTo(200);
        String listaA = listaProfesorA.getResponse().getContentAsString();
        assertThat(listaA).contains(estudianteA.getId().toString());
        assertThat(listaA).doesNotContain(estudianteB.getId().toString());

        MvcResult asignaturasProfesorA = enviar(
                get("/api/v1/estudiantes/" + estudianteA.getId() + "/asignaturas"), tokenProfesorA);
        assertThat(asignaturasProfesorA.getResponse().getStatus()).isEqualTo(200);
        String asignaturasA = asignaturasProfesorA.getResponse().getContentAsString();
        assertThat(asignaturasA).contains("r5-Asignatura A");
        assertThat(asignaturasA).doesNotContain("r5-Asignatura B");

        assertThat(enviar(get("/api/v1/estudiantes/" + estudianteB.getId() + "/asignaturas"), tokenProfesorA)
                .getResponse().getStatus()).isEqualTo(403);
        assertThat(enviar(get("/api/v1/estudiantes/" + estudianteA.getId()), tokenEstudianteA)
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(enviar(get("/api/v1/estudiantes/" + estudianteB.getId()), tokenEstudianteA)
                .getResponse().getStatus()).isEqualTo(403);

        MvcResult listaAdmin = enviar(get("/api/v1/estudiantes"), tokenAdmin);
        assertThat(listaAdmin.getResponse().getStatus()).isEqualTo(200);
        assertThat(listaAdmin.getResponse().getContentAsString())
                .contains(estudianteA.getId().toString(), estudianteB.getId().toString());

        administrador.setEstado(EstadoRegistro.INACTIVO);
        usuarioRepository.saveAndFlush(administrador);
        assertThat(enviar(get("/api/v1/estudiantes"), tokenAdmin)
                .getResponse().getStatus()).isEqualTo(401);
        administrador.setEstado(EstadoRegistro.ACTIVO);
        usuarioRepository.saveAndFlush(administrador);

        assertThat(enviar(get("/api/v1/matriculas")
                        .param("asignaturaId", asignaturaA.getId().toString())
                        .param("semestreId", semestre.getId().toString()), tokenProfesorA)
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(enviar(get("/api/v1/matriculas")
                        .param("asignaturaId", asignaturaB.getId().toString())
                        .param("semestreId", semestre.getId().toString()), tokenProfesorA)
                .getResponse().getStatus()).isEqualTo(403);
        String matriculaAjena = "{\"estudianteId\":\"" + estudianteB.getId()
                + "\",\"asignaturaId\":\"" + asignaturaB.getId()
                + "\",\"semestreId\":\"" + semestre.getId() + "\"}";
        MockHttpServletRequestBuilder matricularAjeno = post("/api/v1/matriculas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(matriculaAjena);
        assertThat(enviar(matricularAjeno, tokenProfesorA)
                .getResponse().getStatus()).isEqualTo(403);
    }

    private MvcResult enviar(MockHttpServletRequestBuilder request, String token) throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request).andReturn();
    }

    private String token(Usuario usuario) {
        return jwtService.generarToken(usuario);
    }

    private Asignatura asignatura(String sufijo) {
        Asignatura asignatura = new Asignatura(PREFIX + "a" + sufijo
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                PREFIX + "Asignatura " + sufijo, programa);
        asignatura.activar(Asignatura.MIN_RA_ACTIVOS);
        return asignatura;
    }

    private <T extends Usuario> T usuario(T usuario, String sufijo) {
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNombreCompleto(PREFIX + sufijo);
        usuario.setCorreoInstitucional(PREFIX + sufijo + UUID.randomUUID() + DOMAIN);
        usuario.setPasswordHash("hash-no-usado-en-esta-prueba");
        usuario.setEstado(EstadoRegistro.ACTIVO);
        return usuario;
    }

    private void asignar(Profesor profesor, Asignatura asignatura) {
        AsignacionDocente asignacion = new AsignacionDocente();
        asignacion.setProfesor(profesor);
        asignacion.setAsignatura(asignatura);
        asignacion.setEstado(EstadoRegistro.ACTIVO);
        asignacionRepository.save(asignacion);
    }

    private void matricular(Estudiante estudiante, Asignatura asignatura) {
        Matricula matricula = new Matricula();
        matricula.setEstudiante(estudiante);
        matricula.setAsignatura(asignatura);
        matricula.setSemestre(semestre);
        matricula.setFechaMatricula(LocalDate.now());
        matricula.setEstado(EstadoMatricula.ACTIVA);
        matriculaRepository.save(matricula);
    }

    private void limpiar() {
        List<ProgramaAcademico> programasPropios = programaRepository.findAll().stream()
                .filter(programa -> programa.getNombre().startsWith(PREFIX)).toList();
        Set<UUID> programaIds = programasPropios.stream().map(ProgramaAcademico::getId).collect(java.util.stream.Collectors.toSet());
        List<Asignatura> asignaturasPropias = asignaturaRepository.findAll().stream()
                .filter(asignatura -> asignatura.getCodigo().startsWith(PREFIX)
                        || (asignatura.getPrograma() != null && programaIds.contains(asignatura.getPrograma().getId())))
                .toList();
        Set<UUID> asignaturaIds = asignaturasPropias.stream().map(Asignatura::getId).collect(java.util.stream.Collectors.toSet());
        List<Usuario> usuarios = usuarioRepository.findAll().stream()
                .filter(usuario -> usuario.getCorreoInstitucional() != null
                        && usuario.getCorreoInstitucional().startsWith(PREFIX))
                .toList();
        List<UUID> ids = usuarios.stream().map(Usuario::getId).toList();
        matriculaRepository.deleteAll(matriculaRepository.findAll().stream()
                .filter(matricula -> ids.contains(matricula.getEstudiante().getId())
                        || (matricula.getAsignatura() != null && asignaturaIds.contains(matricula.getAsignatura().getId())))
                .toList());
        matriculaRepository.flush();
        asignacionRepository.deleteAll(asignacionRepository.findAll().stream()
                .filter(asignacion -> ids.contains(asignacion.getProfesor().getId())
                        || (asignacion.getAsignatura() != null && asignaturaIds.contains(asignacion.getAsignatura().getId())))
                .toList());
        asignacionRepository.flush();
        asignaturaRepository.deleteAll(asignaturasPropias);
        asignaturaRepository.flush();
        estudianteRepository.deleteAll(estudianteRepository.findAll().stream()
                .filter(estudiante -> ids.contains(estudiante.getId())).toList());
        profesorRepository.deleteAll(profesorRepository.findAll().stream()
                .filter(profesor -> ids.contains(profesor.getId())).toList());
        profesorRepository.flush();
        usuarioRepository.deleteAll(usuarioRepository.findAll().stream()
                .filter(usuario -> ids.contains(usuario.getId())
                        && !(usuario instanceof Estudiante)
                        && !(usuario instanceof Profesor)).toList());
        usuarioRepository.flush();
        semestreRepository.deleteAll(semestreRepository.findAll().stream()
                .filter(semestre -> semestre.getCodigo().startsWith(PREFIX)).toList());
        semestreRepository.flush();
        programaRepository.deleteAll(programasPropios);
        tipoDocumentoRepository.deleteAll(tipoDocumentoRepository.findAll().stream()
                .filter(tipo -> tipo.getNombre().startsWith(PREFIX)).toList());
    }

}
