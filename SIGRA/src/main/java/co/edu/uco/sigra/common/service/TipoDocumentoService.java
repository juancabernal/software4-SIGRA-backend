package co.edu.uco.sigra.common.service;

import co.edu.uco.sigra.common.dto.TipoDocumentoResponseDTO;
import co.edu.uco.sigra.common.repository.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoDocumentoService {

    private final TipoDocumentoRepository tipoDocumentoRepository;

    public List<TipoDocumentoResponseDTO> consultar() {
        return tipoDocumentoRepository.findAll(Sort.by(Sort.Direction.ASC, "nombre")).stream()
                .map(tipo -> new TipoDocumentoResponseDTO(tipo.getId(), tipo.getNombre()))
                .toList();
    }
}