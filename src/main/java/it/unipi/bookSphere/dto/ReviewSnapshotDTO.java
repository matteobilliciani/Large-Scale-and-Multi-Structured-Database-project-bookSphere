package it.unipi.bookSphere.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ReviewSnapshotDTO {
    @Schema(description = "MongoDB ObjectId of the review")
    private String id;

    @Schema(description = "MongoDB ObjectId of the user who wrote the review")
    private String userId;

    @Schema(description = "Username of the reviewer", example = "BookLover")
    private String username;

    @Schema(description = "Rating (0-100)", example = "85")
    private Integer rating;

    @Schema(description = "Review snippet/preview", example = "Absolutely incredible...")
    private String snippet;

    @Schema(description = "Full review text/comment", example = "An amazing book that changed my life. The storytelling is incredible...")
    private String text;

    @Schema(description = "Review summary", example = "Must read")
    private String summary;

    @Schema(description = "Date of the review")
    private String date;

    @Schema(description = "Number of likes (only for popular reviews)")
    private Integer numOfLike;
}
