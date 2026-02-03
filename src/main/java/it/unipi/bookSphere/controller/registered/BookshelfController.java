package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.BookshelfRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/bookshelf")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bookshelf (Registered)", description = "User bookshelf management endpoints")
public class BookshelfController {

    // TODO: Inject BookshelfService when implemented
    // private final BookshelfService bookshelfService;

    @Operation(
            summary = "Add book to bookshelf",
            description = "Add a book to the user's bookshelf with a specific status (to_read, reading, read)"
    )
    @PostMapping
    public ResponseEntity<?> addBookToBookshelf(
            @Valid @RequestBody BookshelfRequestDTO request
    ) {
        // TODO: Implement service call
        // bookshelfService.addBook(currentUserId, request.getBookId(), request.getStatus());
        // return ResponseEntity.ok(Map.of("message", "Book added to bookshelf"));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Update book status",
            description = "Change the status of a book in the bookshelf"
    )
    @PatchMapping("/{bookID}")
    public ResponseEntity<?> updateBookStatus(
            @Parameter(description = "MongoDB ObjectId of the book", example = "65b3f...")
            @PathVariable String bookID,
            @Parameter(description = "New status", example = "read", schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"to_read", "reading", "read"}))
            @RequestBody String status
    ) {
        // TODO: Implement service call
        // bookshelfService.updateBookStatus(currentUserId, bookID, status);
        // return ResponseEntity.ok(Map.of("message", "Book status updated"));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Remove book from bookshelf",
            description = "Delete a book from the user's bookshelf"
    )
    @DeleteMapping("/{bookId}")
    public ResponseEntity<?> removeBookFromBookshelf(
            @Parameter(description = "MongoDB ObjectId of the book", example = "65b3f...")
            @PathVariable String bookId
    ) {
        // TODO: Implement service call
        // bookshelfService.removeBook(currentUserId, bookId);
        // return ResponseEntity.noContent().build();
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
