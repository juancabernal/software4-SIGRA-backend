package co.edu.uco.sigra.programas.service.impl;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.mapper.ProgramaMapper;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.programas.service.ProgramaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgramaServiceImpl implements ProgramaService {

    private final ProgramaAcademicoRepository programaRepository;
    private final ProgramaMapper programaMapper;

    @Override
    @Transactional
    public List<ProgramaResponseDTO> consultar(EstadoRegistro estado) {
        // El catálogo de programas es pequeño, así que el filtro por estado se hace en memoria.
        return programaRepository.findAll(Sort.by("nombre")).stream()
                .filter(p -> estado == null || estado == p.getEstado())
                .map(programaMapper::toResponseDTO)
                .toList();
    }
}
