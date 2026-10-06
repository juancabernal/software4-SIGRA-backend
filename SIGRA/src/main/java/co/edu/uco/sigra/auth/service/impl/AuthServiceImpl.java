package co.edu.uco.sigra.auth.service.impl;

import co.edu.uco.sigra.auth.config.AuthConfig;
import co.edu.uco.sigra.auth.dto.LoginRequestDTO;
import co.edu.uco.sigra.auth.dto.LoginResponseDTO;
import co.edu.uco.sigra.auth.dto.UsuarioSesionDTO;
import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.exception.CredencialesInvalidasException;
import co.edu.uco.sigra.auth.exception.UsuarioBloqueadoException;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.AuthService;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.util.Correos;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    public static final int MAX_INTENTOS_FALLIDOS = 5;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    // Hash BCrypt factor 12 descartable, generado solo para esta comparación. No corresponde a ninguna
    // contraseña real. Se usa cuando no hay una contraseña que comparar, para que el rechazo tarde
    // lo mismo que una contraseña incorrecta y no revele si el correo existe.
    private static final String HASH_FICTICIO = "$2a$12$xDGOSBjhORAgIJE.KqQ.lecW7FEnpyd9RpMUBeHc71RRoxPrN/yK6";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           @Qualifier(AuthConfig.RELOJ_AUTH) Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Override
    @Transactional(noRollbackFor = {CredencialesInvalidasException.class, UsuarioBloqueadoException.class})
    public LoginResponseDTO autenticar(LoginRequestDTO request) {
        String correoNormalizado = Correos.normalizar(request.correoInstitucional());
        LocalDateTime ahora = LocalDateTime.now(clock);

        Optional<Usuario> usuarioEncontrado = usuarioRepository.findByCorreoInstitucionalIgnoreCase(correoNormalizado);
        if (usuarioEncontrado.isEmpty()) {
            compararConHashFicticio(request.contrasena());
            throw new CredencialesInvalidasException();
        }
        Usuario usuario = usuarioEncontrado.get();

        if (usuario.getEstado() != EstadoRegistro.ACTIVO) {
            compararConHashFicticio(request.contrasena());
            throw new CredencialesInvalidasException();
        }

        // Bloqueado: se rechaza sin evaluar la contraseña y sin modificar el contador.
        if (usuario.estaBloqueado(ahora, DURACION_BLOQUEO)) {
            throw new UsuarioBloqueadoException();
        }

        // Si hubo un bloqueo y ya expiró, el nuevo intento empieza con el contador reiniciado.
        if (usuario.getFechaBloqueo() != null) {
            usuario.reiniciarContadorIntentos();
        }

        if (usuario.getPasswordHash() == null) {
            compararConHashFicticio(request.contrasena());
            throw registrarFallo(usuario, ahora);
        }

        if (!passwordEncoder.matches(request.contrasena(), usuario.getPasswordHash())) {
            throw registrarFallo(usuario, ahora);
        }

        usuario.reiniciarContadorIntentos();
        usuarioRepository.save(usuario);

        String token = jwtService.generarToken(usuario);
        UsuarioSesionDTO usuarioSesionDTO = new UsuarioSesionDTO(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreoInstitucional(),
                usuario.getRol()
        );

        return new LoginResponseDTO(
                token,
                "Bearer",
                jwtService.getExpiracionSegundos(),
                usuarioSesionDTO
        );
    }

    private void compararConHashFicticio(String contrasena) {
        passwordEncoder.matches(contrasena, HASH_FICTICIO);
    }

    private CredencialesInvalidasException registrarFallo(Usuario usuario, LocalDateTime ahora) {
        usuario.registrarIntentoFallido(ahora, MAX_INTENTOS_FALLIDOS);
        usuarioRepository.save(usuario);
        return new CredencialesInvalidasException();
    }
}
