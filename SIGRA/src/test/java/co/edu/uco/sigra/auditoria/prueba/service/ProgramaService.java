package co.edu.uco.sigra.auditoria.prueba.service;

import java.util.UUID;

public interface ProgramaService {

    record Resultado(UUID id, String estado) {
    }

    Resultado inactivar(UUID id);

    void desvincular(UUID id, String motivo);

    Resultado consultar(UUID id);
}