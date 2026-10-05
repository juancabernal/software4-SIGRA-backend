package co.edu.uco.sigra.auth.service;

import co.edu.uco.sigra.auth.dto.LoginRequestDTO;
import co.edu.uco.sigra.auth.dto.LoginResponseDTO;
import co.edu.uco.sigra.auth.exception.CredencialesInvalidasException;
import co.edu.uco.sigra.auth.exception.UsuarioBloqueadoException;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.impl.AuthServiceImpl;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.profesores.entity.Profesor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// UNIT: AuthServiceImpl con repositorio, encoder y JWT simulados y un reloj fijo (sin Thread.sleep).
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String CORREO = "profesor.valido@uco.net.co";
    private static final String PASSWORD_CORRECTO = "PasswordSeguro123*";
    private static final String PASSWORD_INCORRECTO = "ClaveErronea123*";
    private static final String HASH_PASSWORD = "$2a$12$ejemploHashRealBcryptSeguro123456789";

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Instant T0 = Instant.parse("2026-10-04T12:00:00Z");
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private Clock clock;
    private AuthServiceImpl authService;

    private Profesor usuario;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(T0, ZONA);
        authService = new AuthServiceImpl(usuarioRepository, passwordEncoder, jwtService, clock);

        usuario = new Profesor();
        usuario.setId(UUID.randomUUID());
        usuario.setCorreoInstitucional(CORREO);
        usuario.setNombreCompleto("Profesor de Prueba");
        usuario.setPasswordHash(HASH_PASSWORD);
        usuario.setEstado(EstadoRegistro.ACTIVO);
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueo(null);
    }

    @Test
    @DisplayName("PB04-01: Login correcto - credenciales válidas, usuario activo, emite token y resetea estado")
    void pb04_01_loginCorrecto() {
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD_CORRECTO, HASH_PASSWORD)).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn("token.jwt.hs256");
        when(jwtService.getExpiracionSegundos()).thenReturn(3600L);

        LoginResponseDTO respuesta = authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_CORRECTO));

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.token()).isEqualTo("token.jwt.hs256");
        assertThat(respuesta.tipo()).isEqualTo("Bearer");
        assertThat(respuesta.expiraEn()).isEqualTo(3600L);
        assertThat(respuesta.usuario().id()).isEqualTo(usuario.getId());
        assertThat(respuesta.usuario().rol()).isEqualTo(RolUsuario.PROFESOR);

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getFechaBloqueo()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("PB04-02: Correo inexistente - rechaza sin diferenciar usuario y ejecuta BCrypt ficticio")
    void pb04_02_correoInexistente() {
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase("inexistente@uco.net.co"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.autenticar(new LoginRequestDTO("inexistente@uco.net.co", PASSWORD_CORRECTO)))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");

        // Mitigación de enumeración por tiempo: se compara contra un hash ficticio.
        verify(passwordEncoder, times(1)).matches(eq(PASSWORD_CORRECTO), anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("PB04-03: Contraseña incorrecta - incrementa intentos fallidos a 1 y persiste")
    void pb04_03_passwordIncorrectaPrimerFallo() {
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD_INCORRECTO, HASH_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_INCORRECTO)))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");

        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
        assertThat(usuario.getFechaBloqueo()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("PB04-04: Varios fallos - el contador de intentos avanza correctamente")
    void pb04_04_variosFallosAvanzaContador() {
        usuario.setIntentosFallidos(2);
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD_INCORRECTO, HASH_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_INCORRECTO)))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(3);
        assertThat(usuario.getFechaBloqueo()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("PB04-05: Quinto fallo - intentos = 5 y fechaBloqueo != null")
    void pb04_05_quintoFalloBloqueaUsuario() {
        usuario.setIntentosFallidos(4);
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD_INCORRECTO, HASH_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_INCORRECTO)))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(5);
        assertThat(usuario.getFechaBloqueo()).isNotNull();
        assertThat(usuario.getFechaBloqueo()).isEqualTo(LocalDateTime.now(clock));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("PB04-06: Usuario bloqueado - no autentica aunque contraseña sea correcta y NO llama a passwordEncoder.matches")
    void pb04_06_usuarioBloqueadoNoEvaluaPassword() {
        usuario.setIntentosFallidos(5);
        usuario.setFechaBloqueo(LocalDateTime.now(clock).minusMinutes(5)); // Bloqueado hace 5 min
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_CORRECTO)))
                .isInstanceOf(UsuarioBloqueadoException.class)
                .hasMessage("El usuario está temporalmente bloqueado. Intente nuevamente más tarde.");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generarToken(any());
    }

    @Test
    @DisplayName("PB04-07: Bloqueo expirado - tras 15 minutos permite nuevo intento y si es exitoso reinicia bloqueo")
    void pb04_07_bloqueoExpiradoPermiteNuevoIntento() {
        // Bloqueo ocurrió hace 16 minutos
        usuario.setIntentosFallidos(5);
        usuario.setFechaBloqueo(LocalDateTime.now(clock).minusMinutes(16));

        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD_CORRECTO, HASH_PASSWORD)).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn("token.nuevo.jwt");
        when(jwtService.getExpiracionSegundos()).thenReturn(3600L);

        LoginResponseDTO respuesta = authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_CORRECTO));

        assertThat(respuesta.token()).isEqualTo("token.nuevo.jwt");
        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getFechaBloqueo()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("PB04-08: Login correcto después de fallos previos (1 a 4) - reinicia contador a 0")
    void pb04_08_loginCorrectoDespuesDeFallosReiniciaContador() {
        usuario.setIntentosFallidos(3);
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD_CORRECTO, HASH_PASSWORD)).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn("token.jwt");
        when(jwtService.getExpiracionSegundos()).thenReturn(3600L);

        authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_CORRECTO));

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getFechaBloqueo()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("PB04-09: Usuario inactivo - rechaza como credenciales inválidas, BCrypt ficticio y no genera token")
    void pb04_09_usuarioInactivoRechazado() {
        usuario.setEstado(EstadoRegistro.INACTIVO);
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_CORRECTO)))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");

        verify(passwordEncoder, times(1)).matches(eq(PASSWORD_CORRECTO), anyString());
        verify(passwordEncoder, never()).matches(eq(PASSWORD_CORRECTO), eq(HASH_PASSWORD));
        verify(jwtService, never()).generarToken(any());
    }

    @Test
    @DisplayName("PB04-10: Password hash null - rechaza con BCrypt ficticio, cuenta el intento y no produce error 500")
    void pb04_10_passwordHashNullRechazadoSeguro() {
        usuario.setPasswordHash(null);
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_CORRECTO)))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");

        verify(passwordEncoder, times(1)).matches(eq(PASSWORD_CORRECTO), anyString());
        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("PB04-11: Correo con espacios y mayúsculas - se normaliza antes de consultar")
    void pb04_11_correoNormalizado() {
        String correoConEspaciosYMayusculas = "  PROFESOR.VALIDO@UCO.NET.CO  ";
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD_CORRECTO, HASH_PASSWORD)).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn("token.jwt");
        when(jwtService.getExpiracionSegundos()).thenReturn(3600L);

        LoginResponseDTO respuesta = authService.autenticar(
                new LoginRequestDTO(correoConEspaciosYMayusculas, PASSWORD_CORRECTO)
        );

        assertThat(respuesta).isNotNull();
        verify(usuarioRepository).findByCorreoInstitucionalIgnoreCase(CORREO);
    }

    @Test
    @DisplayName("RF-04 bloqueo: exactamente a los 15 minutos el bloqueo ya terminó y el login correcto entra")
    void bloqueoTerminaExactamenteA15Minutos() {
        usuario.setIntentosFallidos(5);
        usuario.setFechaBloqueo(LocalDateTime.now(clock).minus(BLOQUEO));

        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD_CORRECTO, HASH_PASSWORD)).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn("token.jwt");
        when(jwtService.getExpiracionSegundos()).thenReturn(3600L);

        authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_CORRECTO));

        assertThat(usuario.getIntentosFallidos()).isZero();
        assertThat(usuario.getFechaBloqueo()).isNull();
    }

    @Test
    @DisplayName("RF-04 bloqueo: una milésima antes de los 15 minutos sigue bloqueado, sin evaluar la contraseña")
    void unaMilesimaAntesDeLos15MinutosSigueBloqueado() {
        usuario.setIntentosFallidos(5);
        usuario.setFechaBloqueo(LocalDateTime.now(clock).minus(BLOQUEO).plus(Duration.ofMillis(1)));
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_CORRECTO)))
                .isInstanceOf(UsuarioBloqueadoException.class);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("RF-04 bloqueo: bloqueo expirado con contraseña incorrecta reinicia el contador y deja el intento en 1")
    void bloqueoExpiradoConPasswordIncorrectaReiniciaContador() {
        usuario.setIntentosFallidos(5);
        usuario.setFechaBloqueo(LocalDateTime.now(clock).minusMinutes(16));
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(PASSWORD_INCORRECTO, HASH_PASSWORD)).thenReturn(false);

        assertThatThrownBy(() -> authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_INCORRECTO)))
                .isInstanceOf(CredencialesInvalidasException.class);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(1);
        assertThat(usuario.getFechaBloqueo()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    @DisplayName("RF-04 bloqueo: durante el bloqueo el repositorio no se guarda y el contador no cambia")
    void duranteBloqueoNoSePersisteCambioAlguno() {
        LocalDateTime fechaBloqueo = LocalDateTime.now(clock).minusMinutes(5);
        usuario.setIntentosFallidos(5);
        usuario.setFechaBloqueo(fechaBloqueo);
        when(usuarioRepository.findByCorreoInstitucionalIgnoreCase(CORREO)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.autenticar(new LoginRequestDTO(CORREO, PASSWORD_INCORRECTO)))
                .isInstanceOf(UsuarioBloqueadoException.class);

        assertThat(usuario.getIntentosFallidos()).isEqualTo(5);
        assertThat(usuario.getFechaBloqueo()).isEqualTo(fechaBloqueo);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Transaccionalidad: autenticar declara noRollbackFor para CredencialesInvalidasException y UsuarioBloqueadoException")
    void transaccionalidadNoRollbackForConfigurada() throws NoSuchMethodException {
        Method metodoAutenticar = AuthServiceImpl.class.getMethod("autenticar", LoginRequestDTO.class);
        Transactional anotacion = metodoAutenticar.getAnnotation(Transactional.class);

        assertThat(anotacion).isNotNull();
        assertThat(anotacion.noRollbackFor()).contains(
                CredencialesInvalidasException.class,
                UsuarioBloqueadoException.class
        );
    }
}
