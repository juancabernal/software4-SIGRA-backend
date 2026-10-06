package co.edu.uco.sigra.asignaturas.entity;

import co.edu.uco.sigra.asignaturas.exception.RangoRaInvalidoException;
import co.edu.uco.sigra.asignaturas.exception.TransicionEstadoInvalidaException;
import co.edu.uco.sigra.programas.entity.ProgramaAcademico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Asignatura")
class AsignaturaTest {

    private ProgramaAcademico programa;
    private Asignatura asignatura;

    @BeforeEach
    void setUp() {
        programa = new ProgramaAcademico();
        programa.setCodigo("ISIS");
        programa.setNombre("Ingeniería de Sistemas");
        asignatura = new Asignatura("IS-401", "Ingeniería de Software IV", programa);
    }

    @Test
    @DisplayName("Una asignatura recién creada queda en BORRADOR")
    void recienCreadaQuedaEnBorrador() {
        assertEquals(EstadoAsignatura.BORRADOR, asignatura.getEstado());
    }

    @ParameterizedTest(name = "{0} RA activos es un rango válido")
    @ValueSource(longs = {5, 6, 7})
    @DisplayName("tieneRangoValidoDeRA es verdadero entre 5 y 7 RA")
    void rangoValido(long cantidad) {
        assertTrue(asignatura.tieneRangoValidoDeRA(cantidad));
    }

    @ParameterizedTest(name = "{0} RA activos es un rango inválido")
    @ValueSource(longs = {0, 4, 8, 20})
    @DisplayName("tieneRangoValidoDeRA es falso fuera del rango de 5 a 7")
    void rangoInvalido(long cantidad) {
        assertFalse(asignatura.tieneRangoValidoDeRA(cantidad));
    }

    @Test
    @DisplayName("Activar con 4 RA falla indicando el mínimo y sigue en BORRADOR")
    void activarConPocosRa() {
        RangoRaInvalidoException ex = assertThrows(RangoRaInvalidoException.class, () -> asignatura.activar(4));

        assertTrue(ex.getMessage().contains("al menos 5"));
        assertEquals(EstadoAsignatura.BORRADOR, asignatura.getEstado());
    }

    @Test
    @DisplayName("Activar con 8 RA falla indicando el máximo y sigue en BORRADOR")
    void activarConDemasiadosRa() {
        RangoRaInvalidoException ex = assertThrows(RangoRaInvalidoException.class, () -> asignatura.activar(8));

        assertTrue(ex.getMessage().contains("máximo 7"));
        assertEquals(EstadoAsignatura.BORRADOR, asignatura.getEstado());
    }

    @ParameterizedTest(name = "Activar con {0} RA la deja ACTIVA")
    @ValueSource(longs = {5, 6, 7})
    @DisplayName("Activar con un rango válido de RA la deja ACTIVA")
    void activarConRangoValido(long cantidad) {
        asignatura.activar(cantidad);

        assertEquals(EstadoAsignatura.ACTIVA, asignatura.getEstado());
    }

    @Test
    @DisplayName("Activar una asignatura ya ACTIVA no está permitido")
    void activarAsignaturaActiva() {
        asignatura.activar(5);

        assertThrows(TransicionEstadoInvalidaException.class, () -> asignatura.activar(5));
    }

    @Test
    @DisplayName("Activar una asignatura INACTIVA (reactivar) con 5 RA la deja ACTIVA")
    void activarAsignaturaInactiva() {
        asignatura.activar(5);
        asignatura.inactivar();

        asignatura.activar(5);

        assertEquals(EstadoAsignatura.ACTIVA, asignatura.getEstado());
    }

    @ParameterizedTest(name = "Reactivar con {0} RA la deja ACTIVA")
    @ValueSource(longs = {5, 6, 7})
    @DisplayName("Reactivar una asignatura INACTIVA con un rango válido de RA la deja ACTIVA")
    void reactivarConRangoValido(long cantidad) {
        asignatura.activar(5);
        asignatura.inactivar();

        asignatura.activar(cantidad);

        assertEquals(EstadoAsignatura.ACTIVA, asignatura.getEstado());
    }

    @Test
    @DisplayName("Reactivar con 4 RA falla indicando el mínimo y sigue INACTIVA")
    void reactivarConPocosRa() {
        asignatura.activar(5);
        asignatura.inactivar();

        RangoRaInvalidoException ex = assertThrows(RangoRaInvalidoException.class, () -> asignatura.activar(4));

        assertTrue(ex.getMessage().contains("al menos 5"));
        assertEquals(EstadoAsignatura.INACTIVA, asignatura.getEstado());
    }

    @Test
    @DisplayName("Reactivar con 8 RA falla indicando el máximo y sigue INACTIVA")
    void reactivarConDemasiadosRa() {
        asignatura.activar(5);
        asignatura.inactivar();

        RangoRaInvalidoException ex = assertThrows(RangoRaInvalidoException.class, () -> asignatura.activar(8));

        assertTrue(ex.getMessage().contains("máximo 7"));
        assertEquals(EstadoAsignatura.INACTIVA, asignatura.getEstado());
    }

    @Test
    @DisplayName("Inactivar una asignatura ACTIVA la deja INACTIVA y conserva código y nombre")
    void inactivarAsignaturaActiva() {
        asignatura.activar(5);

        asignatura.inactivar();

        assertEquals(EstadoAsignatura.INACTIVA, asignatura.getEstado());
        assertEquals("IS-401", asignatura.getCodigo());
        assertEquals("Ingeniería de Software IV", asignatura.getNombre());
    }

    @Test
    @DisplayName("Inactivar una asignatura en BORRADOR no está permitido")
    void inactivarAsignaturaEnBorrador() {
        assertThrows(TransicionEstadoInvalidaException.class, () -> asignatura.inactivar());
        assertEquals(EstadoAsignatura.BORRADOR, asignatura.getEstado());
    }

    @Test
    @DisplayName("Renombrar cambia el nombre y deja intactos el código y el programa")
    void renombrar() {
        asignatura.renombrar("Arquitectura de Software");

        assertEquals("Arquitectura de Software", asignatura.getNombre());
        assertEquals("IS-401", asignatura.getCodigo());
        assertSame(programa, asignatura.getPrograma());
    }
}
