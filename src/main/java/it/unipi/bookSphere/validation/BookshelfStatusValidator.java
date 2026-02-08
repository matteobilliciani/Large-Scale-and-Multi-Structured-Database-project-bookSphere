package it.unipi.bookSphere.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;

/**
 * Validator for bookshelf status values
 */
public class BookshelfStatusValidator implements ConstraintValidator<ValidBookshelfStatus, String> {
    
    private static final List<String> VALID_STATUSES = Arrays.asList("to_read", "reading", "read");
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        
        return VALID_STATUSES.contains(value.toLowerCase());
    }
}
