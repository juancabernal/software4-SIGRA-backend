package co.edu.uco.sigra.profesores.exception;

public class CampoNoModificableException extends RuntimeException {
  public CampoNoModificableException(String message) {
    super(message);
  }
}
