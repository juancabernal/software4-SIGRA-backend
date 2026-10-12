package co.edu.uco.sigra.estudiantes.seguridad;

import co.edu.uco.sigra.asignaturas.seguridad.IdentidadActual;
import co.edu.uco.sigra.asignaturas.seguridad.ResolvedorIdentidad;
import co.edu.uco.sigra.estudiantes.exception.AutenticacionRequeridaException;
import co.edu.uco.sigra.estudiantes.exception.PermisoInsuficienteException;
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
 * Control de acceso por rol del módulo de matrículas, <strong>temporal hasta RF-05</strong>.
 * <p>
 * Con {@code sigra.seguridad.roles.habilitado=false} (por defecto) los endpoints se comportan como antes:
 * sin token y sin alcance. Encendido, aplica la matriz declarada con {@link RolesPermitidosMatricula} en
 * cada método del controlador: sin identidad válida, 401; rol no permitido, 403. Las peticiones preflight
 * (OPTIONS) siempre pasan.
 * <p>
 * <strong>Acoplamiento deliberado (design.md, D8):</strong> este interceptor depende de
 * {@link ResolvedorIdentidad} e {@link IdentidadActual}, ambos de {@code asignaturas.seguridad}, en lugar
 * de duplicar la validación de JWT en este módulo. Su autor documentó que ese paquete
 * <strong>se borrará</strong> cuando llegue RF-05: si eso ocurre sin retirar primero este interceptor, el
 * fallo es un error de compilación inmediato en este archivo (no un error silencioso en producción), y los
 * {@code @PreAuthorize} comentados en {@code MatriculaController} son la ruta de salida ya escrita.
 * <p>
 * Cómo retirarlo cuando exista RF-05:
 * <ol>
 *   <li>Descomentar los {@code @PreAuthorize} de {@code MatriculaController} (ya reflejan esta misma matriz).</li>
 *   <li>Obtener la identidad del {@code SecurityContext} en lugar de pedirla a este interceptor.</li>
 *   <li>Borrar este paquete ({@code estudiantes.seguridad}) y su registro en {@code ConfiguracionWebMatricula}.</li>
 * </ol>
 */
@Component
public class ControlAccesoMatriculaInterceptor implements HandlerInterceptor {

    private final ObjectProvider<ResolvedorIdentidad> resolvedor;
    private final boolean habilitado;

    /**
     * El resolvedor se pide de forma diferida: las pruebas {@code @WebMvcTest} de cualquier módulo cargan
     * los HandlerInterceptor pero no JwtService ni JPA, y así su contexto sigue arrancando.
     */
    public ControlAccesoMatriculaInterceptor(ObjectProvider<ResolvedorIdentidad> resolvedor,
                                             @Value("${sigra.seguridad.roles.habilitado:false}") boolean habilitado) {
        this.resolvedor = resolvedor;
        this.habilitado = habilitado;
    }

    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object manejador) {
        if ("OPTIONS".equalsIgnoreCase(peticion.getMethod()) || !(manejador instanceof HandlerMethod metodo)) {
            return true;
        }
        RolesPermitidosMatricula regla = metodo.getMethodAnnotation(RolesPermitidosMatricula.class);
        if (regla == null || !habilitado) {
            return true;
        }
        ResolvedorIdentidad disponible = resolvedor.getIfAvailable();
        IdentidadActual identidad = (disponible == null ? Optional.<IdentidadActual>empty()
                : disponible.resolver(peticion))
                .orElseThrow(AutenticacionRequeridaException::new);
        if (Arrays.stream(regla.value()).noneMatch(rol -> rol == identidad.rol())) {
            throw new PermisoInsuficienteException();
        }
        return true;
    }
}
