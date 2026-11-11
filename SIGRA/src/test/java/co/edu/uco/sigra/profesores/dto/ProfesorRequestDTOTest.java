package co.edu.uco.sigra.profesores.dto;

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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTO de solicitud de profesor")
class ProfesorRequestDTOTest {

    private static final UUID TIPO_DOCUMENTO_ID = UUID.randomUUID();
    private static final String NUMERO_DOCUMENTO = "123456";
    private static final String PRIMER_NOMBRE = "Ana";
    private static final String PRIMER_APELLIDO = "Gómez";
    private static final String SEGUNDO_APELLIDO = "López";
    private static final String CORREO = "ana.gomez@uco.net.co";

    private static final String MENSAJE_NUMERO_FORMATO =
            "El número de documento debe tener entre 6 y 10 dígitos numéricos";
    private static final String MENSAJE_CORREO_FORMATO = "El correo debe tener formato usuario@uco.net.co";

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

    private static Set<ConstraintViolation<ProfesorRequestDTO>> validar(
            UUID tipo, String numero, String nombre, String correo) {
        return validator.validate(new ProfesorRequestDTO(
                tipo, numero, nombre, null, PRIMER_APELLIDO, SEGUNDO_APELLIDO, correo));
    }

    private static Set<ConstraintViolation<ProfesorRequestDTO>> validarPartes(
            String primerNombre, String segundoNombre, String primerApellido, String segundoApellido) {
        return validator.validate(new ProfesorRequestDTO(
                TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, primerNombre, segundoNombre, primerApellido,
                segundoApellido, CORREO));
    }

    private static boolean hayViolacionEn(Set<ConstraintViolation<ProfesorRequestDTO>> violaciones, String campo) {
        return violaciones.stream().anyMatch(v -> v.getPropertyPath().toString().equals(campo));
    }

    @Test
    @DisplayName("con todos los datos válidos no genera violaciones")
    void datosValidos() {
        assertThat(validar(TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, CORREO)).isEmpty();
    }

    @Nested
    @DisplayName("Tipo de documento")
    class TipoDocumentoId {

        @Test
        @DisplayName("en null genera exactamente una violación en tipoDocumentoId")
        void nulo() {
            var violaciones = validar(null, NUMERO_DOCUMENTO, PRIMER_NOMBRE, CORREO);

            assertThat(violaciones).hasSize(1);
            assertThat(violaciones.iterator().next().getPropertyPath()).hasToString("tipoDocumentoId");
        }
    }

    @Nested
    @DisplayName("Número de documento")
    class NumeroDocumento {

        @ParameterizedTest(name = "\"{0}\" es válido")
        @ValueSource(strings = {"123456", "1234567890"})
        @DisplayName("acepta de 6 a 10 dígitos (límites incluidos)")
        void aceptaLimites(String numero) {
            assertThat(validar(TIPO_DOCUMENTO_ID, numero, PRIMER_NOMBRE, CORREO)).isEmpty();
        }

        @ParameterizedTest(name = "\"{0}\" se rechaza con el mensaje de formato")
        @ValueSource(strings = {"12345", "12345678901", "ABC12345", "12 34 56", "123.456"})
        @DisplayName("rechaza menos de 6, más de 10, letras, espacios y símbolos")
        void rechazaFormatosInvalidos(String numero) {
            var violaciones = validar(TIPO_DOCUMENTO_ID, numero, PRIMER_NOMBRE, CORREO);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<ProfesorRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("numeroDocumento");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_NUMERO_FORMATO);
        }

