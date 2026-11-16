package co.edu.uco.sigra.reportes.integracion;

import co.edu.uco.sigra.reportes.service.AccionesMejoraReporteService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import co.edu.uco.sigra.auth.security.UsuarioPrincipal;
import co.edu.uco.sigra.common.enums.RolUsuario;

import static org.assertj.core.api.Assertions.assertThat;

/** Valida el HQL con PostgreSQL sin insertar, modificar ni borrar datos compartidos. */
@SpringBootTest
@Tag("integration")
@Transactional(readOnly = true)
class AccionesMejoraReporteConsultaIT {
    @Autowired private AccionesMejoraReporteService service;
    private final UsuarioPrincipal admin = new UsuarioPrincipal(UUID.randomUUID(), RolUsuario.ADMINISTRADOR);

    @Test
    void ejecutaConsultaRealYFiltraAmbosEstadosSinEscribirDatos() {
        var todas = service.consultar(null, null, null, null, admin);
        var convertidas = service.consultar(null, null, null, true, admin);
        var pendientes = service.consultar(null, null, null, false, admin);
        assertThat(convertidas.filas()).allMatch(f -> f.convertida() && f.evaluacionGeneradaId() != null);
        assertThat(pendientes.filas()).noneMatch(f -> f.convertida());
        assertThat(todas.resumen().total()).isEqualTo(convertidas.resumen().total() + pendientes.resumen().total());
        assertThat(service.consultar(UUID.randomUUID(), null, null, null, admin).filas()).isEmpty();
        assertThat(service.consultar(null, UUID.randomUUID(), null, null, admin).filas()).isEmpty();
        assertThat(service.consultar(null, null, UUID.randomUUID(), null, admin).filas()).isEmpty();
        if (!todas.filas().isEmpty()) {
            var fila = todas.filas().getFirst();
            assertThat(service.consultar(fila.semestreId(), fila.asignaturaId(),
                    fila.resultadoAprendizajeId(), fila.convertida(), admin).filas())
                    .isNotEmpty().allMatch(f -> f.semestreId().equals(fila.semestreId())
                            && f.asignaturaId().equals(fila.asignaturaId())
                            && f.resultadoAprendizajeId().equals(fila.resultadoAprendizajeId())
                            && f.convertida() == fila.convertida());
        }
    }
}
