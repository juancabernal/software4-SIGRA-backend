package co.edu.uco.sigra.asignaturas.dto;

import co.edu.uco.sigra.common.enums.EstadoRegistro;

import java.time.LocalDate;
import java.util.UUID;

public record AsignacionDocenteResponseDTO(
        UUID id,
        UUID profesorId,
        String nombreProfesor,
        UUID asignaturaId,
        String codigoAsignatura,
        String nombreAsignatura,
        EstadoRegistro estado,
        LocalDate fechaAsignacion
) {}
