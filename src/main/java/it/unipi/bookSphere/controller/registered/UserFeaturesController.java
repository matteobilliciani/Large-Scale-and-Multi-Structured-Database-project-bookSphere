package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.RecommendationDTO;
import it.unipi.bookSphere.dto.WrappedDTO;
import it.unipi.bookSphere.service.UserFeaturesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Features (Registered)", description = "Personalized recommendations and yearly recap")
public class UserFeaturesController {

    private final UserFeaturesService userFeaturesService;

    @Operation(
            summary = "Get personalized recommendations",
            description = "Get book recommendations based on user's social network, favorite authors, genres, and reading history. Uses Neo4j Query 1."
    )
    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(
            @Parameter(description = "Maximum number of recommendations (default 10)", example = "10")
            @RequestParam(required = false) Integer limit
    ) {
        List<RecommendationDTO> recommendations = userFeaturesService.getRecommendations(limit);
        return ResponseEntity.ok(recommendations);
    }

    @Operation(
            summary = "Get yearly wrapped",
            description = "Generate personalized yearly recap including highest/lowest rated books, most read authors and genres. Uses MongoDB Query 2."
    )
    @GetMapping("/wrapped")
    public ResponseEntity<WrappedDTO> getYearlyWrapped() {
        WrappedDTO wrapped = userFeaturesService.getYearlyWrapped();
        return ResponseEntity.ok(wrapped);
    }
}
