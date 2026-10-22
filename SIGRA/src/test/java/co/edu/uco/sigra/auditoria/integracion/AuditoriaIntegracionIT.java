package co.edu.uco.sigra.auditoria.integracion;

import co.edu.uco.sigra.auth.entity.Administrador;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.entity.TipoDocumento;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.profesores.entity.Profesor;
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
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// INTEGRATION: contexto completo + PostgreSQL real. Seguridad real (filtro JWT + @PreAuthorize).
// Los datos de apoyo llevan el prefijo ITA / ita- y se borran al terminar. Los eventos de auditoría que
// generan las pruebas NO se borran: la tabla es de solo inserción y eso mismo es lo que se prueba (IT-03).
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class AuditoriaIntegracionIT {

    private static final String URL = "/api/v1/auditoria";
    private static final String CONTRASENA = "Auditoria2026*";
    private static final String TIPO_DOCUMENTO = "ITA Tipo";
    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TipoDocumentoRepository tipoDocumentoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ProgramaAcademicoRepository programaRepository;

    private LocalDate hoy;
    private TipoDocumento tipoDocumento;
    private Administrador administrador;
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        limpiar();
        hoy = LocalDate.now(ZONA_COLOMBIA);

        TipoDocumento tipo = new TipoDocumento();
        tipo.setNombre(TIPO_DOCUMENTO);
        tipoDocumento = tipoDocumentoRepository.save(tipo);

        Administrador nuevo = new Administrador();
        nuevo.setPasswordHash(passwordEncoder.encode(CONTRASENA));
        administrador = crearUsuario(nuevo, "admin");
        tokenAdmin = jwtService.generarToken(administrador);
    }

    @AfterEach
    void limpiar() {
        String usuariosIta = "select id from sigra.usuario where correo_institucional like 'ita-%' "
                + "or tipo_documento_id in (select id from sigra.tipo_documento where nombre = 'ITA Tipo')";
        jdbc.update("delete from sigra.programa_academico where nombre like 'ITA Programa %'");
        jdbc.update("delete from sigra.profesor where id in (" + usuariosIta + ")");
        jdbc.update("delete from sigra.estudiante where id in (" + usuariosIta + ")");
        jdbc.update("delete from sigra.administrador where id in (" + usuariosIta + ")");
        jdbc.update("delete from sigra.usuario where id in (" + usuariosIta + ")");
        jdbc.update("delete from sigra.tipo_documento where nombre = 'ITA Tipo'");
    }

    // ---------------------------------------------------------------- IT-01

    @Test
    @DisplayName("IT-01 El login exitoso y el rechazado quedan en el historial; el rechazado sin usuario")
    void it01Login() throws Exception {
        Integer fallidosAntes = jdbc.queryForObject(
                "select count(*) from sigra.historial_auditoria where accion = 'LOGIN_FALLIDO'", Integer.class);

        assertEstado(401, enviar(post("/api/v1/auth/login"), null, login("Incorrecta123*")));
        Integer fallidosDespues = jdbc.queryForObject(
                "select count(*) from sigra.historial_auditoria where accion = 'LOGIN_FALLIDO'", Integer.class);
        assertThat(fallidosDespues).isEqualTo(fallidosAntes + 1);

        assertEstado(200, enviar(post("/api/v1/auth/login"), null, login(CONTRASENA)));

        List<Map<String, Object>> eventos = consultarHoy("USUARIO");
        assertThat(eventos).anySatisfy(evento -> {
            assertThat(evento.get("accion")).isEqualTo("LOGIN_EXITOSO");
            assertThat(evento.get("idEntidad")).isEqualTo(administrador.getId().toString());
            assertThat(evento.get("usuarioId")).isEqualTo(administrador.getId().toString());
            assertThat(evento.get("rolUsuario")).isEqualTo("ADMINISTRADOR");
            assertThat(evento.get("fechaHora")).isNotNull();
        });
    }

    // ---------------------------------------------------------------- IT-02

    @Test
    @DisplayName("IT-02 Un cambio de estado queda registrado con entidad, acción, usuario, fecha y detalle")
    void it02CambioDeEstado() throws Exception {
        ProgramaAcademico programa = crearPrograma();

        assertEstado(200, enviar(patch("/api/v1/programas/" + programa.getId() + "/inactivar"), tokenAdmin, null));

        List<Map<String, Object>> eventos = consultarHoy("PROGRAMA");
        assertThat(eventos).anySatisfy(evento -> {
            assertThat(evento.get("accion")).isEqualTo("CAMBIO_ESTADO");
            assertThat(evento.get("tipoEntidad")).isEqualTo("PROGRAMA");
            assertThat(evento.get("idEntidad")).isEqualTo(programa.getId().toString());
            assertThat(evento.get("usuarioId")).isEqualTo(administrador.getId().toString());
            assertThat((String) evento.get("detalle")).contains("inactivar").contains("INACTIVO");
        });
    }

    // ---------------------------------------------------------------- IT-03

    @Test
    @DisplayName("IT-03 Solo inserción: la base rechaza UPDATE y DELETE sobre el historial, sin excepción")
    void it03SoloInsercion() throws Exception {
        ProgramaAcademico programa = crearPrograma();
        assertEstado(200, enviar(patch("/api/v1/programas/" + programa.getId() + "/inactivar"), tokenAdmin, null));
        UUID idEvento = jdbc.queryForObject(
                "select id from sigra.historial_auditoria where id_entidad = ? order by fecha_hora desc limit 1",
                UUID.class, programa.getId());
        String detalleOriginal = jdbc.queryForObject(
                "select detalle from sigra.historial_auditoria where id = ?", String.class, idEvento);

        assertThatThrownBy(() -> jdbc.update(
                "update sigra.historial_auditoria set detalle = 'alterado' where id = ?", idEvento))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("delete from sigra.historial_auditoria where id = ?", idEvento))
                .isInstanceOf(DataAccessException.class);

        assertThat(jdbc.queryForObject("select detalle from sigra.historial_auditoria where id = ?",
                String.class, idEvento)).isEqualTo(detalleOriginal);
    }

    // ---------------------------------------------------------------- IT-04

    @Test
    @DisplayName("IT-04 Reporte: paginado, del más reciente al más antiguo, con filtros de fecha y validaciones")
    void it04Reporte() throws Exception {
        ProgramaAcademico uno = crearPrograma();
        ProgramaAcademico dos = crearPrograma();
        assertEstado(200, enviar(patch("/api/v1/programas/" + uno.getId() + "/inactivar"), tokenAdmin, null));
        assertEstado(200, enviar(patch("/api/v1/programas/" + dos.getId() + "/inactivar"), tokenAdmin, null));

        String json = cuerpo(enviar(get(URL).param("desde", hoy.toString()).param("tamano", "1"), tokenAdmin, null));
        assertThat((List<?>) JsonPath.read(json, "$.contenido")).hasSize(1);
        int total = JsonPath.read(json, "$.totalElementos");
        assertThat(total).isGreaterThanOrEqualTo(2);
        assertThat((Integer) JsonPath.read(json, "$.totalPaginas")).isEqualTo(total);

        String dosPrimeros = cuerpo(enviar(get(URL).param("tamano", "2"), tokenAdmin, null));
        List<String> fechas = JsonPath.read(dosPrimeros, "$.contenido[*].fechaHora");
        assertThat(fechas.get(0)).isGreaterThanOrEqualTo(fechas.get(1));

        String futuro = cuerpo(enviar(get(URL).param("desde", hoy.plusDays(1).toString()), tokenAdmin, null));
        assertThat((Integer) JsonPath.read(futuro, "$.totalElementos")).isZero();

        assertEstado(400, enviar(get(URL).param("desde", hoy.toString()).param("hasta", hoy.minusDays(1).toString()),
                tokenAdmin, null));
        assertEstado(400, enviar(get(URL).param("tamano", "101"), tokenAdmin, null));
        assertEstado(400, enviar(get(URL).param("pagina", "-1"), tokenAdmin, null));
        assertEstado(400, enviar(get(URL).param("tipoEntidad", "NO_EXISTE"), tokenAdmin, null));
    }

    // ---------------------------------------------------------------- IT-05

    @Test
    @DisplayName("IT-05 Solo el ADMINISTRADOR consulta el historial: 401 sin token y 403 para PROFESOR y ESTUDIANTE")
    void it05Seguridad() throws Exception {
        Profesor profesor = new Profesor();
        profesor.setNumeroDocumento(documento());
        Estudiante estudiante = new Estudiante();
        estudiante.setNumeroDocumento(documento());

        assertEstado(401, enviar(get(URL), null, null));
        assertEstado(403, enviar(get(URL), jwtService.generarToken(crearUsuario(profesor, "prof")), null));
        assertEstado(403, enviar(get(URL), jwtService.generarToken(crearUsuario(estudiante, "est")), null));
        assertEstado(200, enviar(get(URL), tokenAdmin, null));
    }

    // ---------------------------------------------------------------- Utilidades

    private List<Map<String, Object>> consultarHoy(String tipoEntidad) throws Exception {
        MvcResult resultado = enviar(get(URL).param("tipoEntidad", tipoEntidad).param("desde", hoy.toString())
                .param("tamano", "100"), tokenAdmin, null);
        assertEstado(200, resultado);
        return JsonPath.read(cuerpo(resultado), "$.contenido");
    }

    private String login(String contrasena) {
        return """
                {"correoInstitucional":"%s","contrasena":"%s"}
                """.formatted(administrador.getCorreoInstitucional(), contrasena);
    }

    private ProgramaAcademico crearPrograma() {
        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setNombre("ITA Programa " + UUID.randomUUID().toString().substring(0, 8));
        programa.setCodigo("ITA" + ThreadLocalRandom.current().nextInt(10_000, 100_000));
        programa.setEstado(EstadoRegistro.ACTIVO);
        return programaRepository.save(programa);
    }

    private <U extends Usuario> U crearUsuario(U usuario, String alias) {
        usuario.setTipoDocumento(tipoDocumento);
        usuario.setNombreCompleto("ITA Usuario " + alias);
        usuario.setCorreoInstitucional("ita-" + alias + "-" + UUID.randomUUID().toString().substring(0, 8)
                + "@uco.net.co");
        usuario.setEstado(EstadoRegistro.ACTIVO);
        return usuarioRepository.save(usuario);
    }

    private static String documento() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
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
}