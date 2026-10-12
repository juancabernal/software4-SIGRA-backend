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

        profesor.normalizarCorreoInstitucional();

        assertThat(profesor.getCorreoInstitucional()).isEqualTo("profesor.valido@uco.net.co");
    }

    @Test
    void correoNuloNoSeConvierteSilenciosamenteEnOtroValor() {
        Usuario profesor = new Profesor();
        profesor.setCorreoInstitucional(null);

        profesor.normalizarCorreoInstitucional();

        assertThat(profesor.getCorreoInstitucional()).isNull();
    }

    @Test
    void normalizacionSeEjecutaEnPersistYUpdate() throws NoSuchMethodException {
        var metodo = Usuario.class.getDeclaredMethod("normalizarCorreoInstitucional");

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
