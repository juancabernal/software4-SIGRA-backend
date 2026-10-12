package co.edu.uco.sigra.programas.dto;

import co.edu.uco.sigra.common.enums.EstadoRegistro;

import java.util.UUID;

public record ProgramaResponseDTO(
        UUID id,
        String codigo,
        String nombre,
        EstadoRegistro estado
) {}
