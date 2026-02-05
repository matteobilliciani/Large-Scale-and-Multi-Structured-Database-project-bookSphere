package it.unipi.bookSphere.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews (Open)", description = "Public review endpoints accessible to all users")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "Get reviews by IDs",
            description = "Retrieve multiple reviews given a list of review IDs. Used to fetch all reviews for a book or user."
    )
    @GetMapping
    public ResponseEntity<List<ReviewDTO>> getReviewsByIds(
            @Parameter(description = "List of review IDs", example = "review=99a1...&review=99a2...&review=99a3...")
            @RequestParam(name = "review", required = true) List<String> reviewIds
    ) {
        List<ReviewDTO> reviews = reviewService.getReviewsByIds(reviewIds);
        return ResponseEntity.ok(reviews);
    }
}
