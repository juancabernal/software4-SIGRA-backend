package co.edu.uco.sigra.asignaturas.exception;

public class RangoRaInvalidoException extends RuntimeException {

    private RangoRaInvalidoException(String mensaje) {
        super(mensaje);
    }

    public static RangoRaInvalidoException pocosRa(int minimo, long actuales) {
        return new RangoRaInvalidoException(
                "Para activar la asignatura se requieren al menos " + minimo
                        + " resultados de aprendizaje activos y actualmente tiene " + actuales + ".");
    }

    public static RangoRaInvalidoException demasiadosRa(int maximo, long actuales) {
        return new RangoRaInvalidoException(
                "Una asignatura admite como máximo " + maximo
                        + " resultados de aprendizaje activos y actualmente tiene " + actuales + ".");
    }
}
