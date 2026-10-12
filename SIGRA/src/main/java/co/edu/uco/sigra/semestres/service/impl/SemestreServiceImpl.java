package co.edu.uco.sigra.semestres.service.impl;

import co.edu.uco.sigra.semestres.config.SemestreConfig;
import co.edu.uco.sigra.semestres.dto.SemestreFechaFinRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreRequestDTO;
import co.edu.uco.sigra.semestres.dto.SemestreResponseDTO;
import co.edu.uco.sigra.semestres.entity.EstadoSemestre;
import co.edu.uco.sigra.semestres.entity.Semestre;
import co.edu.uco.sigra.semestres.exception.SemestreDuplicadoException;
import co.edu.uco.sigra.semestres.exception.SemestreNoEncontradoException;
import co.edu.uco.sigra.semestres.exception.SemestreReglaNegocioException;
import co.edu.uco.sigra.semestres.exception.SemestreSolapadoException;
import co.edu.uco.sigra.semestres.mapper.SemestreMapper;
import co.edu.uco.sigra.semestres.repository.SemestreRepository;
import co.edu.uco.sigra.semestres.service.SemestreService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class SemestreServiceImpl implements SemestreService {

    private final SemestreRepository semestreRepository;
    private final SemestreMapper semestreMapper;
    private final Clock reloj;

    // Constructor explícito (no @RequiredArgsConstructor) porque Lombok no copia @Qualifier.
    public SemestreServiceImpl(SemestreRepository semestreRepository,
                               SemestreMapper semestreMapper,
                               @Qualifier(SemestreConfig.RELOJ_SEMESTRES) Clock reloj) {
        this.semestreRepository = semestreRepository;
        this.semestreMapper = semestreMapper;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public SemestreResponseDTO crear(SemestreRequestDTO dto) {
        LocalDate hoy = LocalDate.now(reloj);

        if (!dto.fechaFin().isAfter(dto.fechaInicio())) {
            throw new SemestreReglaNegocioException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
        if (dto.fechaInicio().isBefore(hoy)) {
            throw new SemestreReglaNegocioException("La fecha de inicio debe ser hoy o una fecha futura");
        }
        int anioCodigo = Integer.parseInt(dto.codigo().substring(0, 4));
        if (anioCodigo != dto.fechaInicio().getYear()) {
            throw new SemestreReglaNegocioException(
                    "El año del código debe coincidir con el año de la fecha de inicio");
        }
        if (semestreRepository.existsByCodigo(dto.codigo())) {
            throw new SemestreDuplicadoException(dto.codigo());
        }
        if (semestreRepository.existeSolapamiento(dto.fechaInicio(), dto.fechaFin())) {
            throw new SemestreSolapadoException();
        }

        Semestre semestre = semestreMapper.toEntity(dto);
        semestre.setEstado(calcularEstado(semestre, hoy));
        return semestreMapper.toResponseDTO(semestreRepository.save(semestre));
    }

    @Override
    @Transactional
    public List<SemestreResponseDTO> listar() {
        LocalDate hoy = LocalDate.now(reloj);
        return semestreRepository.findAllByOrderByFechaInicioDesc().stream()
                .map(s -> actualizarEstadoSiCambio(s, hoy))
                .map(semestreMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public SemestreResponseDTO consultarPorCodigo(String codigo) {
        Semestre semestre = buscarPorCodigo(codigo);
        return semestreMapper.toResponseDTO(actualizarEstadoSiCambio(semestre, LocalDate.now(reloj)));
    }

    @Override
    @Transactional
    public SemestreResponseDTO extenderFechaFin(String codigo, SemestreFechaFinRequestDTO dto) {
        LocalDate hoy = LocalDate.now(reloj);
        Semestre semestre = buscarPorCodigo(codigo);

        if (hoy.isAfter(semestre.getFechaFin())) {
            throw new SemestreReglaNegocioException("No se puede modificar un semestre que ya terminó");
        }
        if (!dto.fechaFin().isAfter(semestre.getFechaFin())) {
            throw new SemestreReglaNegocioException(
                    "La nueva fecha de fin debe ser posterior a la actual; solo se permite extender el semestre");
        }
        if (semestreRepository.existeSolapamientoExcluyendo(semestre.getFechaInicio(), dto.fechaFin(), semestre.getId())) {
            throw new SemestreSolapadoException();
        }

        semestre.setFechaFin(dto.fechaFin());
        semestre.setEstado(calcularEstado(semestre, hoy));
        return semestreMapper.toResponseDTO(semestreRepository.save(semestre));
    }

    private Semestre buscarPorCodigo(String codigo) {
        return semestreRepository.findByCodigo(codigo)
                .orElseThrow(() -> new SemestreNoEncontradoException(codigo));
    }

    /** Recalcula el estado por fechas y lo persiste solo si cambió (decisión aprobada: sin proceso programado). */
    private Semestre actualizarEstadoSiCambio(Semestre semestre, LocalDate hoy) {
        EstadoSemestre estadoActual = calcularEstado(semestre, hoy);
        if (semestre.getEstado() != estadoActual) {
            semestre.setEstado(estadoActual);
            return semestreRepository.save(semestre);
        }
        return semestre;
    }

    private EstadoSemestre calcularEstado(Semestre semestre, LocalDate hoy) {
        boolean dentroDelRango = !hoy.isBefore(semestre.getFechaInicio()) && !hoy.isAfter(semestre.getFechaFin());
        return dentroDelRango ? EstadoSemestre.ACTIVO : EstadoSemestre.INACTIVO;
    }
}
