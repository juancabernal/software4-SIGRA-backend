package co.edu.uco.sigra.asignaturas.service;

import co.edu.uco.sigra.asignaturas.dto.AsignacionMasivaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.ResultadoAsignacionMasivaDTO;

import java.util.UUID;

public interface AsignacionDocenteService {

    /**
     * Asigna cada profesor de la petición a cada asignatura de la petición (RF-07). No falla toda
     * la operación por un par individual: lo que ya estaba asignado se omite, y lo que tiene un
     * profesor o una asignatura inactivos se rechaza. Ambos se reportan en el resultado.
     */
    ResultadoAsignacionMasivaDTO asignarMasivo(AsignacionMasivaRequestDTO dto);

    /** Eliminación lógica: cambia el estado a INACTIVO. Idempotente si ya estaba inactiva. */
    void desasignar(UUID id);
}
