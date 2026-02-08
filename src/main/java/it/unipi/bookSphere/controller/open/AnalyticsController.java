package it.unipi.bookSphere.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
        description = "Restituisce le classifiche. NOTA: Puoi filtrare per 'author' O per 'genre', ma non entrambi contemporaneamente."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Classifica restituita con successo"),
        @ApiResponse(responseCode = "400", description = "Errore: Non puoi specificare sia autore che genere")
    })
    @GetMapping("/books")
    public ResponseEntity<?> getBookRankings( // Uso <?> o <Object> per poter ritornare sia la lista che un messaggio di errore stringa
            
            @Parameter(description = "Anno della classifica (opzionale)")
            @RequestParam(required = false) Integer year,

            @Parameter(description = "Filtra per nome autore (Mutuamente esclusivo con genre)")
            @RequestParam(required = false) String author,

            @Parameter(description = "Filtra per genere (Mutuamente esclusivo con author)")
            @RequestParam(required = false) String genre
    ) {
        // --- VALIDAZIONE ---
        // Se entrambi i parametri sono presenti (non null), blocchiamo la richiesta.
        if (author != null && genre != null) {
            return ResponseEntity
                    .badRequest()
                    .body("Errore: Non è possibile filtrare contemporaneamente per 'author' e 'genre'. Scegline solo uno.");
        }

        // --- CHIAMATA AL SERVICE ---
        // A questo punto siamo sicuri che author e genre non sono entrambi valorizzati
        List<RankingDTO> rankings = analyticsService.getBookRankings(year, author, genre);
        
        return ResponseEntity.ok(rankings);
    }

    @Operation(
            summary = "Get author rankings",
            description = "Get author rankings all-time"
    )
    @GetMapping("/rankings/authors")
    public ResponseEntity<List<RankingDTO>> getAuthorRankings(){
        List<RankingDTO> rankings = analyticsService.getAuthorRankings();
        return ResponseEntity.ok(rankings);
    }

    /* 
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
    */

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
