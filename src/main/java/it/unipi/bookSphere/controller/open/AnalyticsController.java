package it.unipi.bookSphere.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics (Open)", description = "Public analytics and statistics endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(
            summary = "Get trending books",
            description = "Retrieve currently trending books based on recent reviews and ratings"
    )
    @GetMapping("/rankings/trendingbooks")
    public ResponseEntity<List<BookDTO>> getTrendingBooks() {
        List<BookDTO> trendingBooks = analyticsService.getTrendingBooks();
        return ResponseEntity.ok(trendingBooks);
    }

    @Operation(
            summary = "Get book rankings",
            description = "Get book rankings for a specific year or all-time"
    )
    @GetMapping("/rankings/books")
    public ResponseEntity<List<RankingDTO>> getBookRankings(
            @Parameter(description = "Year for rankings (optional)", example = "2025")
            @RequestParam(required = false) Integer year
    ) {
        List<RankingDTO> rankings = analyticsService.getBookRankings(year);
        return ResponseEntity.ok(rankings);
    }

    @Operation(
            summary = "Get author rankings",
            description = "Get author rankings for a specific year or all-time"
    )
    @GetMapping("/rankings/authors")
    public ResponseEntity<List<RankingDTO>> getAuthorRankings(
            @Parameter(description = "Year for rankings (optional)", example = "2025")
            @RequestParam(required = false) Integer year
    ) {
        List<RankingDTO> rankings = analyticsService.getAuthorRankings(year);
        return ResponseEntity.ok(rankings);
    }

    @Operation(
            summary = "Get genre rankings",
            description = "Get genre rankings for a specific year or all-time"
    )
    @GetMapping("/rankings/genres")
    public ResponseEntity<List<RankingDTO>> getGenreRankings(
            @Parameter(description = "Year for rankings (optional)", example = "2025")
            @RequestParam(required = false) Integer year
    ) {
        List<RankingDTO> rankings = analyticsService.getGenreRankings(year);
        return ResponseEntity.ok(rankings);
    }

    @Operation(
            summary = "Calculate Trending Probability Index (TPI)",
            description = "Predict how likely a book is to go viral based on author reputation and genre rankings"
    )
    @GetMapping("/tpi/{bookId}")
    public ResponseEntity<TpiPredictionDTO> calculateTPI(
            @Parameter(description = "MongoDB ObjectId of the book", example = "65b3f...")
            @PathVariable String bookId
    ) {
        TpiPredictionDTO tpi = analyticsService.calculateTPI(bookId);
        return ResponseEntity.ok(tpi);
    }

    @Operation(
            summary = "Calculate Author Versatility Index",
            description = "Measure how versatile an author is in terms of covered genres"
    )
    @GetMapping("/versatility/{authorId}")
    public ResponseEntity<?> calculateAuthorVersatility(
            @Parameter(description = "MongoDB ObjectId of the author", example = "65b3a...")
            @PathVariable String authorId
    ) {
        // TODO: Implement service call - Neo4j Query
        // Double versatility = analyticsService.calculateAuthorVersatility(authorId);
        // return ResponseEntity.ok(Map.of("authorId", authorId, "versatility", versatility));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Calculate Internationality Index",
            description = "Measure how far a book or author travels across the globe. Specify the entity type (BOOK or AUTHOR) and its MongoDB ObjectId."
    )
    @GetMapping("/internationality/{entityId}")
    public ResponseEntity<List<InternationalityDTO>> calculateInternationality(
            @Parameter(description = "MongoDB ObjectId of the book or author", example = "65b3f...")
            @PathVariable String entityId,
            @Parameter(description = "Type of entity: BOOK or AUTHOR", example = "BOOK")
            @RequestParam String entityType
    ) {
        List<InternationalityDTO> internationality = analyticsService.calculateInternationality(entityId, entityType);
        return ResponseEntity.ok(internationality);
    }

    @Operation(
            summary = "Identify genre influencers",
            description = "Find real influencers in a genre based on review engagement quality. If genre not specified, returns top influencers across all genres."
    )
    @GetMapping("/influencers")
    public ResponseEntity<List<InfluencerDTO>> getInfluencers(
            @Parameter(description = "Genre name (optional)", example = "Fantasy")
            @RequestParam(required = false) String genre,
            @Parameter(description = "Maximum number of influencers to return (default 10)", example = "10")
            @RequestParam(required = false) Integer limit
    ) {
        List<InfluencerDTO> influencers = analyticsService.getInfluencers(genre, limit);
        return ResponseEntity.ok(influencers);
    }
}
