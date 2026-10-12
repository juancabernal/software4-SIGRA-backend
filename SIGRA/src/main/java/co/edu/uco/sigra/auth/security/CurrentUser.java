package co.edu.uco.sigra.auth.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resuelve la identidad autenticada sin aceptar principals genéricos ni valores enviados por el cliente. */
@Component
public class CurrentUser {

    public UsuarioPrincipal required() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new AccessDeniedException("No hay una identidad autenticada válida");
        }
        return principal;
    }
}
