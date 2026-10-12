package co.edu.uco.sigra.programas.service.impl;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.programas.dto.ProgramaRequestDTO;
import co.edu.uco.sigra.programas.dto.ProgramaResponseDTO;
import co.edu.uco.sigra.programas.dto.ProgramaUpdateDTO;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.exception.CodigoProgramaDuplicadoException;
import co.edu.uco.sigra.programas.exception.NombreProgramaDuplicadoException;
import co.edu.uco.sigra.programas.exception.ProgramaAcademicoNoEncontradoException;
import co.edu.uco.sigra.programas.exception.ProgramaConAsignaturasActivasException;
import co.edu.uco.sigra.programas.mapper.ProgramaMapper;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.programas.service.ProgramaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgramaServiceImpl implements ProgramaService {

    private final ProgramaAcademicoRepository programaRepository;
    private final ProgramaMapper programaMapper;
    private final AsignaturaRepository asignaturaRepository;

    @Override
    @Transactional
    public List<ProgramaResponseDTO> consultar(EstadoRegistro estado) {
        return consultar(null, null, estado);
    }

    @Override
    @Transactional
    public List<ProgramaResponseDTO> consultar(String nombre, String codigo, EstadoRegistro estado) {
        String nombreFiltro = normalizarFiltro(nombre);
        String codigoFiltro = normalizarFiltro(codigo);
        // El catálogo de programas es pequeño, así que los filtros se aplican en memoria.
        return programaRepository.findAll(Sort.by("nombre")).stream()
                .filter(p -> nombreFiltro == null || p.getNombre().toLowerCase(Locale.ROOT).contains(nombreFiltro))
                .filter(p -> codigoFiltro == null || p.getCodigo().toLowerCase(Locale.ROOT).contains(codigoFiltro))
                .filter(p -> estado == null || estado == p.getEstado())
                .map(programaMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public ProgramaResponseDTO consultarPorId(UUID id) {
        return programaMapper.toResponseDTO(buscarOLanzar(id));
    }

    @Override
    @Transactional
    public ProgramaResponseDTO registrar(ProgramaRequestDTO dto) {
        String nombre = dto.nombre().trim();
        String codigo = dto.codigo().trim();
        if (programaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new NombreProgramaDuplicadoException(nombre);
        }
        if (programaRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new CodigoProgramaDuplicadoException(codigo);
        }
        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setNombre(nombre);
        programa.setCodigo(codigo);
        programa.setEstado(EstadoRegistro.ACTIVO);
        return programaMapper.toResponseDTO(programaRepository.save(programa));
    }

    @Override
    @Transactional
    public ProgramaResponseDTO actualizarNombre(UUID id, ProgramaUpdateDTO dto) {
        ProgramaAcademico programa = buscarOLanzar(id);
        String nombre = dto.nombre().trim();
        if (programaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new NombreProgramaDuplicadoException(nombre);
        }
        programa.setNombre(nombre);
        return programaMapper.toResponseDTO(programaRepository.save(programa));
    }

    @Override
    @Transactional
    public ProgramaResponseDTO inactivar(UUID id) {
        ProgramaAcademico programa = buscarOLanzar(id);
        if (programa.getEstado() == EstadoRegistro.INACTIVO) {
            return programaMapper.toResponseDTO(programa);
        }
        if (asignaturaRepository.existsByPrograma_IdAndEstado(id, EstadoAsignatura.ACTIVA)) {
            throw new ProgramaConAsignaturasActivasException(programa.getNombre());
        }
        // Eliminación lógica: el registro se conserva para no perder trazabilidad.
        programa.setEstado(EstadoRegistro.INACTIVO);
        return programaMapper.toResponseDTO(programaRepository.save(programa));
    }

    private ProgramaAcademico buscarOLanzar(UUID id) {
        return programaRepository.findById(id)
                .orElseThrow(() -> new ProgramaAcademicoNoEncontradoException(id));
    }

    private static String normalizarFiltro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim().toLowerCase(Locale.ROOT);
    }
}
