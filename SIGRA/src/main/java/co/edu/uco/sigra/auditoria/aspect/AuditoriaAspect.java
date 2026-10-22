package co.edu.uco.sigra.auditoria.aspect;

import co.edu.uco.sigra.auditoria.entity.AccionAuditoria;
import co.edu.uco.sigra.auditoria.entity.TipoEntidadAuditoria;
import co.edu.uco.sigra.auditoria.service.AuditoriaService;
import co.edu.uco.sigra.auth.dto.LoginResponseDTO;
import co.edu.uco.sigra.auth.dto.UsuarioSesionDTO;
import co.edu.uco.sigra.auth.exception.CredencialesInvalidasException;
import co.edu.uco.sigra.auth.exception.UsuarioBloqueadoException;
import co.edu.uco.sigra.calificaciones.dto.CalificacionResponseDTO;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Registra en el historial de auditoría (RF-19) los eventos de los demás módulos sin tocar su código:
 * <ul>
 *   <li><b>Login</b>: exitoso (con el usuario) y rechazado (credenciales inválidas o usuario bloqueado).</li>
 *   <li><b>Cambios de estado</b>: cualquier método {@code inactivar*}, {@code activar*},
 *       {@code reactivar*}, {@code desvincular*} o {@code desasignar*} de un servicio
 *       ({@code co.edu.uco.sigra..service.*Service}). Un módulo nuevo queda auditado solo con seguir
 *       esa convención de nombres.</li>
 *   <li><b>Calificaciones</b>: el registro de una nota fuera de la fecha tentativa y toda modificación.</li>
 * </ul>
 * Solo se audita lo que terminó bien ({@code @AfterReturning}); una operación rechazada no cambió nada.
 * <p>
 * {@code @Order(0)} deja este aspecto por fuera de la transacción del servicio: el evento se escribe
 * cuando el cambio ya se confirmó. Si al registrar ocurre un error, se deja en el log y no se tumba la
 * respuesta de una operación que ya quedó hecha.
 */
@Aspect
@Component
@Order(0)
public class AuditoriaAspect {

    private static final Logger LOG = LoggerFactory.getLogger(AuditoriaAspect.class);

    private static final String LOGIN = "execution(* co.edu.uco.sigra.auth.service.AuthService.autenticar(..))";

    private static final String CAMBIO_DE_ESTADO =
            "execution(* co.edu.uco.sigra..service.*Service.inactivar*(..))"
                    + " || execution(* co.edu.uco.sigra..service.*Service.activar*(..))"
                    + " || execution(* co.edu.uco.sigra..service.*Service.reactivar*(..))"
                    + " || execution(* co.edu.uco.sigra..service.*Service.desvincular*(..))"
                    + " || execution(* co.edu.uco.sigra..service.*Service.desasignar*(..))";

    private static final String REGISTRAR_CALIFICACION =
            "execution(* co.edu.uco.sigra.calificaciones.service.CalificacionService.registrar(..))";

    private static final String MODIFICAR_CALIFICACION =
            "execution(* co.edu.uco.sigra.calificaciones.service.CalificacionService.modificar(..))";

    private final AuditoriaService auditoria;

    public AuditoriaAspect(AuditoriaService auditoria) {
        this.auditoria = auditoria;
    }

    // ------------------------------------------------------------------ Login

    @AfterReturning(pointcut = LOGIN, returning = "respuesta")
    public void loginExitoso(LoginResponseDTO respuesta) {
        UsuarioSesionDTO usuario = respuesta.usuario();
        registrar(() -> auditoria.registrarEvento(TipoEntidadAuditoria.USUARIO, usuario.id(),
                AccionAuditoria.LOGIN_EXITOSO, "Inicio de sesión exitoso como " + usuario.rol(),
                usuario.id(), usuario.rol()));
    }

    /** No se guarda el correo intentado: el historial no debe exponer datos de quien no pudo entrar. */
    @AfterThrowing(pointcut = LOGIN, throwing = "error")
    public void loginRechazado(RuntimeException error) {
        String motivo;
        if (error instanceof UsuarioBloqueadoException) {
            motivo = "usuario bloqueado temporalmente por intentos fallidos";
        } else if (error instanceof CredencialesInvalidasException) {
            motivo = "credenciales inválidas";
        } else {
            return; // Otros errores (por ejemplo, la base caída) no son intentos de inicio de sesión.
        }
        registrar(() -> auditoria.registrarEvento(TipoEntidadAuditoria.USUARIO, null,
                AccionAuditoria.LOGIN_FALLIDO, "Inicio de sesión rechazado: " + motivo, null, null));
    }

    // ------------------------------------------------------------------ Cambios de estado

    @AfterReturning(pointcut = CAMBIO_DE_ESTADO, returning = "resultado")
    public void cambioDeEstado(JoinPoint punto, Object resultado) {
        TipoEntidadAuditoria tipo = TipoEntidadAuditoria.desdeClase(punto.getTarget().getClass());
        UUID idEntidad = primerUuid(punto.getArgs());
        String detalle = "Cambio de estado: " + punto.getSignature().getName() + estadoResultante(resultado);
        registrar(() -> auditoria.registrarEvento(tipo, idEntidad, AccionAuditoria.CAMBIO_ESTADO, detalle));
    }

    // ------------------------------------------------------------------ Calificaciones

    @AfterReturning(pointcut = REGISTRAR_CALIFICACION, returning = "calificacion")
    public void calificacionRegistrada(CalificacionResponseDTO calificacion) {
        if (!calificacion.fueraDeFecha()) {
            return;
        }
        registrar(() -> auditoria.registrarEvento(TipoEntidadAuditoria.CALIFICACION, calificacion.id(),
                AccionAuditoria.CALIFICACION_FUERA_DE_FECHA,
                "Nota " + calificacion.nota() + " registrada después de la fecha tentativa de la evaluación "
                        + calificacion.evaluacionId()));
    }

    @AfterReturning(pointcut = MODIFICAR_CALIFICACION, returning = "calificacion")
    public void calificacionModificada(CalificacionResponseDTO calificacion) {
        registrar(() -> auditoria.registrarEvento(TipoEntidadAuditoria.CALIFICACION, calificacion.id(),
                AccionAuditoria.CALIFICACION_MODIFICADA,
                "Nota modificada; valor vigente " + calificacion.nota()
                        + (calificacion.fueraDeFecha() ? " (fuera de la fecha tentativa)" : "")));
    }

    // ------------------------------------------------------------------ Auxiliares

    private static void registrar(Runnable evento) {
        try {
            evento.run();
        } catch (RuntimeException e) {
            LOG.error("No se pudo registrar un evento de auditoría", e);
        }
    }

    private static UUID primerUuid(Object[] argumentos) {
        for (Object argumento : argumentos) {
            if (argumento instanceof UUID id) {
                return id;
            }
        }
        return null;
    }

    /** Si el servicio devuelve un DTO con {@code estado()}, se anota el estado en el que quedó la entidad. */
    private static String estadoResultante(Object resultado) {
        if (resultado == null) {
            return "";
        }
        try {
            Method estado = resultado.getClass().getMethod("estado");
            Object valor = estado.invoke(resultado);
            return valor == null ? "" : " (nuevo estado: " + valor + ")";
        } catch (ReflectiveOperationException | RuntimeException e) {
            return "";
        }
    }
}