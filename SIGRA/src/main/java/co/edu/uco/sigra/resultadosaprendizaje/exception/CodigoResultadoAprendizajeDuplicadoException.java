package co.edu.uco.sigra.resultadosaprendizaje.exception;


public class CodigoResultadoAprendizajeDuplicadoException extends RuntimeException {
    public CodigoResultadoAprendizajeDuplicadoException(String codigo) {
        super("Ya existe un resultado de aprendizaje con el código " + codigo + " en esta asignatura.");
    }
}
