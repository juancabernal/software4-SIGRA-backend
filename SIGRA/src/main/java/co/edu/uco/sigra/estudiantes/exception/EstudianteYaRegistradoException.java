package co.edu.uco.sigra.estudiantes.exception;

public class EstudianteYaRegistradoException extends RuntimeException {

    private EstudianteYaRegistradoException(String mensaje) {
        super(mensaje);
    }

    public static EstudianteYaRegistradoException porDocumento(String numeroDocumento) {
        return new EstudianteYaRegistradoException(
                "El estudiante con el documento " + numeroDocumento + " ya está registrado en el sistema.");
    }
}
