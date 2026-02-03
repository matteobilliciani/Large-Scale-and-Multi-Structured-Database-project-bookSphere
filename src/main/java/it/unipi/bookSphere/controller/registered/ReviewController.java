package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.ReviewDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/reviews")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reviews (Registered)", description = "Review management endpoints for registered users")
public class ReviewController {

    // TODO: Inject ReviewService when implemented
    // private final ReviewService reviewService;

    @Operation(
            summary = "Create a review",
            description = "Write a review for a book with rating and optional comment. Updates both MongoDB and Neo4j."
    )
    @PostMapping
    public ResponseEntity<?> createReview(
            @Valid @RequestBody ReviewDTO reviewDTO
    ) {
        // TODO: Implement service call
        // ReviewDTO created = reviewService.createReview(reviewDTO, currentUserId);
        // return ResponseEntity.status(HttpStatus.CREATED).body(created);
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Update a review",
            description = "Modify a previously posted review. Updates both MongoDB and Neo4j."
    )
    @PatchMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @Parameter(description = "MongoDB ObjectId of the review", example = "99a1...")
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewDTO reviewDTO
    ) {
        // TODO: Implement service call
        // ReviewDTO updated = reviewService.updateReview(reviewId, reviewDTO, currentUserId);
        // return ResponseEntity.ok(updated);
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Delete a review",
            description = "Remove a review from the system. Updates both MongoDB and Neo4j."
    )
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @Parameter(description = "MongoDB ObjectId of the review", example = "99a1...")
            @PathVariable String reviewId
    ) {
        // TODO: Implement service call
        // reviewService.deleteReview(reviewId, currentUserId);
        // return ResponseEntity.noContent().build();
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