        @ParameterizedTest(name = "[{0}] se rechaza")
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("rechaza nulo, vacío y solo espacios")
        void rechazaNuloOVacio(String numero) {
            var violaciones = validar(TIPO_DOCUMENTO_ID, numero, PRIMER_NOMBRE, CORREO);

            assertThat(hayViolacionEn(violaciones, "numeroDocumento")).isTrue();
        }
    }

    @Nested
    @DisplayName("Partes del nombre (RF35)")
    class PartesDelNombre {

        @ParameterizedTest(name = "primer nombre [{0}] se rechaza")
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("primer nombre rechaza nulo, vacío y solo espacios")
        void primerNombreRechazaNuloOVacio(String nombre) {
            var violaciones = validar(TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, nombre, CORREO);

            assertThat(violaciones).isNotEmpty();
            assertThat(violaciones).allSatisfy(v -> assertThat(v.getPropertyPath()).hasToString("primerNombre"));
        }

        @ParameterizedTest(name = "\"{0}\" se rechaza (espacios, dígitos o símbolos)")
        @ValueSource(strings = {"Ana María", "Ana1", "Ana-María", " Ana", "Ana "})
        @DisplayName("primer nombre solo admite letras sin espacios")
        void primerNombreFormatoInvalido(String nombre) {
            var violaciones = validar(TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, nombre, CORREO);

            assertThat(violaciones).hasSize(1);
            assertThat(violaciones.iterator().next().getPropertyPath()).hasToString("primerNombre");
        }

        @Test
        @DisplayName("segundo nombre es opcional")
        void segundoNombreOpcional() {
            assertThat(validarPartes("Ana", null, "Gómez", "López")).isEmpty();
            assertThat(validarPartes("Ana", "María", "Gómez", "López")).isEmpty();
        }

        @ParameterizedTest(name = "segundo nombre \"{0}\" se rechaza")
        @ValueSource(strings = {"María José", "Mar1a", ""})
        @DisplayName("segundo nombre informado debe ser una sola palabra de letras")
        void segundoNombreInvalido(String segundo) {
            var violaciones = validarPartes("Ana", segundo, "Gómez", "López");

            assertThat(violaciones).hasSize(1);
            assertThat(violaciones.iterator().next().getPropertyPath()).hasToString("segundoNombre");
        }

        @ParameterizedTest(name = "primer apellido [{0}] se rechaza")
        @NullSource
        @ValueSource(strings = {"", "   ", "De La Cruz", "Gómez2"})
        @DisplayName("primer apellido es obligatorio y sin espacios")
        void primerApellidoInvalido(String apellido) {
            var violaciones = validarPartes("Ana", null, apellido, "López");

            assertThat(violaciones).isNotEmpty();
            assertThat(violaciones).allSatisfy(v -> assertThat(v.getPropertyPath()).hasToString("primerApellido"));
        }

        @ParameterizedTest(name = "segundo apellido [{0}] se rechaza")
        @NullSource
        @ValueSource(strings = {"", "   ", "Del Valle", "López2"})
        @DisplayName("segundo apellido es obligatorio y sin espacios")
        void segundoApellidoInvalido(String apellido) {
            var violaciones = validarPartes("Ana", null, "Gómez", apellido);

            assertThat(violaciones).isNotEmpty();
            assertThat(violaciones).allSatisfy(v -> assertThat(v.getPropertyPath()).hasToString("segundoApellido"));
        }
    }
    @Nested
    @DisplayName("Correo institucional")
    class CorreoInstitucional {

        @ParameterizedTest(name = "\"{0}\" es válido")
        @ValueSource(strings = {"ana@uco.net.co", "ana.gomez@uco.net.co", "  Ana-Gomez@UCO.NET.CO  ", "a_b@uco.net.co"})
        @DisplayName("acepta el dominio uco.net.co sin distinguir mayúsculas ni espacios laterales")
        void aceptaCorreosValidos(String correo) {
            assertThat(validar(TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, correo)).isEmpty();
        }

        @ParameterizedTest(name = "\"{0}\" se rechaza con el mensaje de formato")
        @ValueSource(strings = {"ana@gmail.com", "correo-invalido", "ana@uco.net.co.evil.com", "@uco.net.co",
                "ana gomez@uco.net.co"})
        @DisplayName("rechaza dominios externos y formatos inválidos")
        void rechazaFormatosInvalidos(String correo) {
            var violaciones = validar(TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, correo);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<ProfesorRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("correoInstitucional");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_CORREO_FORMATO);
        }

        @ParameterizedTest(name = "[{0}] se rechaza")
        @NullSource
        @ValueSource(strings = {"", "   "})
        @DisplayName("rechaza nulo, vacío y solo espacios")
        void rechazaNuloOVacio(String correo) {
            var violaciones = validar(TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, correo);

            assertThat(hayViolacionEn(violaciones, "correoInstitucional")).isTrue();
        }
    }
}