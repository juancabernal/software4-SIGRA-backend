package co.edu.uco.sigra.reportes.exception;

/** El rango de fechas de un reporte está invertido, es demasiado amplio o queda fuera del semestre (400). */
public class RangoReporteInvalidoException extends RuntimeException {
    public RangoReporteInvalidoException(String mensaje) {
        super(mensaje);
    }
}
