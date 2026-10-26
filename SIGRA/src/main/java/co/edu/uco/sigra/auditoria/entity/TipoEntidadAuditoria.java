package co.edu.uco.sigra.auditoria.entity;

import java.util.Locale;

public enum TipoEntidadAuditoria {
    USUARIO,
    PROFESOR,
    ESTUDIANTE,
    PROGRAMA,
    ASIGNATURA,
    RESULTADO_APRENDIZAJE,
    SEMESTRE,
    MATRICULA,
    ASIGNACION_DOCENTE,
    EVALUACION,
    CALIFICACION,
    CONCLUSION,
    ACCION_MEJORA,
    OTRO;

    public static TipoEntidadAuditoria desdeClase(Class<?> clase) {
        String nombre = clase.getSimpleName();
        int marcaProxy = nombre.indexOf('$');
        if (marcaProxy > 0) {
            nombre = nombre.substring(0, marcaProxy);
        }
        nombre = quitarSufijo(quitarSufijo(nombre, "Impl"), "Service");
        String constante = nombre.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT);
        if (constante.equals("PROGRAMA_ACADEMICO")) {
            return PROGRAMA;
        }
        for (TipoEntidadAuditoria tipo : values()) {
            if (tipo.name().equals(constante)) {
                return tipo;
            }
        }
        return OTRO;
    }

    private static String quitarSufijo(String texto, String sufijo) {
        return texto.endsWith(sufijo) ? texto.substring(0, texto.length() - sufijo.length()) : texto;
    }
}