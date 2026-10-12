package co.edu.uco.sigra.resultadosaprendizaje.dto;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;


public final class ReglasEntradaResultadoAprendizaje {

    public static final int CODIGO_MAX = 10;
    public static final int DESCRIPCION_MIN = 10;
    public static final int DESCRIPCION_MAX = 100;


    public static final String PATRON_CODIGO = "^[A-Z0-9]+(-[A-Z0-9]+)*$";

    public static final String PATRON_DESCRIPCION = "^[^<>\\p{Cc}\\p{Cf}]*$";

    public static final String CODIGO_OBLIGATORIO = "El código es obligatorio";
    public static final String CODIGO_LONGITUD = "El código debe tener máximo 10 caracteres";
    public static final String CODIGO_FORMATO =
            "El código solo admite letras sin tilde, números y guiones entre ellos (por ejemplo RA-01)";
    public static final String DESCRIPCION_OBLIGATORIA = "La descripción es obligatoria";
    public static final String DESCRIPCION_LONGITUD = "La descripción debe tener entre 10 y 100 caracteres";
    public static final String DESCRIPCION_CARACTERES =
            "La descripción no puede contener los caracteres < o > ni caracteres de control";


    private static final Pattern ESPACIOS = Pattern.compile("[\\s\\p{Z}]+");

    private ReglasEntradaResultadoAprendizaje() {
    }


    public static String normalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.strip().toUpperCase(Locale.ROOT);
    }

    public static String normalizarDescripcion(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String nfc = Normalizer.normalize(descripcion, Normalizer.Form.NFC);
        return ESPACIOS.matcher(nfc).replaceAll(" ").strip();
    }
}
