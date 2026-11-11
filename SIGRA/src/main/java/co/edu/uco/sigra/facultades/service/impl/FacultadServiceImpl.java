package co.edu.uco.sigra.facultades.service.impl;

import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.util.Textos;
import co.edu.uco.sigra.facultades.dto.FacultadRequestDTO;
import co.edu.uco.sigra.facultades.dto.FacultadResponseDTO;
import co.edu.uco.sigra.facultades.entity.Facultad;
import co.edu.uco.sigra.facultades.exception.FacultadConProgramasActivosException;
import co.edu.uco.sigra.facultades.exception.FacultadNoEncontradaException;
import co.edu.uco.sigra.facultades.exception.NombreFacultadDuplicadoException;
import co.edu.uco.sigra.facultades.mapper.FacultadMapper;
import co.edu.uco.sigra.facultades.repository.FacultadRepository;
import co.edu.uco.sigra.facultades.service.FacultadService;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FacultadServiceImpl implements FacultadService {

    private final FacultadRepository facultadRepository;
    private final ProgramaAcademicoRepository programaRepository;
    private final FacultadMapper facultadMapper;

    @Override
    @Transactional(readOnly = true)
    public List<FacultadResponseDTO> consultar(String nombre, EstadoRegistro estado) {
        String termino = Textos.terminoDeBusqueda(nombre);
        // El catálogo de facultades es pequeño, así que los filtros se aplican en memoria.
        return facultadRepository.findAll(Sort.by("nombre")).stream()
                .filter(f -> termino == null || Textos.normalizarBusqueda(f.getNombre()).contains(termino))
                .filter(f -> estado == null || estado == f.getEstado())
                .map(facultadMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FacultadResponseDTO consultarPorId(UUID id) {
        return facultadMapper.toResponseDTO(buscarOLanzar(id));
    }

    @Override
    @Transactional
    public FacultadResponseDTO registrar(FacultadRequestDTO dto) {
        String nombre = dto.nombre().trim();
        if (facultadRepository.existsByNombreIgnoreCase(nombre)) {
            throw new NombreFacultadDuplicadoException(nombre);
        }
        Facultad facultad = new Facultad();
        facultad.setNombre(nombre);
        facultad.setEstado(EstadoRegistro.ACTIVO);
        return facultadMapper.toResponseDTO(facultadRepository.save(facultad));
    }

    @Override
    @Transactional
    public FacultadResponseDTO actualizarNombre(UUID id, FacultadRequestDTO dto) {
        Facultad facultad = buscarOLanzar(id);
        String nombre = dto.nombre().trim();
        if (facultadRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new NombreFacultadDuplicadoException(nombre);
        }
        facultad.setNombre(nombre);
        return facultadMapper.toResponseDTO(facultadRepository.save(facultad));
    }

    @Override
    @Transactional
    public FacultadResponseDTO inactivar(UUID id) {
        Facultad facultad = buscarOLanzar(id);
        if (facultad.getEstado() == EstadoRegistro.INACTIVO) {
            return facultadMapper.toResponseDTO(facultad);
        }
        if (programaRepository.existsByFacultad_IdAndEstado(id, EstadoRegistro.ACTIVO)) {
            throw new FacultadConProgramasActivosException(facultad.getNombre());
        }
        // Eliminación lógica: el registro se conserva para no perder trazabilidad.
        facultad.setEstado(EstadoRegistro.INACTIVO);
        return facultadMapper.toResponseDTO(facultadRepository.save(facultad));
    }

    private Facultad buscarOLanzar(UUID id) {
        return facultadRepository.findById(id)
                .orElseThrow(() -> new FacultadNoEncontradaException(id));
    }
}