package co.edu.uco.sigra.auth;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.RolUsuario;
import co.edu.uco.sigra.profesores.entity.Profesor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// UNIT: no levanta Spring; usa un reloj fijo para comprobar emisión, expiración y firma.
class JwtServiceTest {

    private static final String SECRET_VALIDO = "clave_secreta_super_segura_de_al_menos_32_caracteres_12345";
    private static final long EXPIRACION_MS = 3600000; // 60 minutos
    private static final Instant AHORA = Instant.parse("2026-10-04T12:00:00Z");

    private JwtService jwtService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(AHORA, ZoneOffset.UTC);
        jwtService = new JwtService(SECRET_VALIDO, EXPIRACION_MS, fixedClock);

        usuario = new Profesor();
        usuario.setId(UUID.randomUUID());
        usuario.setCorreoInstitucional("profesor.test@uco.net.co");
        usuario.setNombreCompleto("Profesor de Prueba");
    }

    @Test
    void generarTokenGeneraJwtConClaimsEsperados() {
        String token = jwtService.generarToken(usuario);

        assertThat(token).isNotBlank();
        assertThat(jwtService.esTokenValido(token)).isTrue();
        assertThat(jwtService.extraerSubject(token)).isEqualTo(usuario.getId().toString());
        assertThat(jwtService.extraerCorreo(token)).isEqualTo("profesor.test@uco.net.co");
        assertThat(jwtService.extraerRol(token)).isEqualTo(RolUsuario.PROFESOR);

        Claims claims = jwtService.extraerClaims(token);
        long iatEpoch = claims.getIssuedAt().getTime();
        long expEpoch = claims.getExpiration().getTime();
        assertThat(expEpoch - iatEpoch).isEqualTo(EXPIRACION_MS);
    }

    @Test
    void tokenEmitidoUsaExplicitamenteAlgoritmoHS256() {
        String token = jwtService.generarToken(usuario);
        SecretKey clave = Keys.hmacShaKeyFor(SECRET_VALIDO.getBytes(StandardCharsets.UTF_8));

        Jws<Claims> jws = Jwts.parser()
                .clock(() -> Date.from(AHORA))
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token);

        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("HS256");
    }

    @Test
    void tokenContieneSoloLosClaimsMinimosNecesarios() {
        String token = jwtService.generarToken(usuario);

        Claims claims = jwtService.extraerClaims(token);

        assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "correo", "rol", "iat", "exp");
        assertThat(claims.containsKey("nombre")).isFalse();
    }

    @Test
    void rechazaClaveConMenosDe32Bytes() {
        String secretCorto = "clave_demasiado_corta";
        assertThatThrownBy(() -> new JwtService(secretCorto, EXPIRACION_MS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos 32 bytes");
    }

    @Test
    void rechazaExpiracionCeroONegativaAlConfigurar() {
        assertThatThrownBy(() -> new JwtService(SECRET_VALIDO, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_EXPIRATION_MS");
        assertThatThrownBy(() -> new JwtService(SECRET_VALIDO, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("JWT_EXPIRATION_MS");
    }

    @Test
    void tokenManipuladoEsRechazado() {
        String token = jwtService.generarToken(usuario);
        String tokenManipulado = token.substring(0, token.length() - 5) + "abcde";

        assertThat(jwtService.esTokenValido(tokenManipulado)).isFalse();
    }

    @Test
    void tokenExpiradoEsRechazado() {
        String token = jwtService.generarToken(usuario);

        // Simulamos un reloj 61 minutos después de la emisión
        Clock relojFuturo = Clock.fixed(AHORA.plusMillis(EXPIRACION_MS + 60000), ZoneOffset.UTC);
        JwtService servicioConRelojFuturo = new JwtService(SECRET_VALIDO, EXPIRACION_MS, relojFuturo);

        assertThat(servicioConRelojFuturo.esTokenValido(token)).isFalse();
    }

    @Test
    void getExpiracionSegundosDevuelve3600() {
        assertThat(jwtService.getExpiracionSegundos()).isEqualTo(3600);
    }
}
