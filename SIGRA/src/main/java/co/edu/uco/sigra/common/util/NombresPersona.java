package co.edu.uco.sigra.common.util;

/**
 * Partes de nombre de persona (0000035): sin espacios internos; segundo nombre opcional.
 */
public final class NombresPersona {

    public static final String PATRON_PARTE = "^\\p{L}+$";
    public static final String MENSAJE_PARTE =
            "Solo puede contener letras, sin espacios ni caracteres especiales";

    private NombresPersona() {
    }

    public static String completo(
            String primerNombre, String segundoNombre, String primerApellido, String segundoApellido) {
        StringBuilder sb = new StringBuilder();
        append(sb, primerNombre);
        append(sb, segundoNombre);
        append(sb, primerApellido);
        append(sb, segundoApellido);
        return sb.toString();
    }

    public static String blankToNull(String valor) {
        if (valor == null) {
            return null;
        }
        String trim = valor.trim();
        return trim.isEmpty() ? null : trim;
    }

    private static void append(StringBuilder sb, String parte) {
        String limpio = blankToNull(parte);
        if (limpio == null) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(limpio);
    }
}
