package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternationalityDTO {
    @Schema(description = "Country code", example = "IT")
    private String country;
    
    @Schema(description = "Number of unique users from this country", example = "150")
    private Long uniqueUsers;
    
    @Schema(description = "Total number of interactions (likes + reviews) from this country", example = "287")
    private Long totalInteractions;
}
