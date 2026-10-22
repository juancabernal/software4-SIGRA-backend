package co.edu.uco.sigra.auditoria.aspect;

import co.edu.uco.sigra.auditoria.entity.TipoEntidadAuditoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tipo de entidad deducido del servicio")
class TipoEntidadAuditoriaTest {

    static class ResultadoAprendizajeServiceImpl {}
    static class AsignacionDocenteServiceImpl {}
    static class ProgramaAcademicoServiceImpl {}
    static class ModuloNuevoServiceImpl {}

    @Test
    @DisplayName("Convierte el nombre del servicio en la constante de la entidad; si no existe, OTRO")
    void desdeClase() {
        assertThat(TipoEntidadAuditoria.desdeClase(ResultadoAprendizajeServiceImpl.class))
                .isEqualTo(TipoEntidadAuditoria.RESULTADO_APRENDIZAJE);
        assertThat(TipoEntidadAuditoria.desdeClase(AsignacionDocenteServiceImpl.class))
                .isEqualTo(TipoEntidadAuditoria.ASIGNACION_DOCENTE);
        assertThat(TipoEntidadAuditoria.desdeClase(ProgramaAcademicoServiceImpl.class))
                .isEqualTo(TipoEntidadAuditoria.PROGRAMA);
        assertThat(TipoEntidadAuditoria.desdeClase(ModuloNuevoServiceImpl.class))
                .isEqualTo(TipoEntidadAuditoria.OTRO);
    }
}