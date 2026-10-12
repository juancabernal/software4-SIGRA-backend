package co.edu.uco.sigra.asignaturas.seguridad;

import co.edu.uco.sigra.asignaturas.exception.AutenticacionRequeridaException;
import co.edu.uco.sigra.asignaturas.exception.PermisoInsuficienteException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Optional;

/**
 * Control de acceso por rol del módulo de asignaturas, <strong>temporal hasta RF-05</strong>.
 * <p>
 * Con {@code sigra.seguridad.roles.habilitado=false} (por defecto) los endpoints se comportan como antes:
 * sin token y sin alcance. Encendido, aplica la matriz declarada con {@link RolesPermitidos} en cada
 * método del controlador: sin identidad válida, 401; rol no permitido, 403. Los endpoints marcados con
 * {@code siempre = true} exigen identidad aunque el interruptor esté apagado. Las peticiones preflight
 * (OPTIONS) siempre pasan. La identidad queda en el atributo {@link IdentidadActual#ATRIBUTO}.
 * <p>
 * Cómo retirarlo cuando exista RF-05:
 * <ol>
 *   <li>Descomentar los {@code @PreAuthorize} del controlador (ya reflejan esta misma matriz).</li>
 *   <li>Obtener la identidad del SecurityContext en lugar del atributo de la petición.</li>
 *   <li>Borrar el paquete {@code asignaturas.seguridad}, el registro en WebConfig y la propiedad.</li>
 * </ol>
 */
@Component
public class ControlAccesoAsignaturasInterceptor implements HandlerInterceptor {

    private final ObjectProvider<ResolvedorIdentidad> resolvedor;
    private final boolean habilitado;

    /**
     * El resolvedor se pide de forma diferida: las pruebas {@code @WebMvcTest} de cualquier módulo cargan
     * los HandlerInterceptor pero no JwtService ni JPA, y así su contexto sigue arrancando.
     */
    public ControlAccesoAsignaturasInterceptor(ObjectProvider<ResolvedorIdentidad> resolvedor,
                                               @Value("${sigra.seguridad.roles.habilitado:false}") boolean habilitado) {
        this.resolvedor = resolvedor;
        this.habilitado = habilitado;
    }

    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object manejador) {
        if ("OPTIONS".equalsIgnoreCase(peticion.getMethod()) || !(manejador instanceof HandlerMethod metodo)) {
            return true;
        }
        RolesPermitidos regla = metodo.getMethodAnnotation(RolesPermitidos.class);
        if (regla == null || (!habilitado && !regla.siempre())) {
            return true;
        }
        ResolvedorIdentidad disponible = resolvedor.getIfAvailable();
        IdentidadActual identidad = (disponible == null ? Optional.<IdentidadActual>empty()
                : disponible.resolver(peticion))
                .orElseThrow(AutenticacionRequeridaException::new);
        if (Arrays.stream(regla.value()).noneMatch(rol -> rol == identidad.rol())) {
            throw new PermisoInsuficienteException();
        }
        peticion.setAttribute(IdentidadActual.ATRIBUTO, identidad);
        return true;
    }
}
