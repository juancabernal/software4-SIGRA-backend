package co.edu.uco.sigra.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/** Limita una cadena por bytes UTF-8, no por unidades UTF-16. */
@Documented
@Constraint(validatedBy = Utf8ByteSizeValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface Utf8ByteSize {

    String message() default "El valor supera el máximo permitido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int max();
}
