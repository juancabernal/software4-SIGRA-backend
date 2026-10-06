package co.edu.uco.sigra.asignaturas.seguridad;

import co.edu.uco.sigra.common.enums.RolUsuario;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Roles que pueden invocar un endpoint de asignaturas. La aplica {@link ControlAccesoAsignaturasInterceptor}
 * solo si el interruptor {@code sigra.seguridad.roles.habilitado} está encendido, salvo que
 * {@link #siempre()} sea verdadero: entonces exige identidad aunque el interruptor esté apagado (endpoints
 * que dependen de quién pregunta, como «mis asignaturas»).
 * <p>
 * Es el equivalente temporal del {@code @PreAuthorize} que dejará RF-05.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RolesPermitidos {

    RolUsuario[] value();

    boolean siempre() default false;
}
