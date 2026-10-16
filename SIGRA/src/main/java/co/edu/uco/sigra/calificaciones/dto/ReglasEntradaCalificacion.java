package co.edu.uco.sigra.calificaciones.dto;

import java.text.Normalizer;

public final class ReglasEntradaCalificacion {

    public static final String NOTA_MINIMA = "0.0";
    public static final String NOTA_MAXIMA = "5.0";
    public static final int OBSERVACION_MIN = 10;
    public static final int OBSERVACION_MAX = 500;

    public static final String PATRON_OBSERVACION = "^[^<>]*$";

    public static final String NOTA_OBLIGATORIA = "La nota es obligatoria";
    public static final String NOTA_RANGO = "La nota debe estar entre 0.0 y 5.0";
    public static final String NOTA_DECIMALES = "La nota admite como máximo un decimal (por ejemplo 3.5)";
    public static final String OBSERVACION_OBLIGATORIA = "La observación es obligatoria";
    public static final String OBSERVACION_LONGITUD = "La observación debe tener entre 10 y 500 caracteres";
    public static final String OBSERVACION_CARACTERES = "La observación no puede contener los caracteres < o >";
    public static final String MATRICULA_OBLIGATORIA = "Debe indicar la matrícula del estudiante";

    private ReglasEntradaCalificacion() {
    }

    public static String normalizarObservacion(String observacion) {
        if (observacion == null) {
            return null;
        }
        return Normalizer.normalize(observacion, Normalizer.Form.NFC).strip();
    }
}