package co.edu.uco.sigra.estudiantes.service.impl;

import co.edu.uco.sigra.auth.repository.UsuarioRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import co.edu.uco.sigra.common.util.Correos;
import co.edu.uco.sigra.estudiantes.dto.EstudianteRequestDTO;
import co.edu.uco.sigra.estudiantes.dto.EstudianteResponseDTO;
import co.edu.uco.sigra.estudiantes.entity.Estudiante;
import co.edu.uco.sigra.estudiantes.exception.CorreoEstudianteDuplicadoException;
import co.edu.uco.sigra.estudiantes.exception.DocumentoNoModificableException;
import co.edu.uco.sigra.estudiantes.exception.EstudianteNoEncontradoException;
import co.edu.uco.sigra.estudiantes.exception.EstudianteYaRegistradoException;
import co.edu.uco.sigra.estudiantes.exception.ReferenciaNoEncontradaException;
import co.edu.uco.sigra.estudiantes.mapper.EstudianteMapper;
import co.edu.uco.sigra.estudiantes.repository.EstudianteRepository;
import co.edu.uco.sigra.estudiantes.service.EstudianteService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EstudianteServiceImpl implements EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final EstudianteMapper estudianteMapper;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public EstudianteResponseDTO registrar(EstudianteRequestDTO dto) {
        if (estudianteRepository.existsByTipoDocumento_IdAndNumeroDocumento(dto.tipoDocumentoId(), dto.numeroDocumento())) {
            throw EstudianteYaRegistradoException.porDocumento(dto.numeroDocumento());
        }

        String correo = Correos.normalizar(dto.correoInstitucional());
        if (usuarioRepository.existsByCorreoInstitucionalIgnoreCase(correo)) {
            throw CorreoEstudianteDuplicadoException.porCorreo(correo);
        }

        var tipoDocumento = tipoDocumentoRepository.findById(dto.tipoDocumentoId())
                .orElseThrow(() -> ReferenciaNoEncontradaException.tipoDocumento(dto.tipoDocumentoId()));

        Estudiante estudiante = estudianteMapper.toEntity(dto);
        estudiante.setCorreoInstitucional(correo);
        estudiante.setTipoDocumento(tipoDocumento);

        Estudiante guardado = estudianteRepository.save(estudiante);
        return estudianteMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public List<EstudianteResponseDTO> consultar(String filtro) {
        String criterio = (filtro == null || filtro.isBlank()) ? null : filtro;
        List<Estudiante> estudiantes = (criterio == null)
                ? estudianteRepository.findAll()
                : estudianteRepository.buscarPorNombreODocumento(criterio);
        return estudiantes.stream()
                .map(estudianteMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public EstudianteResponseDTO consultarPorId(UUID id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> EstudianteNoEncontradoException.porId(id));
        return estudianteMapper.toResponseDTO(estudiante);
    }

    @Override
    @Transactional
    public EstudianteResponseDTO modificar(UUID id, EstudianteRequestDTO dto) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> EstudianteNoEncontradoException.porId(id));

        if (!estudiante.getTipoDocumento().getId().equals(dto.tipoDocumentoId())
                || !estudiante.getNumeroDocumento().equals(dto.numeroDocumento())) {
            throw DocumentoNoModificableException.paraEstudiante();
        }

        String correo = Correos.normalizar(dto.correoInstitucional());
        if (!correo.equals(estudiante.getCorreoInstitucional())
                && usuarioRepository.existsByCorreoInstitucionalIgnoreCaseAndIdNot(correo, id)) {
            throw CorreoEstudianteDuplicadoException.porCorreo(correo);
        }

        estudianteMapper.updateEntityFromDTO(dto, estudiante);
        estudiante.setCorreoInstitucional(correo);

        return estudianteMapper.toResponseDTO(estudianteRepository.save(estudiante));
    }

    @Override
    @Transactional
    public void inactivar(UUID id) {
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> EstudianteNoEncontradoException.porId(id));

        estudiante.setEstado(EstadoRegistro.INACTIVO);
        estudianteRepository.save(estudiante);
    }
}
