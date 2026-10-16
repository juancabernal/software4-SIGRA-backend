package co.edu.uco.sigra.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public final class Utf8ByteSizeValidator implements ConstraintValidator<Utf8ByteSize, String> {

    private int max;

    @Override
    public void initialize(Utf8ByteSize annotation) {
        max = annotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // La obligatoriedad queda a cargo de @NotBlank.
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= max;
    }
}
