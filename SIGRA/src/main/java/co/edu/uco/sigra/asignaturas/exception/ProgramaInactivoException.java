package co.edu.uco.sigra.asignaturas.exception;

public class ProgramaInactivoException extends RuntimeException {
    public ProgramaInactivoException(String nombrePrograma) {
        super("El programa académico \"" + nombrePrograma + "\" está inactivo y no admite nuevas asignaturas.");
    }
}
