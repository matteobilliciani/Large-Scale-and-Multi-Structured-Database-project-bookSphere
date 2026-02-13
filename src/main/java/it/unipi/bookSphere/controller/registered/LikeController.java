package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.service.registered.LikeService;
import it.unipi.bookSphere.validation.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
        String bookId = ValidationUtils.extractAndValidateField(requestBody, "bookId");
        ValidationUtils.validateObjectId(bookId, "bookId");
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
        ValidationUtils.validateObjectId(bookID, "bookID");
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
        String reviewId = ValidationUtils.extractAndValidateField(requestBody, "reviewId");
        ValidationUtils.validateObjectId(reviewId, "reviewId");
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
        ValidationUtils.validateObjectId(reviewid, "reviewid");
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
        String genreName = ValidationUtils.extractAndValidateField(requestBody, "genreName");
        ValidationUtils.validateGenreName(genreName);
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
        ValidationUtils.validateGenreName(name);
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
        String authorId = ValidationUtils.extractAndValidateField(requestBody, "authorId");
        ValidationUtils.validateObjectId(authorId, "authorId");
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
        ValidationUtils.validateObjectId(authorID, "authorID");
        likeService.unlikeAuthor(authorID);
        return ResponseEntity.ok(Map.of("message", "Author unliked successfully"));
    }

    // ========== GET LIKED ITEMS ==========
    
    @Operation(
            summary = "Get liked books",
            description = "Retrieve all books liked by the current user. Queries Neo4j for LIKES relationships. Supports pagination."
    )
    @GetMapping("/liked/book")
    public ResponseEntity<Page<BookDTO>> getLikedBooks(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Page<BookDTO> likedBooks = likeService.getLikedBooks(page, size);
        return ResponseEntity.ok(likedBooks);
    }

    @Operation(
            summary = "Get liked authors",
            description = "Retrieve all authors liked by the current user. Queries Neo4j for LIKES relationships. Supports pagination."
    )
    @GetMapping("/liked/author")
    public ResponseEntity<Page<AuthorDTO>> getLikedAuthors(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Page<AuthorDTO> likedAuthors = likeService.getLikedAuthors(page, size);
        return ResponseEntity.ok(likedAuthors);
    }

    @Operation(
            summary = "Get liked reviews",
            description = "Retrieve all reviews liked by the current user. Queries Neo4j for LIKES relationships. Supports pagination."
    )
    @GetMapping("/liked/review")
    public ResponseEntity<Page<ReviewDTO>> getLikedReviews(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Page<ReviewDTO> likedReviews = likeService.getLikedReviews(page, size);
        return ResponseEntity.ok(likedReviews);
    }

    @Operation(
            summary = "Get liked genres",
            description = "Retrieve all genres liked by the current user. Queries Neo4j for LIKES relationships. Supports pagination."
    )
    @GetMapping("/liked/genre")
    public ResponseEntity<Page<GenreDTO>> getLikedGenres(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Page<GenreDTO> likedGenres = likeService.getLikedGenres(page, size);
        return ResponseEntity.ok(likedGenres);
    }
}
