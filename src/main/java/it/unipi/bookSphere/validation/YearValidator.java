package it.unipi.bookSphere.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Year;

/**
 * Validator for publication year
 */
public class YearValidator implements ConstraintValidator<ValidYear, Integer> {
    
    private int minYear;
    private boolean allowFuture;
    
    @Override
    public void initialize(ValidYear constraintAnnotation) {
        this.minYear = constraintAnnotation.minYear();
        this.allowFuture = constraintAnnotation.allowFuture();
    }
    
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        
        int currentYear = Year.now().getValue();
        
        // Check minimum year
        if (value < minYear) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Year must be after " + minYear
            ).addConstraintViolation();
            return false;
        }
        
        // Check if future is allowed
        if (!allowFuture && value > currentYear) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Year cannot be in the future (current year: " + currentYear + ")"
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
