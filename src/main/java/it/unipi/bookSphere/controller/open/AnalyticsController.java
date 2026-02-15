package it.unipi.bookSphere.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.service.open.AnalyticsService;
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

            @Parameter(description = "Filtra per ID autore (Mutuamente esclusivo con genre)")
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

    @Operation(
            summary = "Get book revaluation",
            description = "Retrieve books that have been revaluated recently"
    )
    @GetMapping("/books/revaluated")
    public ResponseEntity<List<BookTrendDTO>> getBookRevaluation() {
        List<BookTrendDTO> bookRevaluation = analyticsService.getBookRevaluation();
        return ResponseEntity.ok(bookRevaluation);
    }

    @Operation(
            summary = "Get book rankings by book IDs",
            description = "Retrieve book rankings for a specific list of book IDs, optionally filtered by year"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Classifica restituita con successo"),
            @ApiResponse(responseCode = "400", description = "Errore: lista di IDs vuota o non valida")
    })
    @PostMapping("/rankings/byBookIds")
    public ResponseEntity<List<RankingDTO>> getBookRankingsByIds(
            @Parameter(description = "Anno della classifica (opzionale)")
            @RequestParam(required = false) Integer year,
            
            @RequestBody BookIdsRequestDTO request
    ) {
        List<RankingDTO> rankings = analyticsService.getBookRankingsByIds(year, request.getBookIds());
        return ResponseEntity.ok(rankings);
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
