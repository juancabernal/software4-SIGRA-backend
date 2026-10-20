package co.edu.uco.sigra.estudiantes.dto;

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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DTO de solicitud de estudiante")
class EstudianteRequestDTOTest {

    private static final UUID TIPO_DOCUMENTO_ID = UUID.randomUUID();
    private static final String NUMERO_DOCUMENTO = "123456";
    private static final String NOMBRE_COMPLETO = "Juan Pérez";
    private static final String CORREO_INSTITUCIONAL = "juan.perez@uco.net.co";

    private static final String MENSAJE_TIPO_DOCUMENTO_OBLIGATORIO = "El tipo de documento es obligatorio";
    private static final String MENSAJE_NUMERO_DOCUMENTO_OBLIGATORIO = "El número de documento es obligatorio";
    private static final String MENSAJE_NUMERO_DOCUMENTO_FORMATO =
            "El número de documento debe tener entre 6 y 10 dígitos numéricos";
    private static final String MENSAJE_NOMBRE_COMPLETO_OBLIGATORIO = "El nombre completo es obligatorio";
    private static final String MENSAJE_NOMBRE_COMPLETO_LONGITUD =
            "El nombre completo debe tener entre 3 y 150 caracteres";
    private static final String MENSAJE_NOMBRE_COMPLETO_FORMATO =
            "El nombre completo solo puede contener letras y un único espacio entre cada palabra";
    private static final String MENSAJE_CORREO_OBLIGATORIO = "El correo institucional es obligatorio";
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

    private static EstudianteRequestDTO dtoValido() {
        return new EstudianteRequestDTO(TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO_INSTITUCIONAL);
    }

    @Test
    @DisplayName("con todos los datos válidos no genera violaciones")
    void datosValidos() {
        Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dtoValido());

