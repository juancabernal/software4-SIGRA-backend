package co.edu.uco.sigra.facultades.exception;

public class FacultadConProgramasActivosException extends RuntimeException {

    public FacultadConProgramasActivosException(String nombre) {
        super("No se puede inactivar la facultad '" + nombre + "' porque tiene programas activos");
    }
}
