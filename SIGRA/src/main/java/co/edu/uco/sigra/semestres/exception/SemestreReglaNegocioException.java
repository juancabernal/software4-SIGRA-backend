package co.edu.uco.sigra.semestres.exception;

/** Violación de una regla de negocio de semestres (fechas inválidas, extensión no permitida...). */
public class SemestreReglaNegocioException extends RuntimeException {
    public SemestreReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
