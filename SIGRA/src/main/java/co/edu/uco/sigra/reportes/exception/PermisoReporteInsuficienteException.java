package co.edu.uco.sigra.reportes.exception;

/** El rol no puede consultar el reporte, o el profesor pidió una asignatura que no tiene asignada (403). */
public class PermisoReporteInsuficienteException extends RuntimeException {
    public PermisoReporteInsuficienteException() {
        super("No tiene permisos suficientes para realizar esta acción.");
    }
}
