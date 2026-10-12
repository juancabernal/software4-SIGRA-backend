package co.edu.uco.sigra.common.controller;

import co.edu.uco.sigra.common.dto.TipoDocumentoResponseDTO;
import co.edu.uco.sigra.common.service.TipoDocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tipos-documento")
@RequiredArgsConstructor
public class TipoDocumentoController {

    private final TipoDocumentoService tipoDocumentoService;

    @GetMapping
    //@PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<TipoDocumentoResponseDTO>> consultarTiposDocumento() {
        return ResponseEntity.ok(tipoDocumentoService.consultar());
    }

}