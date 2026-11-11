package co.edu.uco.sigra.reportes.exception;

/** El semestre, el programa o la asignatura del filtro no existen (404). El mensaje no incluye identificadores. */
public class ReporteRecursoNoEncontradoException extends RuntimeException {
    public ReporteRecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
