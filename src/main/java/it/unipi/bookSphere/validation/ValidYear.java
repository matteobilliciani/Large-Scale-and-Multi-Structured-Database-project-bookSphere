package it.unipi.bookSphere.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a year is reasonable (not in future, not too old)
 */
@Documented
@Constraint(validatedBy = YearValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidYear {
    String message() default "Invalid publication year";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int minYear() default 1000; // Minimum year
    boolean allowFuture() default false; // Allow future years
}
