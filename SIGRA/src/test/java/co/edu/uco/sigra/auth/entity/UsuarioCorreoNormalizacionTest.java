package co.edu.uco.sigra.auth.entity;

import co.edu.uco.sigra.profesores.entity.Profesor;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// UNIT: el callback de persistencia normaliza el correo. La persistencia real en PostgreSQL
// se verifica en las pruebas de integración (SigraContextIT y el flujo Bruno).
class UsuarioCorreoNormalizacionTest {

    @Test
    void correoSeNormalizaConEspaciosYMayusculasAntesDePersistir() {
        Usuario profesor = new Profesor();
        profesor.setCorreoInstitucional("  Profesor.Valido@UCO.NET.CO ");

        profesor.normalizarCamposUsuario();

        assertThat(profesor.getCorreoInstitucional()).isEqualTo("profesor.valido@uco.net.co");
    }

    @Test
    void correoNuloNoSeConvierteSilenciosamenteEnOtroValor() {
        Usuario profesor = new Profesor();
        profesor.setCorreoInstitucional(null);

        profesor.normalizarCamposUsuario();

        assertThat(profesor.getCorreoInstitucional()).isNull();
    }

    @Test
    void partesDelNombreSeRecortanYSegundoNombreVacioPasaANulo() {
        Usuario profesor = new Profesor();
        profesor.setPrimerNombre(" Ana ");
        profesor.setSegundoNombre("   ");
        profesor.setPrimerApellido(" Gómez");
        profesor.setSegundoApellido("López ");

        profesor.normalizarCamposUsuario();

        assertThat(profesor.getPrimerNombre()).isEqualTo("Ana");
        assertThat(profesor.getSegundoNombre()).isNull();
        assertThat(profesor.getPrimerApellido()).isEqualTo("Gómez");
        assertThat(profesor.getSegundoApellido()).isEqualTo("López");
        assertThat(profesor.getNombreCompleto()).isEqualTo("Ana Gómez López");
    }

    @Test
    void normalizacionSeEjecutaEnPersistYUpdate() throws NoSuchMethodException {
        var metodo = Usuario.class.getDeclaredMethod("normalizarCamposUsuario");

        assertThat(metodo.isAnnotationPresent(PrePersist.class)).isTrue();
        assertThat(metodo.isAnnotationPresent(PreUpdate.class)).isTrue();
    }

    @Test
    void correoInstitucionalSigueSiendoObligatorio() throws NoSuchFieldException {
        Column columna = Usuario.class.getDeclaredField("correoInstitucional").getAnnotation(Column.class);

        assertThat(columna.nullable()).isFalse();
        assertThat(columna.unique()).isTrue();
    }
}
