package co.edu.uco.sigra.asignaturas.dto;

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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Saneamiento y validación de entrada de asignaturas")
class ReglasEntradaTest {

    private static final UUID PROGRAMA = UUID.randomUUID();
    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    private static Set<ConstraintViolation<AsignaturaRequestDTO>> validar(String codigo, String nombre) {
        return validador.validate(new AsignaturaRequestDTO(codigo, nombre, PROGRAMA));
    }

    private static Set<ConstraintViolation<AsignaturaRequestDTO>> violacionesDe(String campo, String codigo, String nombre) {
        Set<ConstraintViolation<AsignaturaRequestDTO>> todas = validar(codigo, nombre);
        todas.removeIf(v -> !v.getPropertyPath().toString().equals(campo));
        return todas;
    }

    @Nested
    @DisplayName("Normalización")
    class Normalizacion {

        @ParameterizedTest(name = "«{0}» se normaliza a «{1}»")
        @CsvSource(delimiter = '|', value = {
                "  mat-401  | MAT-401",
                "isw4       | ISW4",
                "Bru-07     | BRU-07",
        })
        @DisplayName("El código se recorta y pasa a mayúsculas")
        void codigo(String entrada, String esperado) {
            assertThat(new AsignaturaRequestDTO(entrada, "Nombre", PROGRAMA).codigo()).isEqualTo(esperado);
        }

        @Test
        @DisplayName("El nombre reduce espacios, tabs y saltos de línea a un solo espacio y se recorta")
        void nombreEspacios() {
            String nombre = "  Cálculo \t\t Integral \n  II  ";
            assertThat(new AsignaturaRequestDTO("MAT-401", nombre, PROGRAMA).nombre()).isEqualTo("Cálculo Integral II");
        }

        @Test
        @DisplayName("El nombre se normaliza a Unicode NFC (tilde combinada → carácter único)")
        void nombreNfc() {
            String descompuesto = "Programación";
            String nombre = new AsignaturaRequestDTO("PRG-101", descompuesto, PROGRAMA).nombre();
            assertThat(nombre).isEqualTo("Programación").hasSize(12);
        }

        @Test
        @DisplayName("Modificar aplica la misma normalización al nombre")
        void modificar() {
            assertThat(new AsignaturaUpdateDTO("  Bases   de\tDatos ").nombre()).isEqualTo("Bases de Datos");
        }

        @Test
        @DisplayName("El texto de búsqueda se recorta y uno vacío equivale a no filtrar")
        void textoBusqueda() {
            assertThat(new AsignaturaFiltroDTO("  bruno  ", null, null, null, null).texto()).isEqualTo("bruno");
            assertThat(new AsignaturaFiltroDTO("   ", null, null, null, null).texto()).isNull();
        }

        @Test
        @DisplayName("Los valores nulos se conservan nulos para que los valide @NotBlank")
        void nulos() {
            AsignaturaRequestDTO dto = new AsignaturaRequestDTO(null, null, PROGRAMA);
            assertThat(dto.codigo()).isNull();
            assertThat(dto.nombre()).isNull();
        }
    }

    @Nested
    @DisplayName("Código")
    class Codigo {

        @ParameterizedTest(name = "«{0}» es válido")
        @ValueSource(strings = {"ABC", "MAT-401", "IS-401-A", "A1-B2-C3", "ABCDEFGHIJKLMNOPQRST", " mat-401 "})
        @DisplayName("Códigos válidos")
        void validos(String codigo) {
            assertThat(violacionesDe("codigo", codigo, "Nombre válido")).isEmpty();
        }

        @ParameterizedTest(name = "«{0}» tiene {1} caracteres")
        @CsvSource({"AB, 2", "ABC, 3", "ABCDEFGHIJKLMNOPQRST, 20", "ABCDEFGHIJKLMNOPQRSTU, 21"})
        @DisplayName("Longitud de 3 a 20 caracteres")
        void longitud(String codigo, int longitud) {
            boolean valido = longitud >= 3 && longitud <= 20;
            assertThat(violacionesDe("codigo", codigo, "Nombre válido").isEmpty()).isEqualTo(valido);
            if (!valido) {
                assertThat(violacionesDe("codigo", codigo, "Nombre válido"))
                        .extracting(ConstraintViolation::getMessage)
                        .contains(ReglasEntrada.CODIGO_LONGITUD);
            }
        }

