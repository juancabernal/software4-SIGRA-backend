package co.edu.uco.sigra.estudiantes.seguridad;

import co.edu.uco.sigra.common.enums.RolUsuario;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Roles que pueden invocar un endpoint de matrícula. La aplica {@link ControlAccesoMatriculaInterceptor}
 * solo si el interruptor {@code sigra.seguridad.roles.habilitado} está encendido.
 * <p>
 * Es el equivalente temporal del {@code @PreAuthorize} que dejará RF-05.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RolesPermitidosMatricula {

    RolUsuario[] value();
}
