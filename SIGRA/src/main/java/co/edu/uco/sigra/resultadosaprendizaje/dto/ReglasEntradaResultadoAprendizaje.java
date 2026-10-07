package co.edu.uco.sigra.resultadosaprendizaje.dto;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Saneamiento y reglas de entrada de los resultados de aprendizaje (RNF-14).
 * <p>
 * Sigue el mismo criterio que {@code asignaturas.dto.ReglasEntrada}: los DTO de entrada normalizan en su
 * constructor compacto, antes de Bean Validation, así que las validaciones se aplican sobre el valor ya
 * normalizado. Los mensajes nunca repiten el valor enviado.
 * Longitudes según el MR: código hasta 10 caracteres y descripción hasta 500.
 */
public final class ReglasEntradaResultadoAprendizaje {

    public static final int CODIGO_MAX = 10;
    public static final int DESCRIPCION_MAX = 500;

    /** Letras sin tilde y números, con guiones solo entre ellos (por ejemplo RA-01). */
    public static final String PATRON_CODIGO = "^[A-Z0-9]+(-[A-Z0-9]+)*$";
    /** Sin < ni > y sin caracteres de control ni de formato invisibles. */
    public static final String PATRON_DESCRIPCION = "^[^<>\\p{Cc}\\p{Cf}]*$";

    public static final String CODIGO_OBLIGATORIO = "El código es obligatorio";
    public static final String CODIGO_LONGITUD = "El código debe tener máximo 10 caracteres";
    public static final String CODIGO_FORMATO =
            "El código solo admite letras sin tilde, números y guiones entre ellos (por ejemplo RA-01)";
    public static final String DESCRIPCION_OBLIGATORIA = "La descripción es obligatoria";
    public static final String DESCRIPCION_LONGITUD = "La descripción debe tener máximo 500 caracteres";
    public static final String DESCRIPCION_CARACTERES =
            "La descripción no puede contener los caracteres < o > ni caracteres de control";

    /** Cualquier secuencia de espacios en blanco, incluidos tabs, saltos de línea y espacios Unicode. */
    private static final Pattern ESPACIOS = Pattern.compile("[\\s\\p{Z}]+");

    private ReglasEntradaResultadoAprendizaje() {
    }

    /**
     * Recorta y pasa a mayúsculas (Locale.ROOT). Se usa strip() y no trim(): trim() también borra
     * caracteres de control en los extremos, y esos deben llegar a la validación para rechazarse.
     */
    public static String normalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.strip().toUpperCase(Locale.ROOT);
    }

    /** Normalización Unicode NFC, espacios repetidos (incluidos saltos de línea) reducidos a uno y recorte. */
    public static String normalizarDescripcion(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String nfc = Normalizer.normalize(descripcion, Normalizer.Form.NFC);
        return ESPACIOS.matcher(nfc).replaceAll(" ").strip();
    }
}
