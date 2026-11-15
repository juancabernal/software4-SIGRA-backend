package co.edu.uco.sigra.estudiantes.service;

import co.edu.uco.sigra.estudiantes.dto.MiMateriaDTO;
import co.edu.uco.sigra.estudiantes.dto.MiMateriaDetalleDTO;

import java.util.List;
import java.util.UUID;

public interface MiProcesoService {

    List<MiMateriaDTO> listarMaterias();

    MiMateriaDetalleDTO consultarMateria(UUID matriculaId);

    List<MiMateriaDetalleDTO> consultarProgreso();

    List<MiMateriaDTO> consultarHistorial();
}
