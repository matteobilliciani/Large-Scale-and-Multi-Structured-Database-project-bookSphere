package it.unipi.bookSphere.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.service.AdminModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Moderation", description = "Admin endpoints for content moderation and user management")
public class AdminModerationController {

    private final AdminModerationService adminModerationService;

    @Operation(
            summary = "Delete review (moderation)",
            description = "Remove an offensive or inappropriate review from the system. Deletes from both MongoDB and Neo4j."
    )
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<?> deleteReview(
            @Parameter(description = "MongoDB ObjectId of the review", example = "99a1...")
            @PathVariable String id
    ) {
        adminModerationService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Ban user",
            description = "Ban a user from the platform by updating their status to 'banned' in MongoDB"
    )
    @PatchMapping("/users/{id}/ban")
    public ResponseEntity<?> banUser(
            @Parameter(description = "MongoDB ObjectId of the user", example = "65d1...")
            @PathVariable String id
    ) {
        adminModerationService.banUser(id);
        return ResponseEntity.ok(Map.of("message", "User banned successfully", "userId", id));
    }

    @Operation(
            summary = "View all users",
            description = "Retrieve a list of all registered users for moderation purposes"
    )
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminModerationService.getAllUsers(page, size));
    }

    @Operation(
            summary = "View all reviews",
            description = "Retrieve a list of all reviews for moderation purposes"
    )
    @GetMapping("/reviews")
    public ResponseEntity<?> getAllReviews(
            @Parameter(description = "Page number", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminModerationService.getAllReviews(page, size));
    }
}
