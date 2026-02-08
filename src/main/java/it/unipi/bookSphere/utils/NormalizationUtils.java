package it.unipi.bookSphere.utils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Utility class for normalizing text to prevent inconsistencies
 */
public class NormalizationUtils {

    /**
     * Normalize a genre name to Title Case for consistency
     * "fantasy" -> "Fantasy"
     * "science fiction" -> "Science Fiction"
     * "HORROR" -> "Horror"
     */
    public static String normalizeGenreName(String genreName) {
        if (genreName == null || genreName.isBlank()) {
            return genreName;
        }
        
        // Trim and normalize spaces
        String normalized = genreName.trim().replaceAll("\\s+", " ");
        
        // Convert to Title Case
        return Arrays.stream(normalized.split("\\s+"))
                .map(word -> word.isEmpty() ? word : 
                     Character.toTitleCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
    
    /**
     * Normalize an author name to Title Case for consistency
     * "j.k. rowling" -> "J.K. Rowling"
     * "STEPHEN KING" -> "Stephen King"
     * "george r.r. martin" -> "George R.R. Martin"
     */
    public static String normalizeAuthorName(String authorName) {
        if (authorName == null || authorName.isBlank()) {
            return authorName;
        }
        
        // Trim and normalize spaces
        String normalized = authorName.trim().replaceAll("\\s+", " ");
        
        // Convert to Title Case, preserving dots for initials
        return Arrays.stream(normalized.split("\\s+"))
                .map(word -> {
                    if (word.isEmpty()) return word;
                    
                    // Handle initials like "J.K." or "R.R."
                    if (word.contains(".")) {
                        return Arrays.stream(word.split("\\."))
                                .map(part -> part.isEmpty() ? part : 
                                     Character.toUpperCase(part.charAt(0)) + 
                                     (part.length() > 1 ? part.substring(1).toLowerCase() : ""))
                                .collect(Collectors.joining("."));
                    }
                    
                    // Regular word
                    return Character.toTitleCase(word.charAt(0)) + word.substring(1).toLowerCase();
                })
                .collect(Collectors.joining(" "));
    }
    
    /**
     * Normalize a book title - trim and normalize spaces
     * Preserves original capitalization as book titles can have specific formatting
     * "  The Lord  of the   Rings  " -> "The Lord of the Rings"
     */
    public static String normalizeBookTitle(String title) {
        if (title == null || title.isBlank()) {
            return title;
        }
        
        // Trim and normalize spaces only, preserve original case
        return title.trim().replaceAll("\\s+", " ");
    }
    
    /**
     * Normalize a username - lowercase and trim
     * "UserName123" -> "username123"
     */
    public static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            return username;
        }
        
        return username.trim().toLowerCase();
    }
    
    /**
     * Remove accents and diacritics from text
     * "José García" -> "Jose Garcia"
     */
    public static String removeAccents(String text) {
        if (text == null) {
            return null;
        }
        
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
    
    /**
     * Create a search-friendly version of text (lowercase, no accents, normalized spaces)
     * Useful for case-insensitive and accent-insensitive searches
     */
    public static String toSearchable(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        
        String normalized = removeAccents(text);
        return normalized.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
