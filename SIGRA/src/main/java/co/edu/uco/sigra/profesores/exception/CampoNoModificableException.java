package co.edu.uco.sigra.profesores.exception;

public class CampoNoModificableException extends RuntimeException {
    public CampoNoModificableException(String nombreCampo) {
        super("El campo '" + nombreCampo + "' no puede modificarse una vez registrado");
    }
}
