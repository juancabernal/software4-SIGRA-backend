package co.edu.uco.sigra.auth.service;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.common.enums.RolUsuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    public static final String ALGORITMO_PERMITIDO = "HS256";

    private final SecretKey key;
    private final long expirationMs;
    private final Clock clock;

    // Constructor que usa Spring: al haber dos constructores, este debe marcarse explícitamente.
    @Autowired
    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        this(secret, expirationMs, Clock.systemUTC());
    }

    public JwtService(String secret, long expirationMs, Clock clock) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes para HS256");
        }
        if (expirationMs <= 0) {
            throw new IllegalArgumentException("JWT_EXPIRATION_MS debe ser mayor que cero");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    // Claims mínimos por minimización de datos: el nombre se obtiene de LoginResponseDTO.
    public String generarToken(Usuario usuario) {
        Instant ahora = clock.instant();
        Instant expiracion = ahora.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("correo", usuario.getCorreoInstitucional())
                .claim("rol", usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiracion))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims extraerClaims(String token) {
        Jws<Claims> firmado = Jwts.parser()
                .clock(() -> Date.from(clock.instant()))
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                ;
        if (!ALGORITMO_PERMITIDO.equals(firmado.getHeader().getAlgorithm())) {
            throw new JwtException("Algoritmo JWT no permitido");
        }
        Claims claims = firmado.getPayload();
        if (claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.get("correo", String.class) == null
                || claims.get("rol", String.class) == null
                || claims.getIssuedAt() == null
                || claims.getExpiration() == null) {
            throw new JwtException("Claims JWT requeridos ausentes");
        }
        // Fuerza la validación de los tipos y valores que utiliza autorización.
        java.util.UUID.fromString(claims.getSubject());
        RolUsuario.valueOf(claims.get("rol", String.class));
        if (claims.get("correo", String.class).isBlank()) {
            throw new JwtException("Claim de correo inválido");
        }
        return claims;
    }

    public String extraerSubject(String token) {
        return extraerClaims(token).getSubject();
    }

    public String extraerCorreo(String token) {
        return extraerClaims(token).get("correo", String.class);
    }

    public RolUsuario extraerRol(String token) {
        String rol = extraerClaims(token).get("rol", String.class);
        return RolUsuario.valueOf(rol);
    }

    public boolean esTokenValido(String token) {
        try {
            Claims claims = extraerClaims(token);
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(Date.from(clock.instant()));
        } catch (JwtException | IllegalArgumentException e) {
            // Nunca se registra el token ni el mensaje de la excepción (puede contener fragmentos del token).
            log.debug("Token JWT rechazado: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    public long getExpiracionSegundos() {
        return expirationMs / 1000;
    }
}
