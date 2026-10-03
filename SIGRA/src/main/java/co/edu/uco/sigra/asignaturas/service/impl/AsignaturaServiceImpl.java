package co.edu.uco.sigra.asignaturas.service.impl;

import co.edu.uco.sigra.asignaturas.dto.AsignaturaRequestDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaResponseDTO;
import co.edu.uco.sigra.asignaturas.dto.AsignaturaUpdateDTO;
import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.exception.CodigoAsignaturaDuplicadoException;
import co.edu.uco.sigra.asignaturas.exception.ProgramaInactivoException;
import co.edu.uco.sigra.asignaturas.exception.ProgramaNoEncontradoException;
import co.edu.uco.sigra.asignaturas.mapper.AsignaturaMapper;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.asignaturas.service.AsignaturaService;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import co.edu.uco.sigra.programas.repository.ProgramaAcademicoRepository;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.shared.enums.EstadoRegistro;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsignaturaServiceImpl implements AsignaturaService {

    private final AsignaturaRepository asignaturaRepository;
    private final ProgramaAcademicoRepository programaAcademicoRepository;
    private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    private final AsignaturaMapper asignaturaMapper;

    @Override
    @Transactional
    public AsignaturaResponseDTO registrarAsignatura(AsignaturaRequestDTO dto) {
        String codigo = dto.codigo().trim().toUpperCase(Locale.ROOT);
        if (asignaturaRepository.existsByCodigo(codigo)) {
            throw new CodigoAsignaturaDuplicadoException(codigo);
        }

        ProgramaAcademico programa = programaAcademicoRepository.findById(dto.programaId())
                .orElseThrow(() -> new ProgramaNoEncontradoException(dto.programaId()));
        if (programa.getEstado() != EstadoRegistro.ACTIVO) {
            throw new ProgramaInactivoException(programa.getNombre());
        }

        Asignatura asignatura = new Asignatura(codigo, dto.nombre().trim(), programa);
        return asignaturaMapper.toResponseDTO(asignaturaRepository.saveAndFlush(asignatura), 0);
    }

    @Override
    @Transactional
    public AsignaturaResponseDTO obtenerAsignatura(UUID id) {
        return asignaturaMapper.toResponseDTO(buscar(id), contarRaActivos(id));
    }

    @Override
    @Transactional
    public AsignaturaResponseDTO modificarAsignatura(UUID id, AsignaturaUpdateDTO dto) {
        Asignatura asignatura = buscar(id);
        asignatura.renombrar(dto.nombre().trim());
        return asignaturaMapper.toResponseDTO(asignaturaRepository.save(asignatura), contarRaActivos(id));
    }

    @Override
    @Transactional
    public AsignaturaResponseDTO activar(UUID id) {
        Asignatura asignatura = buscar(id);
        long cantidadRa = contarRaActivos(id);
        asignatura.activar(cantidadRa);
        return asignaturaMapper.toResponseDTO(asignaturaRepository.save(asignatura), cantidadRa);
    }

    @Override
    @Transactional
    public AsignaturaResponseDTO inactivar(UUID id) {
        Asignatura asignatura = buscar(id);
        asignatura.inactivar();
        Asignatura guardada = asignaturaRepository.save(asignatura);
        // Cascada: el SRS (RF-06c) exime del mínimo de RA a la asignatura que se inactiva y esta no se reactiva,
        // así que sus RA activos pasan a INACTIVO en la misma transacción. No se borra nada.
        resultadoAprendizajeRepository.cambiarEstadoPorAsignatura(id, EstadoRegistro.ACTIVO, EstadoRegistro.INACTIVO);
        return asignaturaMapper.toResponseDTO(guardada, contarRaActivos(id));
    }

    private Asignatura buscar(UUID id) {
        return asignaturaRepository.findById(id)
                .orElseThrow(() -> new AsignaturaNoEncontradaException(id));
    }

    private long contarRaActivos(UUID id) {
        return resultadoAprendizajeRepository.countByAsignatura_IdAndEstado(id, EstadoRegistro.ACTIVO);
    }
}
