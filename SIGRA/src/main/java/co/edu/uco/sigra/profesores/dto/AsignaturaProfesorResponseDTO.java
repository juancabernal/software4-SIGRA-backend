package co.edu.uco.sigra.profesores.dto;

import co.edu.uco.sigra.common.enums.EstadoRegistro;

import java.util.UUID;

public record AsignaturaProfesorResponseDTO (
    UUID asignacionId,
    UUID asignaturaId,
    String codigoAsignatura,
    String nombreAsignatura,
    EstadoRegistro estadoAsignatura
){}