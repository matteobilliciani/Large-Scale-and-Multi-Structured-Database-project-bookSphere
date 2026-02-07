package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User's yearly wrapped data including reading statistics")
public class WrappedDTO {
    
    @Schema(description = "Year of the wrapped data", example = "2025")
    private Integer year;
    
    @Schema(description = "Best rated book by the user")
    private BookSummaryWithRatingDTO bestBook;
    
    @Schema(description = "Worst rated book by the user")
    private BookSummaryWithRatingDTO worstBook;
    
    @Schema(description = "Top 3 most read authors")
    private List<AuthorFrequencyDTO> topAuthors;
    
    @Schema(description = "Top 3 most read genres")
    private List<GenreFrequencyDTO> topGenres;
    
    @Schema(description = "Total books read in the year", example = "45")
    private Integer totalBooksRead;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookSummaryWithRatingDTO {
        private String id;
        private String title;
        private String authorName;
        private Integer rating;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorFrequencyDTO {
        private String name;
        private Integer count;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenreFrequencyDTO {
        private String name;
        private Integer count;
    }
}