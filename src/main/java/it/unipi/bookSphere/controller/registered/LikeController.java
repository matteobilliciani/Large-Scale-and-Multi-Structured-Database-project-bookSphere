package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Likes (Registered)", description = "Like/unlike endpoints for books, reviews, authors, and genres")
public class LikeController {

    // TODO: Inject LikeService when implemented
    // private final LikeService likeService;

    // ========== BOOK LIKES ==========
    
    @Operation(
            summary = "Like a book",
            description = "Add a like to a book. Creates LIKES relationship in Neo4j."
    )
    @PostMapping("/like/book")
    public ResponseEntity<?> likeBook(
            @Parameter(description = "Book ID in request body")
            @RequestBody String bookId
    ) {
        // TODO: Implement service call
        // likeService.likeBook(currentUserId, bookId);
        // return ResponseEntity.ok(Map.of("message", "Book liked successfully"));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Unlike a book",
            description = "Remove a like from a book. Removes LIKES relationship in Neo4j."
    )
    @DeleteMapping("/unlike/book/{bookID}")
    public ResponseEntity<?> unlikeBook(
            @Parameter(description = "MongoDB ObjectId of the book", example = "65b3f...")
            @PathVariable String bookID
    ) {
        // TODO: Implement service call
        // likeService.unlikeBook(currentUserId, bookID);
        // return ResponseEntity.ok(Map.of("message", "Book unliked successfully"));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ========== REVIEW LIKES ==========
    
    @Operation(
            summary = "Like a review",
            description = "Add a like to a review. Creates LIKES relationship in Neo4j and updates likes_count in MongoDB."
    )
    @PostMapping("/like/review")
    public ResponseEntity<?> likeReview(
            @Parameter(description = "Review ID in request body")
            @RequestBody String reviewId
    ) {
        // TODO: Implement service call
        // likeService.likeReview(currentUserId, reviewId);
        // return ResponseEntity.ok(Map.of("message", "Review liked successfully"));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Unlike a review",
            description = "Remove a like from a review. Removes LIKES relationship in Neo4j and updates likes_count in MongoDB."
    )
    @DeleteMapping("/unlikes/review/{reviewid}")
    public ResponseEntity<?> unlikeReview(
            @Parameter(description = "MongoDB ObjectId of the review", example = "99a1...")
            @PathVariable String reviewid
    ) {
        // TODO: Implement service call
        // likeService.unlikeReview(currentUserId, reviewid);
        // return ResponseEntity.ok(Map.of("message", "Review unliked successfully"));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ========== GENRE LIKES ==========
    
    @Operation(
            summary = "Like a genre",
            description = "Add a like to a genre. Creates LIKES relationship in Neo4j and updates MongoDB."
    )
    @PostMapping("/likes/genres")
    public ResponseEntity<?> likeGenre(
            @Parameter(description = "Genre name in request body")
            @RequestBody String genreName
    ) {
        // TODO: Implement service call
        // likeService.likeGenre(currentUserId, genreName);
        // return ResponseEntity.ok(Map.of("message", "Genre liked successfully"));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Unlike a genre",
            description = "Remove a like from a genre. Removes LIKES relationship in Neo4j and updates MongoDB."
    )
    @DeleteMapping("/unlike/genres/{name}")
    public ResponseEntity<?> unlikeGenre(
            @Parameter(description = "Genre name", example = "Fantasy")
            @PathVariable String name
    ) {
        // TODO: Implement service call
        // likeService.unlikeGenre(currentUserId, name);
        // return ResponseEntity.ok(Map.of("message", "Genre unliked successfully"));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ========== AUTHOR LIKES ==========
    
    @Operation(
            summary = "Like an author",
            description = "Add a like to an author. Creates LIKES relationship in Neo4j."
    )
    @PostMapping("/likes/authors")
    public ResponseEntity<?> likeAuthor(
            @Parameter(description = "Author ID in request body")
            @RequestBody String authorId
    ) {
        // TODO: Implement service call
        // likeService.likeAuthor(currentUserId, authorId);
        // return ResponseEntity.ok(Map.of("message", "Author liked successfully"));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Unlike an author",
            description = "Remove a like from an author. Removes LIKES relationship in Neo4j."
    )
    @DeleteMapping("/unlike/authors/{authorID}")
    public ResponseEntity<?> unlikeAuthor(
            @Parameter(description = "Author ID", example = "65b3a...")
            @PathVariable String authorID
    ) {
        // TODO: Implement service call
        // likeService.unlikeAuthor(currentUserId, authorID);
        // return ResponseEntity.ok(Map.of("message", "Author unliked successfully"));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // ========== GET LIKED ITEMS ==========
    
    @Operation(
            summary = "Get liked books",
            description = "Retrieve all books liked by the current user. Queries Neo4j for LIKES relationships."
    )
    @GetMapping("/liked/book")
    public ResponseEntity<?> getLikedBooks() {
        // TODO: Implement service call
        // List<BookDTO> likedBooks = likeService.getLikedBooks(currentUserId);
        // return ResponseEntity.ok(likedBooks);
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Get liked authors",
            description = "Retrieve all authors liked by the current user. Queries Neo4j for LIKES relationships."
    )
    @GetMapping("/liked/author")
    public ResponseEntity<?> getLikedAuthors() {
        // TODO: Implement service call
        // List<AuthorDTO> likedAuthors = likeService.getLikedAuthors(currentUserId);
        // return ResponseEntity.ok(likedAuthors);
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Get liked reviews",
            description = "Retrieve all reviews liked by the current user. Queries Neo4j for LIKES relationships."
    )
    @GetMapping("/liked/review")
    public ResponseEntity<?> getLikedReviews() {
        // TODO: Implement service call
        // List<ReviewDTO> likedReviews = likeService.getLikedReviews(currentUserId);
        // return ResponseEntity.ok(likedReviews);
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Get liked genres",
            description = "Retrieve all genres liked by the current user. Queries Neo4j for LIKES relationships."
    )
    @GetMapping("/liked/genre")
    public ResponseEntity<?> getLikedGenres() {
        // TODO: Implement service call
        // List<GenreDTO> likedGenres = likeService.getLikedGenres(currentUserId);
        // return ResponseEntity.ok(likedGenres);
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
