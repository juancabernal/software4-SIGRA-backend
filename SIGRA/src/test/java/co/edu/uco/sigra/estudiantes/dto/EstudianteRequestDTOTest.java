package co.edu.uco.sigra.estudiantes.dto;

import co.edu.uco.sigra.common.util.NombresPersona;
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
    private static final String PRIMER_NOMBRE = "Juan";
    private static final String SEGUNDO_NOMBRE = "Carlos";
    private static final String PRIMER_APELLIDO = "Pérez";
    private static final String SEGUNDO_APELLIDO = "Gómez";
    private static final String CORREO_INSTITUCIONAL = "juan.perez@uco.net.co";

    private static final String MENSAJE_TIPO_DOCUMENTO_OBLIGATORIO = "El tipo de documento es obligatorio";
    private static final String MENSAJE_NUMERO_DOCUMENTO_OBLIGATORIO = "El número de documento es obligatorio";
    private static final String MENSAJE_NUMERO_DOCUMENTO_FORMATO =
            "El número de documento debe tener entre 6 y 10 dígitos numéricos";
    private static final String MENSAJE_PRIMER_NOMBRE_OBLIGATORIO = "El primer nombre es obligatorio";
    private static final String MENSAJE_PRIMER_NOMBRE_LONGITUD = "El primer nombre debe tener entre 1 y 50 caracteres";
    private static final String MENSAJE_PRIMER_APELLIDO_OBLIGATORIO = "El primer apellido es obligatorio";
    private static final String MENSAJE_SEGUNDO_APELLIDO_OBLIGATORIO = "El segundo apellido es obligatorio";
    private static final String MENSAJE_PARTE_FORMATO = NombresPersona.MENSAJE_PARTE;
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
        return new EstudianteRequestDTO(TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO,
                SEGUNDO_APELLIDO, CORREO_INSTITUCIONAL);
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
                    null, NUMERO_DOCUMENTO, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO,
                SEGUNDO_APELLIDO, CORREO_INSTITUCIONAL);

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
                    TIPO_DOCUMENTO_ID, numeroDocumento, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO, CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            assertThat(violaciones.iterator().next().getPropertyPath()).hasToString("numeroDocumento");
        }

        @Test
        @DisplayName("el mensaje de @Pattern está en español")
        void mensajeFormatoInvalido() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, "12345", PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO, CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            assertThat(violaciones.iterator().next().getMessage()).isEqualTo(MENSAJE_NUMERO_DOCUMENTO_FORMATO);
        }

        @ParameterizedTest(name = "\"{0}\" es un número de documento válido (límite)")
        @ValueSource(strings = {"123456", "1234567890"})
        @DisplayName("con 6 o 10 dígitos (límites del rango válido) no genera violaciones")
        void longitudLimiteValida(String numeroDocumento) {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, numeroDocumento, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO, CORREO_INSTITUCIONAL);

            assertThat(validator.validate(dto)).isEmpty();
        }

        @Test
        @DisplayName("en null genera exactamente una violación por @NotBlank con el mensaje en español")
        void nulo() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, null, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO, CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("numeroDocumento");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_NUMERO_DOCUMENTO_OBLIGATORIO);
        }
    }

    @Nested
    @DisplayName("Partes del nombre (RF35)")
    class PartesDelNombre {

        @Test
        @DisplayName("primer nombre en null genera exactamente una violación por @NotBlank")
        void primerNombreNulo() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, null, SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO,
                    CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("primerNombre");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_PRIMER_NOMBRE_OBLIGATORIO);
        }

        @Test
        @DisplayName("primer nombre vacío genera tres violaciones: @NotBlank, @Size y @Pattern")
        void primerNombreVacio() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, "", SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO,
                    CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(3);
            assertThat(violaciones)
                    .allSatisfy(v -> assertThat(v.getPropertyPath()).hasToString("primerNombre"));
            assertThat(violaciones.stream().map(ConstraintViolation::getMessage))
                    .containsExactlyInAnyOrder(
                            MENSAJE_PRIMER_NOMBRE_OBLIGATORIO,
                            MENSAJE_PRIMER_NOMBRE_LONGITUD,
                            MENSAJE_PARTE_FORMATO);
        }

        @Test
        @DisplayName("primer nombre solo espacios genera dos violaciones: @NotBlank y @Pattern")
        void primerNombreSoloEspacios() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, "   ", SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO,
                    CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(2);
            assertThat(violaciones)
                    .allSatisfy(v -> assertThat(v.getPropertyPath()).hasToString("primerNombre"));
            assertThat(violaciones.stream().map(ConstraintViolation::getMessage))
                    .containsExactlyInAnyOrder(MENSAJE_PRIMER_NOMBRE_OBLIGATORIO, MENSAJE_PARTE_FORMATO);
        }

        @ParameterizedTest(name = "\"{0}\" no es una parte de nombre válida")
        @ValueSource(strings = {
                "Juan123",
                "Juan@",
                "Juan_Perez",
                "Juan-Perez",
                "Juan Carlos",
                " Juan",
                "Juan "
        })
        @DisplayName("con dígitos, símbolos, guiones o espacios genera una violación por @Pattern en primerNombre")
        void primerNombreFormatoInvalido(String valor) {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, valor, SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO,
                    CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("primerNombre");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_PARTE_FORMATO);
        }

        @ParameterizedTest(name = "\"{0}\" es una parte de nombre válida")
        @ValueSource(strings = {"Ana", "José", "Ñustes", "Pérez"})
        @DisplayName("con solo letras (incluye tildes y Ñ) no genera violaciones")
        void formatoValido(String valor) {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, valor, SEGUNDO_NOMBRE, valor, valor,
                    CORREO_INSTITUCIONAL);

            assertThat(validator.validate(dto)).isEmpty();
        }

        @Test
        @DisplayName("primer nombre con más de 50 caracteres genera una violación por @Size")
        void primerNombreMuyLargo() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, "A".repeat(51), SEGUNDO_NOMBRE, PRIMER_APELLIDO,
                    SEGUNDO_APELLIDO, CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("primerNombre");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_PRIMER_NOMBRE_LONGITUD);
        }

        @Test
        @DisplayName("segundo nombre en null es válido (opcional)")
        void segundoNombreNuloEsValido() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, null, PRIMER_APELLIDO, SEGUNDO_APELLIDO,
                    CORREO_INSTITUCIONAL);

            assertThat(validator.validate(dto)).isEmpty();
        }

        @ParameterizedTest(name = "segundo nombre \"{0}\" es inválido")
        @ValueSource(strings = {"Luis Fernando", "Luis1", "Luis-", "", " "})
        @DisplayName("segundo nombre informado con espacios, símbolos o vacío genera violación por @Pattern")
        void segundoNombreInvalido(String valor) {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, valor, PRIMER_APELLIDO, SEGUNDO_APELLIDO,
                    CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("segundoNombre");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_PARTE_FORMATO);
        }

        @Test
        @DisplayName("primer apellido en null genera exactamente una violación por @NotBlank")
        void primerApellidoNulo() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, SEGUNDO_NOMBRE, null, SEGUNDO_APELLIDO,
                    CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("primerApellido");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_PRIMER_APELLIDO_OBLIGATORIO);
        }

        @Test
        @DisplayName("primer apellido con espacio interno genera una violación por @Pattern")
        void primerApellidoConEspacio() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, SEGUNDO_NOMBRE, "De La Cruz",
                    SEGUNDO_APELLIDO, CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("primerApellido");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_PARTE_FORMATO);
        }

        @Test
        @DisplayName("segundo apellido en null genera exactamente una violación por @NotBlank")
        void segundoApellidoNulo() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO, null,
                    CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("segundoApellido");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_SEGUNDO_APELLIDO_OBLIGATORIO);
        }

        @Test
        @DisplayName("segundo apellido con espacio interno genera una violación por @Pattern")
        void segundoApellidoConEspacio() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO,
                    "Del Valle", CORREO_INSTITUCIONAL);

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("segundoApellido");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_PARTE_FORMATO);
        }
    }

    @Nested
    @DisplayName("Correo institucional")
    class CorreoInstitucional {

        @Test
        @DisplayName("en null genera exactamente una violación por @NotBlank con el mensaje en español")
        void nulo() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO, null);

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
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO, "alguien@gmail.com");

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
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO, "Usuario@UCO.NET.CO");

            assertThat(validator.validate(dto)).isEmpty();
        }

        @Test
        @DisplayName("vacío genera dos violaciones: @NotBlank y @Pattern, ambas sobre correoInstitucional")
        void vacio() {
            EstudianteRequestDTO dto = new EstudianteRequestDTO(
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO, "");

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
                    TIPO_DOCUMENTO_ID, NUMERO_DOCUMENTO, PRIMER_NOMBRE, SEGUNDO_NOMBRE, PRIMER_APELLIDO, SEGUNDO_APELLIDO, "@uco.net.co");

            Set<ConstraintViolation<EstudianteRequestDTO>> violaciones = validator.validate(dto);

            assertThat(violaciones).hasSize(1);
            ConstraintViolation<EstudianteRequestDTO> violacion = violaciones.iterator().next();
            assertThat(violacion.getPropertyPath()).hasToString("correoInstitucional");
            assertThat(violacion.getMessage()).isEqualTo(MENSAJE_CORREO_FORMATO);
        }
    }
}
