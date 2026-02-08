package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.service.ReviewService;
import it.unipi.bookSphere.utils.ValidationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController("registeredReviewController")
@RequestMapping("/api/v1/me/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reviews (Registered)", description = "Review management endpoints for registered users")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "Create a review",
            description = "Write a review for a book with rating and optional comment. Updates both MongoDB and Neo4j."
    )
    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(
            @Valid @RequestBody ReviewDTO reviewDTO
    ) {
        ReviewDTO created = reviewService.createReview(reviewDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Update a review",
            description = "Modify a previously posted review. Updates both MongoDB and Neo4j."
    )
    @PatchMapping("/{reviewID}")
    public ResponseEntity<ReviewDTO> updateReview(
            @Parameter(description = "MongoDB ObjectId of the review", example = "99a1...")
            @PathVariable String reviewID,
            @Valid @RequestBody ReviewDTO reviewDTO
    ) {
        ValidationUtils.validateObjectId(reviewID, "reviewID");
        ReviewDTO updated = reviewService.updateReview(reviewID, reviewDTO);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete a review",
            description = "Remove a review from the system. Updates both MongoDB and Neo4j."
    )
    @DeleteMapping("/{reviewID}")
    public ResponseEntity<Void> deleteReview(
            @Parameter(description = "MongoDB ObjectId of the review", example = "99a1...")
            @PathVariable String reviewID
    ) {
        ValidationUtils.validateObjectId(reviewID, "reviewID");
        reviewService.deleteReview(reviewID);
        return ResponseEntity.noContent().build();
    }
}
