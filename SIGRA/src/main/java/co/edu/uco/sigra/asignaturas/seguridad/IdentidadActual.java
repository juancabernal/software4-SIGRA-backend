package co.edu.uco.sigra.asignaturas.seguridad;

import co.edu.uco.sigra.common.enums.RolUsuario;

import java.util.UUID;

/**
 * Quién hace la petición: el id del usuario (el mismo id del profesor, estudiante o administrador)
 * y su rol. El interceptor la deja como atributo de la petición con el nombre {@link #ATRIBUTO}.
 */
public record IdentidadActual(UUID id, RolUsuario rol) {

    public static final String ATRIBUTO = "sigra.asignaturas.identidad";
}
