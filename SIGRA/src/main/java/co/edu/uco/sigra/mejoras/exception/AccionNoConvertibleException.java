package co.edu.uco.sigra.mejoras.exception;

/**
 * La acción de mejora no se puede convertir en evaluación porque el nombre o la descripción que hereda
 * no cumplen las reglas de una evaluación (RF-11): longitud máxima o caracteres no permitidos (400).
 */
public class AccionNoConvertibleException extends RuntimeException {
    public AccionNoConvertibleException(String detalle) {
        super("La acción de mejora no se puede convertir en evaluación: " + detalle);
    }
}
