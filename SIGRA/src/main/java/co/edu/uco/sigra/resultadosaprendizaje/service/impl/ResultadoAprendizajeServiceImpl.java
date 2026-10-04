package co.edu.uco.sigra.resultadosaprendizaje.service.impl;

import co.edu.uco.sigra.asignaturas.entity.Asignatura;
import co.edu.uco.sigra.asignaturas.entity.EstadoAsignatura;
import co.edu.uco.sigra.asignaturas.exception.AsignaturaNoEncontradaException;
import co.edu.uco.sigra.asignaturas.repository.AsignaturaRepository;
import co.edu.uco.sigra.common.enums.EstadoRegistro;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeActualizarRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeCrearRequestDTO;
import co.edu.uco.sigra.resultadosaprendizaje.dto.ResultadoAprendizajeResponseDTO;
import co.edu.uco.sigra.resultadosaprendizaje.entity.ResultadoAprendizaje;
import co.edu.uco.sigra.resultadosaprendizaje.exception.CodigoResultadoAprendizajeDuplicadoException;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeNoEncontradoException;
import co.edu.uco.sigra.resultadosaprendizaje.exception.ResultadoAprendizajeReglaNegocioException;
import co.edu.uco.sigra.resultadosaprendizaje.mapper.ResultadoAprendizajeMapper;
import co.edu.uco.sigra.resultadosaprendizaje.repository.ResultadoAprendizajeRepository;
import co.edu.uco.sigra.resultadosaprendizaje.service.ResultadoAprendizajeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class ResultadoAprendizajeServiceImpl implements ResultadoAprendizajeService {

    private final ResultadoAprendizajeRepository resultadoAprendizajeRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final ResultadoAprendizajeMapper resultadoAprendizajeMapper;


    @Override
    @Transactional
    public ResultadoAprendizajeResponseDTO registrar(UUID asignaturaId, ResultadoAprendizajeCrearRequestDTO dto) {
        Asignatura asignatura = buscarAsignatura(asignaturaId);
        if (asignatura.getEstado() == EstadoAsignatura.INACTIVA) {
            throw ResultadoAprendizajeReglaNegocioException.asignaturaInactiva();
        }

        String codigo = normalizarCodigo(dto.codigo());
        if (resultadoAprendizajeRepository.existsByAsignatura_IdAndCodigo(asignaturaId, codigo)) {
            throw new CodigoResultadoAprendizajeDuplicadoException(codigo);
        }
        if (contarActivos(asignaturaId) >= Asignatura.MAX_RA_ACTIVOS) {
            throw ResultadoAprendizajeReglaNegocioException.maximoActivosAlcanzado(Asignatura.MAX_RA_ACTIVOS);
        }

        ResultadoAprendizaje resultado = resultadoAprendizajeMapper.toEntity(dto);
        resultado.setAsignatura(asignatura);
        resultado.setCodigo(codigo);
        resultado.setDescripcion(dto.descripcion().trim());
        resultado.setEstado(EstadoRegistro.ACTIVO);

        return resultadoAprendizajeMapper.toResponseDTO(resultadoAprendizajeRepository.save(resultado));
    }


    @Override
    @Transactional
    public List<ResultadoAprendizajeResponseDTO> consultarPorAsignatura(UUID asignaturaId, EstadoRegistro estado) {
        // Se valida que exista para distinguir "asignatura inexistente" (404) de "asignatura sin RA" (lista vacía).
        buscarAsignatura(asignaturaId);

        List<ResultadoAprendizaje> resultados = (estado == null)
                ? resultadoAprendizajeRepository.findByAsignatura_IdOrderByCodigoAsc(asignaturaId)
                : resultadoAprendizajeRepository.findByAsignatura_IdAndEstadoOrderByCodigoAsc(asignaturaId, estado);
        return resultadoAprendizajeMapper.toResponseDTOList(resultados);
    }

    @Override
    @Transactional
    public List<ResultadoAprendizajeResponseDTO> consultarTodos(EstadoRegistro estado) {
        List<ResultadoAprendizaje> resultados = (estado == null)
                ? resultadoAprendizajeRepository.findAll(Sort.by("codigo"))
                : resultadoAprendizajeRepository.findByEstadoOrderByCodigoAsc(estado);
        return resultadoAprendizajeMapper.toResponseDTOList(resultados);
    }

    @Override
    @Transactional
    public ResultadoAprendizajeResponseDTO consultarPorId(UUID id) {
        return resultadoAprendizajeMapper.toResponseDTO(buscarResultado(id));
    }


    @Override
    @Transactional
    public ResultadoAprendizajeResponseDTO actualizarDescripcion(UUID id, ResultadoAprendizajeActualizarRequestDTO dto) {
        ResultadoAprendizaje resultado = buscarResultado(id);
        if (resultado.getEstado() == EstadoRegistro.INACTIVO) {
            throw ResultadoAprendizajeReglaNegocioException.descripcionDeInactivo(resultado.getCodigo());
        }

        resultado.setDescripcion(dto.descripcion().trim());
        return resultadoAprendizajeMapper.toResponseDTO(resultadoAprendizajeRepository.save(resultado));
    }


    @Override
    @Transactional
    public ResultadoAprendizajeResponseDTO inactivar(UUID id) {
        ResultadoAprendizaje resultado = buscarResultado(id);
        if (resultado.getEstado() == EstadoRegistro.INACTIVO) {
            throw ResultadoAprendizajeReglaNegocioException.yaInactivo(resultado.getCodigo());
        }

        Asignatura asignatura = resultado.getAsignatura();
        // El conteo incluye a este RA: si hay MIN o menos, al inactivarlo quedaría por debajo del mínimo.
        if (asignatura.getEstado() == EstadoAsignatura.ACTIVA
                && contarActivos(asignatura.getId()) <= Asignatura.MIN_RA_ACTIVOS) {
            throw ResultadoAprendizajeReglaNegocioException.minimoActivosRequerido(Asignatura.MIN_RA_ACTIVOS);
        }

        resultado.setEstado(EstadoRegistro.INACTIVO);
        return resultadoAprendizajeMapper.toResponseDTO(resultadoAprendizajeRepository.save(resultado));
    }

    @Override
    @Transactional
    public ResultadoAprendizajeResponseDTO reactivar(UUID id) {
        ResultadoAprendizaje resultado = buscarResultado(id);
        if (resultado.getEstado() == EstadoRegistro.ACTIVO) {
            throw ResultadoAprendizajeReglaNegocioException.yaActivo(resultado.getCodigo());
        }

        Asignatura asignatura = resultado.getAsignatura();
        if (asignatura.getEstado() == EstadoAsignatura.INACTIVA) {
            throw ResultadoAprendizajeReglaNegocioException.asignaturaInactiva();
        }
        if (contarActivos(asignatura.getId()) >= Asignatura.MAX_RA_ACTIVOS) {
            throw ResultadoAprendizajeReglaNegocioException.maximoActivosAlcanzado(Asignatura.MAX_RA_ACTIVOS);
        }

        resultado.setEstado(EstadoRegistro.ACTIVO);
        return resultadoAprendizajeMapper.toResponseDTO(resultadoAprendizajeRepository.save(resultado));
    }


    private ResultadoAprendizaje buscarResultado(UUID id) {
        return resultadoAprendizajeRepository.findById(id)
                .orElseThrow(() -> new ResultadoAprendizajeNoEncontradoException(id));
    }

    private Asignatura buscarAsignatura(UUID asignaturaId) {
        return asignaturaRepository.findById(asignaturaId)
                .orElseThrow(() -> new AsignaturaNoEncontradaException(asignaturaId));
    }

    private long contarActivos(UUID asignaturaId) {
        return resultadoAprendizajeRepository.countByAsignatura_IdAndEstado(asignaturaId, EstadoRegistro.ACTIVO);
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }
}
