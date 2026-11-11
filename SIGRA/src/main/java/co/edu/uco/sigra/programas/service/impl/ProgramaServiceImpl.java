package co.edu.uco.sigra.programas.service.impl;

import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.util.Textos;
import co.edu.uco.sigra.facultades.entity.Facultad;
import co.edu.uco.sigra.facultades.exception.FacultadInactivaException;
import co.edu.uco.sigra.facultades.exception.FacultadNoEncontradaException;
import co.edu.uco.sigra.facultades.repository.FacultadRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgramaServiceImpl implements ProgramaService {

    private final ProgramaAcademicoRepository programaRepository;
    private final FacultadRepository facultadRepository;
    private final ProgramaMapper programaMapper;
    private final AsignaturaRepository asignaturaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProgramaResponseDTO> consultar(EstadoRegistro estado) {
        return consultar(null, null, estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramaResponseDTO> consultar(String nombre, String codigo, EstadoRegistro estado) {
        return consultar(nombre, codigo, null, estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramaResponseDTO> consultar(String nombre, String codigo, UUID facultadId, EstadoRegistro estado) {
        String nombreFiltro = Textos.terminoDeBusqueda(nombre);
        String codigoFiltro = Textos.terminoDeBusqueda(codigo);
        // El catálogo de programas es pequeño, así que los filtros se aplican en memoria.
        return programaRepository.findAll(Sort.by("nombre")).stream()
                .filter(p -> nombreFiltro == null || Textos.normalizarBusqueda(p.getNombre()).contains(nombreFiltro))
                .filter(p -> codigoFiltro == null || Textos.normalizarBusqueda(p.getCodigo()).contains(codigoFiltro))
                .filter(p -> facultadId == null
                        || (p.getFacultad() != null && facultadId.equals(p.getFacultad().getId())))
                .filter(p -> estado == null || estado == p.getEstado())
                .map(programaMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramaResponseDTO consultarPorId(UUID id) {
        return programaMapper.toResponseDTO(buscarOLanzar(id));
    }

    @Override
    @Transactional
    public ProgramaResponseDTO registrar(ProgramaRequestDTO dto) {
        String nombre = dto.nombre().trim();
        String codigo = dto.codigo().trim().toUpperCase(Locale.ROOT);

        Facultad facultad = buscarFacultadActiva(dto.facultadId());
        if (programaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new NombreProgramaDuplicadoException(nombre);
        }
        if (programaRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new CodigoProgramaDuplicadoException(codigo);
        }

        ProgramaAcademico programa = new ProgramaAcademico();
        programa.setNombre(nombre);
        programa.setCodigo(codigo);
        programa.setFacultad(facultad);
        programa.setEstado(EstadoRegistro.ACTIVO);
        return programaMapper.toResponseDTO(programaRepository.save(programa));
    }

    @Override
    @Transactional
    public ProgramaResponseDTO actualizar(UUID id, ProgramaUpdateDTO dto) {
        ProgramaAcademico programa = buscarOLanzar(id);
        String nombre = dto.nombre().trim();
        if (programaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new NombreProgramaDuplicadoException(nombre);
        }
        // Reenviar la facultad actual (p. ej. un formulario que manda todos los campos) no se valida:
        // así un programa que quedó en una facultad ya inactiva se puede seguir renombrando.
        if (dto.facultadId() != null && !dto.facultadId().equals(idFacultad(programa))) {
            programa.setFacultad(buscarFacultadActiva(dto.facultadId()));
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

    private Facultad buscarFacultadActiva(UUID facultadId) {
        Facultad facultad = facultadRepository.findById(facultadId)
                .orElseThrow(() -> new FacultadNoEncontradaException(facultadId));
        if (facultad.getEstado() == EstadoRegistro.INACTIVO) {
            throw new FacultadInactivaException(facultad.getNombre());
        }
        return facultad;
    }

    private static UUID idFacultad(ProgramaAcademico programa) {
        return programa.getFacultad() == null ? null : programa.getFacultad().getId();
    }

    private ProgramaAcademico buscarOLanzar(UUID id) {
        return programaRepository.findById(id)
                .orElseThrow(() -> new ProgramaAcademicoNoEncontradoException(id));
    }
}