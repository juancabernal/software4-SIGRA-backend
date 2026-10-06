package co.edu.uco.sigra.asignaturas.repository;

import java.util.UUID;

/** Proyección de un conteo agrupado por asignatura (RA, matrículas...). */
public interface ConteoPorAsignatura {
    UUID getAsignaturaId();
    Long getCantidad();
}
