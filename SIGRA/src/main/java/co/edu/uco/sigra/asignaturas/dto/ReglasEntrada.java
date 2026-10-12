package co.edu.uco.sigra.asignaturas.dto;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Saneamiento y reglas de entrada del módulo de asignaturas.
 * <p>
 * Los DTO de entrada normalizan en su constructor compacto (antes de que se ejecute Bean Validation),
 * así que las validaciones se aplican sobre los valores ya normalizados. Los mensajes nunca repiten el
 * valor enviado.
 */
public final class ReglasEntrada {

    public static final int CODIGO_MIN = 3;
    public static final int CODIGO_MAX = 20;
    public static final int NOMBRE_MIN = 3;
    public static final int NOMBRE_MAX = 100;
    public static final int TEXTO_BUSQUEDA_MAX = 100;
    public static final int RA_FILTRO_MAX = 999;

    /** Letras sin tilde y números, con guiones solo entre ellos: sin guion al inicio, al final ni seguidos. */
    public static final String PATRON_CODIGO = "^[A-Z0-9]+(-[A-Z0-9]+)*$";
    /** Sin < ni > y sin caracteres de control ni de formato invisibles. */
    public static final String PATRON_NOMBRE = "^[^<>\\p{Cc}\\p{Cf}]*$";

    public static final String CODIGO_OBLIGATORIO = "El código es obligatorio";
    public static final String CODIGO_LONGITUD = "El código debe tener entre 3 y 20 caracteres";
    public static final String CODIGO_FORMATO =
            "El código solo admite letras sin tilde, números y guiones entre ellos (por ejemplo MAT-401)";
    public static final String NOMBRE_OBLIGATORIO = "El nombre es obligatorio";
    public static final String NOMBRE_LONGITUD = "El nombre debe tener entre 3 y 100 caracteres";
    public static final String NOMBRE_CARACTERES =
            "El nombre no puede contener los caracteres < o > ni caracteres de control";
    public static final String TEXTO_LONGITUD = "El texto de búsqueda no puede superar los 100 caracteres";
    public static final String RA_NEGATIVO = "La cantidad de RA no puede ser negativa";
    public static final String RA_MAXIMO = "La cantidad de RA no puede ser mayor que 999";

    /** Cualquier secuencia de espacios en blanco, incluidos tabs, saltos de línea y espacios Unicode. */
    private static final Pattern ESPACIOS = Pattern.compile("[\\s\\p{Z}]+");

    private ReglasEntrada() {
    }

    /**
     * Recorta y pasa a mayúsculas (Locale.ROOT). Se usa strip() y no trim(): trim() también borra
     * caracteres de control en los extremos, y esos deben llegar a la validación para rechazarse.
     */
    public static String normalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.strip().toUpperCase(Locale.ROOT);
    }

    /** Normalización Unicode NFC, espacios repetidos reducidos a uno y recorte. */
    public static String normalizarNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        String nfc = Normalizer.normalize(nombre, Normalizer.Form.NFC);
        return ESPACIOS.matcher(nfc).replaceAll(" ").strip();
    }

    /** Recorta el texto de búsqueda; uno vacío equivale a no filtrar. */
    public static String normalizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.strip();
        return recortado.isEmpty() ? null : recortado;
    }
}
