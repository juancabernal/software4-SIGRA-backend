package co.edu.uco.sigra.estudiantes.exception;

public class DocumentoNoModificableException extends RuntimeException {

    private DocumentoNoModificableException(String mensaje) {
        super(mensaje);
    }

    public static DocumentoNoModificableException paraEstudiante() {
        return new DocumentoNoModificableException(
                "El tipo y el número de documento del estudiante no se pueden modificar después del registro.");
    }
}
