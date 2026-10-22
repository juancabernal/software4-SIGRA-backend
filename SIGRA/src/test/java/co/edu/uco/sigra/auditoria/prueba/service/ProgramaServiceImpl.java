package co.edu.uco.sigra.auditoria.prueba.service;

import java.util.UUID;

public class ProgramaServiceImpl implements ProgramaService {

    public static final UUID ID_QUE_FALLA = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public Resultado inactivar(UUID id) {
        if (ID_QUE_FALLA.equals(id)) {
            throw new IllegalStateException("regla de negocio incumplida");
        }
        return new Resultado(id, "INACTIVO");
    }

    @Override
    public void desvincular(UUID id, String motivo) {
        // sin resultado: el aspecto igual debe auditar
    }

    @Override
    public Resultado consultar(UUID id) {
        return new Resultado(id, "ACTIVO");
    }
}