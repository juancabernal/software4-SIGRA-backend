package co.edu.uco.sigra.evaluaciones.dto;

import java.text.Normalizer;
import java.util.regex.Pattern;


public final class ReglasEntradaEvaluacion {

    public static final int NOMBRE_MAX = 30;
    public static final int DESCRIPCION_MAX = 100;
    public static final String PORCENTAJE_MIN = "0";
    public static final String PORCENTAJE_MAX = "100";

    /** Sin < ni > y sin caracteres de control ni de formato invisibles. */
    public static final String PATRON_TEXTO = "^[^<>\\p{Cc}\\p{Cf}]*$";

    public static final String NOMBRE_OBLIGATORIO = "El nombre es obligatorio";
    public static final String NOMBRE_LONGITUD = "El nombre debe tener máximo 30 caracteres";
    public static final String NOMBRE_CARACTERES =
            "El nombre no puede contener los caracteres < o > ni caracteres de control";
    public static final String FECHA_OBLIGATORIA = "La fecha tentativa es obligatoria";
    public static final String PORCENTAJE_OBLIGATORIO = "El porcentaje es obligatorio";
    public static final String PORCENTAJE_RANGO = "El porcentaje debe ser mayor que 0 y máximo 100";
    public static final String PORCENTAJE_DECIMALES = "El porcentaje admite máximo 2 decimales";
    public static final String DESCRIPCION_OBLIGATORIA = "La descripción es obligatoria";
    public static final String DESCRIPCION_LONGITUD = "La descripción debe tener máximo 100 caracteres";
    public static final String DESCRIPCION_CARACTERES =
            "La descripción no puede contener los caracteres < o > ni caracteres de control";

    /** Cualquier secuencia de espacios en blanco, incluidos tabs, saltos de línea y espacios Unicode. */
    private static final Pattern ESPACIOS = Pattern.compile("[\\s\\p{Z}]+");

    private ReglasEntradaEvaluacion() {
    }

    /** Normalización Unicode NFC, espacios repetidos reducidos a uno y recorte. */
    public static String normalizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String nfc = Normalizer.normalize(texto, Normalizer.Form.NFC);
        return ESPACIOS.matcher(nfc).replaceAll(" ").strip();
    }
}
