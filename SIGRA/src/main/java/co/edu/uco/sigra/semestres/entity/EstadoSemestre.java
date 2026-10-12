package co.edu.uco.sigra.semestres.entity;

/**
 * Estado de un semestre. Se deriva de las fechas: ACTIVO solo si la fecha actual
 * está dentro de [fechaInicio, fechaFin]; INACTIVO si aún no empieza o ya terminó.
 */
public enum EstadoSemestre {
    ACTIVO,
    INACTIVO
}
