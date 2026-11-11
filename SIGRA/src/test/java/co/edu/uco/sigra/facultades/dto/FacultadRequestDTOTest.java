package co.edu.uco.sigra.facultades.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTO de solicitud de facultad")
class FacultadRequestDTOTest {

    private static final String MENSAJE_OBLIGATORIO = "El nombre es obligatorio";
    private static final String MENSAJE_LONGITUD = "El nombre no puede superar los 150 caracteres";

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void configurarValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    private static Set<ConstraintViolation<FacultadRequestDTO>> validar(String nombre) {
        return validator.validate(new FacultadRequestDTO(nombre));
    }

    @Test
    @DisplayName("con un nombre válido no genera violaciones")
    void datosValidos() {
        assertThat(validar("Facultad de Ingeniería")).isEmpty();
    }

    @Nested
    @DisplayName("Nombre")
    class Nombre {

        @ParameterizedTest(name = "[{0}] se rechaza con el mensaje de obligatorio")
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("rechaza nulo, vacío y solo espacios")
        void rechazaNuloOVacio(String nombre) {
            var violaciones = validar(nombre);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<FacultadRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("nombre");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_OBLIGATORIO);
        }

        @Test
        @DisplayName("acepta exactamente 150 caracteres (límite incluido)")
        void aceptaLimite() {
            assertThat(validar("A".repeat(150))).isEmpty();
        }

        @Test
        @DisplayName("rechaza 151 caracteres con el mensaje de longitud")
        void rechazaMasDe150() {
            var violaciones = validar("A".repeat(151));

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<FacultadRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("nombre");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_LONGITUD);
        }

        @ParameterizedTest(name = "\"{0}\" es válido")
        @ValueSource(strings = {"F", "Facultad de Ciencias Económicas, Administrativas y Contables", "Ñandú & Cía."})
        @DisplayName("acepta nombres cortos, largos, con tildes y con símbolos")
        void aceptaVariantes(String nombre) {
            assertThat(validar(nombre)).isEmpty();
        }
    }
}
