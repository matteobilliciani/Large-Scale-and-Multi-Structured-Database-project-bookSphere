package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Features (Registered)", description = "Personalized recommendations and yearly recap")
public class UserFeaturesController {

    // TODO: Inject UserFeaturesService when implemented
    // private final UserFeaturesService userFeaturesService;

    @Operation(
            summary = "Get personalized recommendations",
            description = "Get book recommendations based on user's social network, favorite authors, genres, and reading history. Uses Neo4j Query 1."
    )
    @GetMapping("/recommendations")
    public ResponseEntity<?> getRecommendations() {
        // TODO: Implement service call - Neo4j Query 1
        // List<BookDTO> recommendations = userFeaturesService.getRecommendations(currentUserId);
        // return ResponseEntity.ok(recommendations);
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Get yearly wrapped",
            description = "Generate personalized yearly recap including highest/lowest rated books, most read authors and genres. Uses MongoDB Query 2."
    )
    @GetMapping("/wrapped")
    public ResponseEntity<?> getYearlyWrapped() {
        // TODO: Implement service call - MongoDB Query 2
        // Map<String, Object> wrapped = userFeaturesService.getYearlyWrapped(currentUserId);
        // return ResponseEntity.ok(wrapped);
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
