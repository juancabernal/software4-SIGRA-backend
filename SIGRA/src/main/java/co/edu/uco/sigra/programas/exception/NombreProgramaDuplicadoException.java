package co.edu.uco.sigra.programas.exception;

public class NombreProgramaDuplicadoException extends RuntimeException {

    public NombreProgramaDuplicadoException(String nombre) {
        super("Ya existe un programa académico con el nombre '" + nombre + "'");
    }
}
