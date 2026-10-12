package co.edu.uco.sigra.common.dto;

import java.util.UUID;

public record TipoDocumentoResponseDTO(
        UUID id,
        String nombre
) {}