        @ParameterizedTest(name = "«{0}» es inválido")
        @ValueSource(strings = {"MAT 401", "MAT_401", "MAT.401", "MAT#401", "-MAT401", "MAT401-", "MAT--401", "MAÑ-401", "MÁT-401"})
        @DisplayName("Formato inválido: espacios internos, símbolos, guiones al borde o seguidos, tildes")
        void formatoInvalido(String codigo) {
            assertThat(violacionesDe("codigo", codigo, "Nombre válido"))
                    .extracting(ConstraintViolation::getMessage)
                    .contains(ReglasEntrada.CODIGO_FORMATO);
        }

        @ParameterizedTest(name = "«{0}» es obligatorio")
        @ValueSource(strings = {"", "   "})
        @DisplayName("Vacío o solo espacios es obligatorio")
        void vacio(String codigo) {
            assertThat(violacionesDe("codigo", codigo, "Nombre válido"))
                    .extracting(ConstraintViolation::getMessage)
                    .contains(ReglasEntrada.CODIGO_OBLIGATORIO);
        }
    }

    @Nested
    @DisplayName("Nombre")
    class Nombre {

        @ParameterizedTest(name = "nombre de {0} caracteres")
        @CsvSource({"2, false", "3, true", "100, true", "101, false"})
        @DisplayName("Longitud de 3 a 100 caracteres")
        void longitud(int longitud, boolean valido) {
            String nombre = "a".repeat(longitud);
            assertThat(violacionesDe("nombre", "MAT-401", nombre).isEmpty()).isEqualTo(valido);
        }

        @Test
        @DisplayName("La longitud se mide sobre el nombre ya normalizado")
        void longitudNormalizada() {
            String conEspacios = "  a   b  ";
            assertThat(violacionesDe("nombre", "MAT-401", conEspacios)).isEmpty();
            assertThat(violacionesDe("nombre", "MAT-401", " " + "a".repeat(100) + " ")).isEmpty();
        }

        @ParameterizedTest(name = "nombre con {0}")
        @ValueSource(strings = {"<script>alert(1)</script>", "Cálculo <b>", "Cálculo > Álgebra",
                "Cero\u0000Bits", "Campana\u0007", "Invisible​Texto", "Marca﻿BOM"})
        @DisplayName("Rechaza < >, caracteres de control y de formato invisibles")
        void caracteresProhibidos(String nombre) {
            assertThat(violacionesDe("nombre", "MAT-401", nombre))
                    .extracting(ConstraintViolation::getMessage)
                    .contains(ReglasEntrada.NOMBRE_CARACTERES);
        }

        @ParameterizedTest(name = "«{0}» es válido")
        @ValueSource(strings = {"Cálculo Integral", "Ingeniería de Software IV", "Física (Mecánica) & Ondas", "Ñandú 2-B"})
        @DisplayName("Acepta tildes, eñes, números y signos comunes")
        void validos(String nombre) {
            assertThat(violacionesDe("nombre", "MAT-401", nombre)).isEmpty();
        }

        @Test
        @DisplayName("Modificar valida el nombre con las mismas reglas")
        void modificar() {
            assertThat(validador.validate(new AsignaturaUpdateDTO("ab"))).extracting(ConstraintViolation::getMessage)
                    .contains(ReglasEntrada.NOMBRE_LONGITUD);
            assertThat(validador.validate(new AsignaturaUpdateDTO("<i>Hola</i>"))).extracting(ConstraintViolation::getMessage)
                    .contains(ReglasEntrada.NOMBRE_CARACTERES);
            assertThat(validador.validate(new AsignaturaUpdateDTO("Bases de Datos"))).isEmpty();
        }
    }

    @ParameterizedTest(name = "código «{0}» y nombre «{1}»")
    @CsvSource(delimiter = '|', value = {
            "MAT 401!       | <script>x</script>",
            "--ZZQQ--       | Q",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ | XY",
    })
    @DisplayName("Ningún mensaje de error repite el valor enviado")
    void mensajesNoRepitenValor(String codigo, String nombre) {
        Set<ConstraintViolation<AsignaturaRequestDTO>> violaciones = validar(codigo, nombre);
        assertThat(violaciones).isNotEmpty();
        AsignaturaRequestDTO normalizado = new AsignaturaRequestDTO(codigo, nombre, PROGRAMA);
        for (ConstraintViolation<AsignaturaRequestDTO> v : violaciones) {
            assertThat(v.getMessage())
                    .doesNotContain(codigo.trim())
                    .doesNotContain(normalizado.codigo())
                    .doesNotContain(normalizado.nombre().length() > 1 ? normalizado.nombre() : "\u0000");
        }
    }
}
