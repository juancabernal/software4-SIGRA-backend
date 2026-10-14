package co.edu.uco.sigra.estudiantes.entity;

/**
 * Estado de una matrícula: {@link #ACTIVA} cuando el estudiante está matriculado en esa
 * asignatura para ese semestre, y {@link #DESVINCULADA} cuando fue retirado. No hay más
 * estados posibles.
 * <p>
 * Es un enum propio del módulo y no {@code common.enums.EstadoRegistro} (design.md, D1).
 * {@code EstadoRegistro} es el vocabulario de «registro dado de baja» que comparten los
 * catálogos y las fichas de medio proyecto, y una matrícula desvinculada no es eso: conserva
 * sus calificaciones vivas y es el testimonio de que un estudiante cursó la asignatura y dejó
 * de cursarla, o sea historial académico. Añadirle {@code DESVINCULADA} al enum compartido
 * metería un valor sin sentido en el dominio de otros módulos y rompería la frontera.
 */
public enum EstadoMatricula {
    ACTIVA,
    DESVINCULADA
}
