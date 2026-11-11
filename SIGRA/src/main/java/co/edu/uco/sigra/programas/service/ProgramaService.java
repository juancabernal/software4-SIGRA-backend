package co.edu.uco.sigra.programas.service;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.programas.dto.ProgramaRequestDTO;
import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.dto.ProgramaUpdateDTO;

import java.util.List;
import java.util.UUID;

public interface ProgramaService {

    /** Programas ordenados por nombre; si {@code estado} es null devuelve todos. */
    List<ProgramaResponseDTO> consultar(EstadoRegistro estado);

    /** Filtros opcionales: nombre y código se comparan por contenido, sin distinguir mayúsculas. */
    List<ProgramaResponseDTO> consultar(String nombre, String codigo, EstadoRegistro estado);

    /**
     * Igual que {@link #consultar(String, String, EstadoRegistro)} y, además, si {@code facultadId} no es
     * null devuelve solo los programas de esa facultad (una facultad sin programas, o inexistente, da
     * lista vacía).
     */
    List<ProgramaResponseDTO> consultar(String nombre, String codigo, UUID facultadId, EstadoRegistro estado);

    ProgramaResponseDTO consultarPorId(UUID id);

    ProgramaResponseDTO registrar(ProgramaRequestDTO dto);

    /**
     * Cambia el nombre y, si {@code dto.facultadId()} viene informado y es distinto del actual, mueve el
     * programa a esa facultad (que debe existir y estar ACTIVA). El código no se modifica nunca.
     */
    ProgramaResponseDTO actualizar(UUID id, ProgramaUpdateDTO dto);

    ProgramaResponseDTO inactivar(UUID id);
}
