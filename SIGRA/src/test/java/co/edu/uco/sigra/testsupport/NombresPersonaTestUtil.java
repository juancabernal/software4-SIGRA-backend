package co.edu.uco.sigra.testsupport;

import co.edu.uco.sigra.auth.entity.Usuario;

import java.util.Arrays;

/**
 * Apoyo para pruebas (0000035): reparte un texto de nombre en las cuatro partes de {@link Usuario}
 * (primer nombre, segundo nombre, primer apellido, segundo apellido).
 *
 * <ul>
 *   <li>1 palabra: primer nombre; primer apellido {@code Prueba}; segundo apellido por defecto</li>
 *   <li>2 palabras: primer nombre + primer apellido; segundo apellido por defecto</li>
 *   <li>3 palabras: primer nombre, primer apellido y segundo apellido</li>
 *   <li>4 o más: primer nombre, segundo nombre (palabras intermedias) y los dos últimos como apellidos</li>
 * </ul>
 *
 * <p>Sobre entidades se usa {@code S/D} como segundo apellido por defecto (la columna es NOT NULL);
 * en cuerpos JSON enviados a la API se usa una palabra de solo letras para pasar la validación.</p>
 */
public final class NombresPersonaTestUtil {

    public static final String SEGUNDO_APELLIDO_POR_DEFECTO = "S/D";
    public static final String SEGUNDO_APELLIDO_VALIDO_API = "Prueba";

    private NombresPersonaTestUtil() {
    }

    /** Devuelve {primerNombre, segundoNombre|null, primerApellido, segundoApellido}. */
    public static String[] partes(String texto, String segundoApellidoPorDefecto) {
        String[] t = texto.trim().split("\\s+");
        return switch (t.length) {
            case 1 -> new String[] {t[0], null, "Prueba", segundoApellidoPorDefecto};
            case 2 -> new String[] {t[0], null, t[1], segundoApellidoPorDefecto};
            case 3 -> new String[] {t[0], null, t[1], t[2]};
            default -> new String[] {
                    t[0],
                    String.join(" ", Arrays.copyOfRange(t, 1, t.length - 2)),
                    t[t.length - 2],
                    t[t.length - 1]
            };
        };
    }

    public static void aplicar(Usuario usuario, String texto) {
        String[] p = partes(texto, SEGUNDO_APELLIDO_POR_DEFECTO);
        usuario.setPrimerNombre(p[0]);
        usuario.setSegundoNombre(p[1]);
        usuario.setPrimerApellido(p[2]);
        usuario.setSegundoApellido(p[3]);
    }

    /**
     * Fragmento JSON con las cuatro partes (sin llaves ni coma final), apto para la API.
     * Con {@code null} no incluye ninguna parte de nombre.
     */
    public static String fragmentoJson(String texto) {
        if (texto == null) {
            return "";
        }
        String[] p = partes(texto, SEGUNDO_APELLIDO_VALIDO_API);
        StringBuilder sb = new StringBuilder();
        sb.append("\"primerNombre\":").append(literal(p[0]));
        if (p[1] != null) {
            sb.append(",\"segundoNombre\":").append(literal(p[1]));
        }
        sb.append(",\"primerApellido\":").append(literal(p[2]));
        sb.append(",\"segundoApellido\":").append(literal(p[3]));
        return sb.toString();
    }

    private static String literal(String valor) {
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
