package it.unipi.bookSphere.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a bookshelf status is one of: to_read, reading, read
 */
@Documented
@Constraint(validatedBy = BookshelfStatusValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBookshelfStatus {
    String message() default "Status must be one of: to_read, reading, read";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
