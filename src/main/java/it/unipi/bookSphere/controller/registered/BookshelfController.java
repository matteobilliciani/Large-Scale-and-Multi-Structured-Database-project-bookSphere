package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.BookshelfRequestDTO;
import it.unipi.bookSphere.service.registered.BookshelfService;
import it.unipi.bookSphere.validation.ValidationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/me/bookshelf")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bookshelf (Registered)", description = "User bookshelf management endpoints")
public class BookshelfController {

    private final BookshelfService bookshelfService;

    @Operation(
            summary = "Add book to bookshelf",
            description = "Add a book to the user's bookshelf with a specific status (to_read, reading, read)"
    )
    @PostMapping
    public ResponseEntity<Map<String, String>> addBookToBookshelf(
            @Valid @RequestBody BookshelfRequestDTO request
    ) {
        bookshelfService.addBookToBookshelf(request.getBookId(), request.getStatus());
        return ResponseEntity.ok(Map.of("message", "Book added to bookshelf"));
    }

    @Operation(
            summary = "Update book status",
            description = "Change the status of a book in the bookshelf"
    )
    @PatchMapping("/{bookID}")
    public ResponseEntity<Map<String, String>> updateBookStatus(
            @Parameter(description = "MongoDB ObjectId of the book", example = "65b3f...")
            @PathVariable String bookID,
            @Parameter(description = "New status", example = "read")
            @RequestBody Map<String, String> requestBody
    ) {
        ValidationUtils.validateObjectId(bookID, "bookID");
        String status = ValidationUtils.extractAndValidateField(requestBody, "status");
        ValidationUtils.validateBookshelfStatus(status);
        bookshelfService.updateBookStatus(bookID, status);
        return ResponseEntity.ok(Map.of("message", "Book status updated"));
    }

    @Operation(
            summary = "Remove book from bookshelf",
            description = "Delete a book from the user's bookshelf"
    )
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeBookFromBookshelf(
            @Parameter(description = "MongoDB ObjectId of the book", example = "65b3f...")
            @PathVariable String bookId
    ) {
        ValidationUtils.validateObjectId(bookId, "bookId");
        bookshelfService.removeBookFromBookshelf(bookId);
        return ResponseEntity.noContent().build();
    }
}
