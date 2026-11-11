package co.edu.uco.sigra.facultades.exception;

public class NombreFacultadDuplicadoException extends RuntimeException {

    public NombreFacultadDuplicadoException(String nombre) {
        super("Ya existe una facultad con el nombre '" + nombre + "'");
    }
}
