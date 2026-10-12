package co.edu.uco.sigra.asignaturas.seguridad;

import co.edu.uco.sigra.auth.entity.Administrador;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.profesores.entity.Profesor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Resolvedor de identidad desde el encabezado Authorization")
class ResolvedorIdentidadTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-bytes-0123456789";
    private static final long UNA_HORA_MS = 3_600_000;
    private static final Instant AHORA = Instant.parse("2026-10-06T12:00:00Z");

    @Mock
    private UsuarioRepository usuarioRepository;

    private JwtService jwt;
    private ResolvedorIdentidad resolvedor;

    @BeforeEach
    void setUp() {
        jwt = new JwtService(SECRETO, UNA_HORA_MS, Clock.fixed(AHORA, ZoneOffset.UTC));
        resolvedor = new ResolvedorIdentidad(jwt, usuarioRepository);
    }

    private static <T extends Usuario> T usuario(T usuario, EstadoRegistro estado) {
        usuario.setId(UUID.randomUUID());
        usuario.setCorreoInstitucional("persona@uco.net.co");
        usuario.setEstado(estado);
        return usuario;
    }

    private static MockHttpServletRequest peticion(String autorizacion) {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/v1/asignaturas");
        if (autorizacion != null) {
            peticion.addHeader("Authorization", autorizacion);
        }
        return peticion;
    }

    @Test
    @DisplayName("Un token válido de un usuario ACTIVO devuelve su id y su rol")
    void tokenValido() {
        Profesor profesor = usuario(new Profesor(), EstadoRegistro.ACTIVO);
        when(usuarioRepository.findById(profesor.getId())).thenReturn(Optional.of(profesor));

        Optional<IdentidadActual> identidad = resolvedor.resolver(peticion("Bearer " + jwt.generarToken(profesor)));

        assertThat(identidad).contains(new IdentidadActual(profesor.getId(), RolUsuario.PROFESOR));
    }

    @Test
    @DisplayName("El prefijo Bearer no distingue mayúsculas")
    void prefijoSinMayusculas() {
        Estudiante estudiante = usuario(new Estudiante(), EstadoRegistro.ACTIVO);
        when(usuarioRepository.findById(estudiante.getId())).thenReturn(Optional.of(estudiante));

        assertThat(resolvedor.resolver(peticion("bearer " + jwt.generarToken(estudiante))))
                .map(IdentidadActual::rol).contains(RolUsuario.ESTUDIANTE);
    }

    @ParameterizedTest(name = "Authorization «{0}»")
    @ValueSource(strings = {"", "Bearer", "Bearer ", "Basic dXN1YXJpbzpjbGF2ZQ==", "Bearer abc.def.ghi", "Token xyz"})
    @DisplayName("Sin Bearer, vacío o mal formado no hay identidad")
    void encabezadoInvalido(String autorizacion) {
        assertThat(resolvedor.resolver(peticion(autorizacion))).isEmpty();
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("Sin encabezado no hay identidad")
    void sinEncabezado() {
        assertThat(resolvedor.resolver(peticion(null))).isEmpty();
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("Un token firmado con otro secreto no es válido")
    void otroSecreto() {
        JwtService ajeno = new JwtService("otro-secreto-distinto-con-mas-de-32-bytes-9876543210", UNA_HORA_MS,
                Clock.fixed(AHORA, ZoneOffset.UTC));
        Administrador admin = usuario(new Administrador(), EstadoRegistro.ACTIVO);

        assertThat(resolvedor.resolver(peticion("Bearer " + ajeno.generarToken(admin)))).isEmpty();
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("Un token expirado no es válido")
    void tokenExpirado() {
        JwtService delPasado = new JwtService(SECRETO, UNA_HORA_MS,
                Clock.fixed(AHORA.minusSeconds(2 * 3600), ZoneOffset.UTC));
        Administrador admin = usuario(new Administrador(), EstadoRegistro.ACTIVO);

        assertThat(resolvedor.resolver(peticion("Bearer " + delPasado.generarToken(admin)))).isEmpty();
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("Un usuario que ya no existe no tiene identidad")
    void usuarioInexistente() {
        Administrador admin = usuario(new Administrador(), EstadoRegistro.ACTIVO);
        when(usuarioRepository.findById(any())).thenReturn(Optional.empty());

        assertThat(resolvedor.resolver(peticion("Bearer " + jwt.generarToken(admin)))).isEmpty();
    }

    @Test
    @DisplayName("Un usuario INACTIVO no tiene identidad aunque su token siga vigente")
    void usuarioInactivo() {
        Administrador admin = usuario(new Administrador(), EstadoRegistro.INACTIVO);
        when(usuarioRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThat(resolvedor.resolver(peticion("Bearer " + jwt.generarToken(admin)))).isEmpty();
    }

    @Test
    @DisplayName("Si el rol del token no coincide con el rol real del usuario, no hay identidad")
    void rolDistinto() {
        Administrador comoAdmin = usuario(new Administrador(), EstadoRegistro.ACTIVO);
        Estudiante real = usuario(new Estudiante(), EstadoRegistro.ACTIVO);
        real.setId(comoAdmin.getId());
        when(usuarioRepository.findById(comoAdmin.getId())).thenReturn(Optional.of(real));

        assertThat(resolvedor.resolver(peticion("Bearer " + jwt.generarToken(comoAdmin)))).isEmpty();
    }
}
