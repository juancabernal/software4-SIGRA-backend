package co.edu.uco.sigra.estudiantes.service;

import java.util.UUID;

/**
 * Puerto hacia las calificaciones de una matrícula (design.md, D4).
 * <p>
 * El módulo {@code calificaciones} todavía no existe: hoy es de otro integrante del equipo y
 * está vacío. Mientras tanto, esta interfaz es el único punto por el que {@code estudiantes}
 * sabe si una matrícula tiene calificaciones registradas, para poder exigir la confirmación
 * adicional al desvincular (ver requisito "Confirmación adicional para desvincular una
 * matrícula con calificaciones"). El día que {@code calificaciones} exista, bastará con que
 * publique un {@code @Service} que implemente este método con una consulta real: no hace falta
 * cambiar nada en {@code estudiantes}.
 * <p>
 * Devuelve un conteo y no un booleano porque el mensaje de rechazo de la desvinculación tiene
 * que decir cuántas calificaciones están en juego, no solo que existen.
 */
public interface CalificacionesDeMatricula {

    /**
     * Cuenta las calificaciones registradas para una matrícula.
     *
     * @param matriculaId identificador de la matrícula a consultar
     * @return la cantidad de calificaciones registradas; {@code 0} si no tiene ninguna
     */
    long contarPorMatricula(UUID matriculaId);
}
