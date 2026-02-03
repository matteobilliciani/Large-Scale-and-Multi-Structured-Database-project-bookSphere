package it.unipi.bookSphere.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.BookDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books (Open)", description = "Public book endpoints accessible to all users")
public class BookController {

    // TODO: Inject BookService when implemented
    // private final BookService bookService;

    @Operation(
            summary = "Get book by ID",
            description = "Retrieve detailed information about a book including snapshot reviews"
    )
    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(
            @Parameter(description = "MongoDB ObjectId of the book", example = "65b3f...")
            @PathVariable String id
    ) {
        // TODO: Implement service call
        // BookDTO book = bookService.findById(id);
        // return ResponseEntity.ok(book);
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Search book by title",
            description = "Search for books by title (supports partial matching)"
    )
    @GetMapping("/search")
    public ResponseEntity<?> searchBookByTitle(
            @Parameter(description = "Book title to search", example = "The Fellowship")
            @RequestParam String title
    ) {
        // TODO: Implement service call
        // List<BookDTO> books = bookService.searchByTitle(title);
        // return ResponseEntity.ok(books);
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
