package it.unipi.bookSphere.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Validator for ISO country codes
 */
public class CountryCodeValidator implements ConstraintValidator<ValidCountryCode, String> {
    
    private static final Set<String> VALID_COUNTRY_CODES = 
        Arrays.stream(Locale.getISOCountries()).collect(Collectors.toSet());
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        
        // Accept both uppercase and lowercase
        return VALID_COUNTRY_CODES.contains(value.toUpperCase());
    }
}
