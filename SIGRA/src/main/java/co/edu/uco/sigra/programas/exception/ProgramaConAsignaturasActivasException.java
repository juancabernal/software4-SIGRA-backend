package co.edu.uco.sigra.programas.exception;

public class ProgramaConAsignaturasActivasException extends RuntimeException {

    public ProgramaConAsignaturasActivasException(String nombre) {
        super("No se puede inactivar el programa '" + nombre + "' porque tiene asignaturas activas");
    }
}
