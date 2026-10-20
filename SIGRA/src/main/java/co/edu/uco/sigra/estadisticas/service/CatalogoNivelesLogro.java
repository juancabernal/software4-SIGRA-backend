package co.edu.uco.sigra.estadisticas.service;

import co.edu.uco.sigra.estadisticas.entity.NivelLogro;

import java.util.List;

/**
 * Catálogo de los 4 rangos de nivel de logro (RF-14), sobre la escala de notas 0.0 – 5.0.
 * <p>
 * Rangos confirmados por el equipo: Bajo 0 – 2.99, Medio 3 – 3.99, Alto 4 – 4.49 y
 * Sobresaliente 4.5 – 5. Se implementan como intervalos semiabiertos {@code [mínimo, siguiente)}
 * para que no queden huecos entre 2.99 y 3.00 (por ejemplo, un promedio ponderado de 2.995 es
 * Bajo, porque todavía no llega a 3.0). El último rango incluye el 5.0.
 * <p>
 * Es una clase utilitaria sin estado para que también la pueda usar el módulo de calificaciones
 * al clasificar cada calificación individual (RF-14: nivel de logro de cada calificación).
 */
public final class CatalogoNivelesLogro {

    public static final double NOTA_MINIMA = 0.0;
    public static final double NOTA_MAXIMA = 5.0;

    /** Rango de un nivel: desde {@code notaMinima} (incluida) hasta {@code notaLimite}. */
    public record Rango(NivelLogro nivel, double notaMinima, double notaLimite, boolean limiteIncluido) {

        public boolean contiene(double nota) {
            return nota >= notaMinima && (limiteIncluido ? nota <= notaLimite : nota < notaLimite);
        }
    }

    private static final List<Rango> RANGOS = List.of(
            new Rango(NivelLogro.BAJO, 0.0, 3.0, false),
            new Rango(NivelLogro.MEDIO, 3.0, 4.0, false),
            new Rango(NivelLogro.ALTO, 4.0, 4.5, false),
            new Rango(NivelLogro.SOBRESALIENTE, 4.5, NOTA_MAXIMA, true));

    private CatalogoNivelesLogro() {
    }

    /** Los 4 rangos en orden ascendente (Bajo, Medio, Alto, Sobresaliente). */
    public static List<Rango> rangos() {
        return RANGOS;
    }

    /**
     * Clasifica una nota (o un promedio ponderado) en su nivel de logro.
     *
     * @throws IllegalArgumentException si la nota está fuera de 0.0 – 5.0 o no es un número
     */
    public static NivelLogro clasificar(double nota) {
        return RANGOS.stream()
                .filter(rango -> rango.contiene(nota))
                .findFirst()
                .map(Rango::nivel)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La nota " + nota + " está fuera de la escala " + NOTA_MINIMA + " – " + NOTA_MAXIMA + "."));
    }
}
