package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Likes (Registered)", description = "Like/unlike endpoints for books, reviews, authors, and genres")
public class LikeController {

    private final LikeService likeService;

    // ========== BOOK LIKES ==========
    
    @Operation(
            summary = "Like a book",
            description = "Add a like to a book. Creates LIKES relationship in Neo4j."
    )
    @PostMapping("/like/book")
    public ResponseEntity<Map<String, String>> likeBook(
            @Parameter(description = "Book ID in request body")
            @RequestBody Map<String, String> requestBody
    ) {
        String bookId = requestBody.get("bookId");
        likeService.likeBook(bookId);
        return ResponseEntity.ok(Map.of("message", "Book liked successfully"));
    }

    @Operation(
            summary = "Unlike a book",
            description = "Remove a like from a book. Removes LIKES relationship in Neo4j."
    )
    @DeleteMapping("/unlike/book/{bookID}")
    public ResponseEntity<Map<String, String>> unlikeBook(
            @Parameter(description = "MongoDB ObjectId of the book", example = "65b3f...")
            @PathVariable String bookID
    ) {
        likeService.unlikeBook(bookID);
        return ResponseEntity.ok(Map.of("message", "Book unliked successfully"));
    }

    // ========== REVIEW LIKES ==========
    
    @Operation(
            summary = "Like a review",
            description = "Add a like to a review. Creates LIKES relationship in Neo4j and updates likes_count in MongoDB."
    )
    @PostMapping("/like/review")
    public ResponseEntity<Map<String, String>> likeReview(
            @Parameter(description = "Review ID in request body")
            @RequestBody Map<String, String> requestBody
    ) {
        String reviewId = requestBody.get("reviewId");
        likeService.likeReview(reviewId);
        return ResponseEntity.ok(Map.of("message", "Review liked successfully"));
    }

    @Operation(
            summary = "Unlike a review",
            description = "Remove a like from a review. Removes LIKES relationship in Neo4j and updates likes_count in MongoDB."
    )
    @DeleteMapping("/unlikes/review/{reviewid}")
    public ResponseEntity<Map<String, String>> unlikeReview(
            @Parameter(description = "MongoDB ObjectId of the review", example = "99a1...")
            @PathVariable String reviewid
    ) {
        likeService.unlikeReview(reviewid);
        return ResponseEntity.ok(Map.of("message", "Review unliked successfully"));
    }

    // ========== GENRE LIKES ==========
    
    @Operation(
            summary = "Like a genre",
            description = "Add a like to a genre. Creates LIKES relationship in Neo4j."
    )
    @PostMapping("/likes/genres")
    public ResponseEntity<Map<String, String>> likeGenre(
            @Parameter(description = "Genre name in request body")
            @RequestBody Map<String, String> requestBody
    ) {
        String genreName = requestBody.get("genreName");
        likeService.likeGenre(genreName);
        return ResponseEntity.ok(Map.of("message", "Genre liked successfully"));
    }

    @Operation(
            summary = "Unlike a genre",
            description = "Remove a like from a genre. Removes LIKES relationship in Neo4j."
    )
    @DeleteMapping("/unlike/genres/{name}")
    public ResponseEntity<Map<String, String>> unlikeGenre(
            @Parameter(description = "Genre name", example = "Fantasy")
            @PathVariable String name
    ) {
        likeService.unlikeGenre(name);
        return ResponseEntity.ok(Map.of("message", "Genre unliked successfully"));
    }

    // ========== AUTHOR LIKES ==========
    
    @Operation(
            summary = "Like an author",
            description = "Add a like to an author. Creates LIKES relationship in Neo4j."
    )
    @PostMapping("/likes/authors")
    public ResponseEntity<Map<String, String>> likeAuthor(
            @Parameter(description = "Author ID in request body")
            @RequestBody Map<String, String> requestBody
    ) {
        String authorId = requestBody.get("authorId");
        likeService.likeAuthor(authorId);
        return ResponseEntity.ok(Map.of("message", "Author liked successfully"));
    }

    @Operation(
            summary = "Unlike an author",
            description = "Remove a like from an author. Removes LIKES relationship in Neo4j."
    )
    @DeleteMapping("/unlike/authors/{authorID}")
    public ResponseEntity<Map<String, String>> unlikeAuthor(
            @Parameter(description = "Author ID", example = "65b3a...")
            @PathVariable String authorID
    ) {
        likeService.unlikeAuthor(authorID);
        return ResponseEntity.ok(Map.of("message", "Author unliked successfully"));
    }

    // ========== GET LIKED ITEMS ==========
    
    @Operation(
            summary = "Get liked books",
            description = "Retrieve all books liked by the current user. Queries Neo4j for LIKES relationships."
    )
    @GetMapping("/liked/book")
    public ResponseEntity<List<BookDTO>> getLikedBooks() {
        List<BookDTO> likedBooks = likeService.getLikedBooks();
        return ResponseEntity.ok(likedBooks);
    }

    @Operation(
            summary = "Get liked authors",
            description = "Retrieve all authors liked by the current user. Queries Neo4j for LIKES relationships."
    )
    @GetMapping("/liked/author")
    public ResponseEntity<List<AuthorDTO>> getLikedAuthors() {
        List<AuthorDTO> likedAuthors = likeService.getLikedAuthors();
        return ResponseEntity.ok(likedAuthors);
    }

    @Operation(
            summary = "Get liked reviews",
            description = "Retrieve all reviews liked by the current user. Queries Neo4j for LIKES relationships."
    )
    @GetMapping("/liked/review")
    public ResponseEntity<List<ReviewDTO>> getLikedReviews() {
        List<ReviewDTO> likedReviews = likeService.getLikedReviews();
        return ResponseEntity.ok(likedReviews);
    }

    @Operation(
            summary = "Get liked genres",
            description = "Retrieve all genres liked by the current user. Queries Neo4j for LIKES relationships."
    )
    @GetMapping("/liked/genre")
    public ResponseEntity<List<GenreDTO>> getLikedGenres() {
        List<GenreDTO> likedGenres = likeService.getLikedGenres();
        return ResponseEntity.ok(likedGenres);
    }
}
