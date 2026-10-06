package co.edu.uco.sigra.asignaturas.seguridad;

import co.edu.uco.sigra.auth.entity.Usuario;
import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.auth.service.JwtService;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.enums.RolUsuario;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Obtiene la identidad desde el encabezado {@code Authorization: Bearer <token>} con el JwtService
 * existente, sin depender de cómo arme el SecurityContext el módulo de seguridad (RF-05).
 * <p>
 * El token es válido si {@link JwtService#esTokenValido} lo acepta, su {@code sub} es el id de un
 * usuario que existe y está ACTIVO, y el rol del token coincide con el rol real del usuario. Cualquier
 * otro caso devuelve vacío (el interceptor responde 401). Nunca se registra el token.
 */
@Component
@RequiredArgsConstructor
public class ResolvedorIdentidad {

    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public Optional<IdentidadActual> resolver(HttpServletRequest peticion) {
        String encabezado = peticion.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado == null || !encabezado.regionMatches(true, 0, PREFIJO, 0, PREFIJO.length())) {
            return Optional.empty();
        }
        String token = encabezado.substring(PREFIJO.length()).strip();
        if (token.isEmpty() || !jwtService.esTokenValido(token)) {
            return Optional.empty();
        }
        try {
            UUID id = UUID.fromString(jwtService.extraerSubject(token));
            RolUsuario rolDelToken = jwtService.extraerRol(token);
            return usuarioRepository.findById(id)
                    .filter(usuario -> usuario.getEstado() == EstadoRegistro.ACTIVO)
                    .filter(usuario -> usuario.getRol() == rolDelToken)
                    .map(Usuario::getRol)
                    .map(rol -> new IdentidadActual(id, rol));
        } catch (RuntimeException e) {
            // sub que no es un UUID, rol desconocido o claims ilegibles: es un token no válido.
            return Optional.empty();
        }
    }
}
