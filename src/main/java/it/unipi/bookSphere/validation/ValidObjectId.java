package it.unipi.bookSphere.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a string is a valid MongoDB ObjectId format (24 hex characters)
 */
@Documented
@Constraint(validatedBy = ObjectIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidObjectId {
    String message() default "Invalid ObjectId format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    boolean optional() default false; // Allow null values if true
}
