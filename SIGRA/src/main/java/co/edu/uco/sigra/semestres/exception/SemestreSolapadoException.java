package co.edu.uco.sigra.semestres.exception;

public class SemestreSolapadoException extends RuntimeException {
    public SemestreSolapadoException() {
        super("El rango de fechas se cruza con el de otro semestre registrado");
    }
}
