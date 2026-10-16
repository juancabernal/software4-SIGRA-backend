package co.edu.uco.sigra.common.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalización de texto para búsquedas: sin tildes ni diacríticos y en minúsculas, de modo que
 * «gomez», «Gómez» y «GÓMEZ» coincidan. Solo se usa para comparar; nunca para persistir.
 */
public final class Textos {

    private Textos() {
    }

    public static String normalizarBusqueda(String valor) {
        if (valor == null) {
            return null;
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Recorta el término de búsqueda; uno nulo o en blanco equivale a no filtrar (null). */
    public static String terminoDeBusqueda(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return normalizarBusqueda(valor.strip());
    }
}