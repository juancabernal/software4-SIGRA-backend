package co.edu.uco.sigra.estudiantes.exception;

public class CorreoEstudianteDuplicadoException extends RuntimeException {

    private CorreoEstudianteDuplicadoException(String mensaje) {
        super(mensaje);
    }

    public static CorreoEstudianteDuplicadoException porCorreo(String correoInstitucional) {
        return new CorreoEstudianteDuplicadoException(
                "Ya existe un estudiante registrado con el correo " + correoInstitucional + ".");
    }
}
