package co.edu.uco.sigra.facultades.exception;

public class FacultadInactivaException extends RuntimeException {

    public FacultadInactivaException(String nombre) {
        super("No se puede asociar un programa a la facultad '" + nombre + "' porque está inactiva");
    }
}
