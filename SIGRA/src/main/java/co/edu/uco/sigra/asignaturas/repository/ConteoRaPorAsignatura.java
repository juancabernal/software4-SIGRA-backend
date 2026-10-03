package co.edu.uco.sigra.asignaturas.repository;

import java.util.UUID;

public interface ConteoRaPorAsignatura {
    UUID getAsignaturaId();
    Long getCantidad();
}
