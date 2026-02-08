package it.unipi.bookSphere.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for MongoDB ObjectId format
 */
public class ObjectIdValidator implements ConstraintValidator<ValidObjectId, String> {
    
    private boolean optional;
    
    @Override
    public void initialize(ValidObjectId constraintAnnotation) {
        this.optional = constraintAnnotation.optional();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Allow null if optional is true
        if (value == null) {
            return optional;
        }
        
        // Check if it's blank
        if (value.isBlank()) {
            return false;
        }
        
        // MongoDB ObjectId is a 24-character hex string
        if (value.length() != 24) {
            return false;
        }
        
        // Check if all characters are valid hex digits
        return value.matches("^[0-9a-fA-F]{24}$");
    }
}
