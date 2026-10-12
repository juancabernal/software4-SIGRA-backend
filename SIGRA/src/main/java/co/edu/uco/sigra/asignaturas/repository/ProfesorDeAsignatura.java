package co.edu.uco.sigra.asignaturas.repository;

import java.util.UUID;

/** Proyección: nombre de un profesor con asignación docente en una asignatura. */
public interface ProfesorDeAsignatura {
    UUID getAsignaturaId();
    String getNombre();
}
