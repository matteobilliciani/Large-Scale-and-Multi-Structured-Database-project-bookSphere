package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrendScoreDTO {
    @Schema(description = "Current trending score/rating", example = "4.35")
    private Double rating;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
