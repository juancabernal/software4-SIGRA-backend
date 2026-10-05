package co.edu.uco.sigra.common.util;

import java.util.Locale;

/**
 * Normalización única de correos institucionales: sin espacios laterales y en minúsculas.
 * Se usa en la entidad (antes de persistir), en la autenticación y en la validación de unicidad.
 */
public final class Correos {

    private Correos() {
    }

    public static String normalizar(String correo) {
        if (correo == null) {
            return null;
        }
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
