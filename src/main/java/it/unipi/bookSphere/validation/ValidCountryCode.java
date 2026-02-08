package it.unipi.bookSphere.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a country code is valid (2-letter ISO code)
 */
@Documented
@Constraint(validatedBy = CountryCodeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCountryCode {
    String message() default "Invalid country code (must be 2-letter ISO code)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
