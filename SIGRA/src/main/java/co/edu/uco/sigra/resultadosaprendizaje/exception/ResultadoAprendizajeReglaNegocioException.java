package co.edu.uco.sigra.resultadosaprendizaje.exception;


public class ResultadoAprendizajeReglaNegocioException extends RuntimeException {

    private ResultadoAprendizajeReglaNegocioException(String mensaje) {
        super(mensaje);
    }


    public static ResultadoAprendizajeReglaNegocioException maximoActivosAlcanzado(int maximo) {
        return new ResultadoAprendizajeReglaNegocioException(
                "La asignatura ya tiene " + maximo + " resultados de aprendizaje activos, que es el máximo permitido.");
    }


    public static ResultadoAprendizajeReglaNegocioException minimoActivosRequerido(int minimo) {
        return new ResultadoAprendizajeReglaNegocioException(
                "No se puede inactivar: una asignatura activa debe tener mínimo " + minimo
                        + " resultados de aprendizaje activos.");
    }


    public static ResultadoAprendizajeReglaNegocioException asignaturaInactiva() {
        return new ResultadoAprendizajeReglaNegocioException(
                "La asignatura está inactiva; no se pueden registrar ni reactivar resultados de aprendizaje.");
    }

    public static ResultadoAprendizajeReglaNegocioException yaInactivo(String codigo) {
        return new ResultadoAprendizajeReglaNegocioException(
                "El resultado de aprendizaje " + codigo + " ya se encuentra inactivo.");
    }

    public static ResultadoAprendizajeReglaNegocioException yaActivo(String codigo) {
        return new ResultadoAprendizajeReglaNegocioException(
                "El resultado de aprendizaje " + codigo + " ya se encuentra activo.");
    }

    public static ResultadoAprendizajeReglaNegocioException descripcionDeInactivo(String codigo) {
        return new ResultadoAprendizajeReglaNegocioException(
                "No se puede modificar la descripción del resultado de aprendizaje " + codigo
                        + " porque está inactivo.");
    }
}