        assertThat(violaciones).isEmpty();
    }

    @Nested
    @DisplayName("Tipo de documento")
    class TipoDocumentoId {

        @Test
        @DisplayName("en null genera exactamente una violación en tipoDocumentoId con el mensaje en español")
        void nulo() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    null, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("tipoDocumentoId");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_TIPO_DOCUMENTO_OBLIGATORIO);
        }
    }

    @Nested
    @DisplayName("Número de documento")
    class NumeroDocumento {

        @ParameterizedTest(name = "\"{0}\" no es un número de documento válido")
        @ValueSource(strings = {"12A456", "123-456", "12345", "12345678901"})
        @DisplayName("con letras, símbolos o una longitud fuera de 6-10 dígitos genera una violación")
        void formatoInvalido(String numeroDocumento) {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, numeroDocumento, NOMBRE_COMPLETO, CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            assertThat(violaciones.iterator().next().getPropertyPath()).hasToString("numeroDocumento");
        }

        @Test
        @DisplayName("el mensaje de @Pattern está en español")
        void mensajeFormatoInvalido() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, "12345", NOMBRE_COMPLETO, CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            assertThat(violaciones.iterator().next().getMessage()).isEqualTo(MENSAJE_NUMERO_DOCUMENTO_FORMATO);
        }

        @ParameterizedTest(name = "\"{0}\" es un número de documento válido (límite)")
        @ValueSource(strings = {"123456", "1234567890"})
        @DisplayName("con 6 o 10 dígitos (límites del rango válido) no genera violaciones")
        void longitudLimiteValida(String numeroDocumento) {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, numeroDocumento, NOMBRE_COMPLETO, CORREO_INSTITUCIONAL);

            assertThat(validator.validate(dto)).isEmpty();
        }

        @Test
        @DisplayName("en null genera exactamente una violación por @NotBlank con el mensaje en español")
        void nulo() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, null, NOMBRE_COMPLETO, CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("numeroDocumento");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_NUMERO_DOCUMENTO_OBLIGATORIO);
        }
    }

    @Nested
    @DisplayName("Nombre completo")
    class NombreCompleto {

        @Test
        @DisplayName("en null genera exactamente una violación por @NotBlank (@Size y @Pattern no evalúan null)")
        void nulo() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, null, CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("nombreCompleto");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_NOMBRE_COMPLETO_OBLIGATORIO);
        }

        @Test
        @DisplayName("vacío genera tres violaciones: @NotBlank, @Size y @Pattern, las tres sobre nombreCompleto")
        void vacio() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, "", CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(3);
            assertThat(violaciones)
                    .allSatisfy(v -> assertThat(v.getPropertyPath()).hasToString("nombreCompleto"));
            assertThat(violaciones.stream().map(ConstraintViolation::getMessage))
                    .containsExactlyInAnyOrder(
                            MENSAJE_NOMBRE_COMPLETO_OBLIGATORIO,
                            MENSAJE_NOMBRE_COMPLETO_LONGITUD,
                            MENSAJE_NOMBRE_COMPLETO_FORMATO);
        }

        @Test
        @DisplayName("solo espacios genera dos violaciones: @NotBlank y @Pattern, ambas sobre nombreCompleto")
        void soloEspacios() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, "   ", CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(2);
            assertThat(violaciones)
                    .allSatisfy(v -> assertThat(v.getPropertyPath()).hasToString("nombreCompleto"));
            assertThat(violaciones.stream().map(ConstraintViolation::getMessage))
                    .containsExactlyInAnyOrder(
                            MENSAJE_NOMBRE_COMPLETO_OBLIGATORIO, MENSAJE_NOMBRE_COMPLETO_FORMATO);
        }

        @ParameterizedTest(name = "\"{0}\" no es un nombre completo válido")
        @ValueSource(strings = {
                "Juan123",
                "Juan Pérez2",
                "Juan@Perez",
                "Juan_Perez",
                "Juan-Perez",
                "Juan  Pérez",
                " Juan Pérez",
                "Juan Pérez "
        })
        @DisplayName("con dígitos, símbolos, guiones o espacios dobles/al borde genera una violación por @Pattern")
        void formatoInvalido(String nombreCompleto) {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, nombreCompleto, CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("nombreCompleto");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_NOMBRE_COMPLETO_FORMATO);
        }

        @ParameterizedTest(name = "\"{0}\" es un nombre completo válido")
        @ValueSource(strings = {"Ana", "José María Gómez Pérez", "Maria Ñustes"})
        @DisplayName("con solo letras (incluye tildes, Ñ y nombres compuestos) no genera violaciones")
        void formatoValido(String nombreCompleto) {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, nombreCompleto, CORREO_INSTITUCIONAL);

            assertThat(validator.validate(dto)).isEmpty();
        }

        @Test
        @DisplayName("con menos de 3 caracteres genera una violación por @Size")
        void muyCorto() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, "Jo", CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("nombreCompleto");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_NOMBRE_COMPLETO_LONGITUD);
        }

        @Test
        @DisplayName("con más de 150 caracteres genera una violación por @Size")
        void muyLargo() {
            String nombreLargo = "A".repeat(151);
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, nombreLargo, CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("nombreCompleto");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_NOMBRE_COMPLETO_LONGITUD);
        }
    }

    @Nested
    @DisplayName("Correo institucional")
    class CorreoInstitucional {

        @Test
        @DisplayName("en null genera exactamente una violación por @NotBlank con el mensaje en español")
        void nulo() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, null);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("correoInstitucional");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_CORREO_OBLIGATORIO);
        }

        @Test
        @DisplayName("con un dominio distinto a uco.net.co genera una violación con el mensaje en español")
        void dominioDistinto() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, "alguien@gmail.com");

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("correoInstitucional");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_CORREO_FORMATO);
        }

        @Test
        @DisplayName("con el dominio en mayúsculas no genera violaciones (insensible a mayúsculas)")
        void dominioEnMayusculas() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, "Usuario@UCO.NET.CO");

            assertThat(validator.validate(dto)).isEmpty();
        }

        @Test
        @DisplayName("vacío genera dos violaciones: @NotBlank y @Pattern, ambas sobre correoInstitucional")
        void vacio() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, "");

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(2);
            assertThat(violaciones)
                    .allSatisfy(v -> assertThat(v.getPropertyPath()).hasToString("correoInstitucional"));
            assertThat(violaciones.stream().map(ConstraintViolation::getMessage))
                    .containsExactlyInAnyOrder(MENSAJE_CORREO_OBLIGATORIO, MENSAJE_CORREO_FORMATO);
        }

        @Test
        @DisplayName("con dominio correcto pero sin parte local genera una violación por @Pattern")
        void sinParteLocal() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, NOMBRE_COMPLETO, "@uco.net.co");

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("correoInstitucional");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_CORREO_FORMATO);
        }
    }
}
