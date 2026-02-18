package it.unipi.bookSphere.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.BookDTO;
import it.unipi.bookSphere.service.open.BookService;
import it.unipi.bookSphere.validation.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Tag(name = "Books (Open)", description = "Public book endpoints accessible to all users")
public class BookController {

    private final BookService bookService;

    @Operation(
            summary = "Get book by ID",
            description = "Retrieve detailed information about a book including snapshot reviews"
    )
    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getBookById(
            @Parameter(description = "MongoDB ObjectId of the book", example = "65b3f...")
            @PathVariable String id
    ) {
        ValidationUtils.validateObjectId(id, "id");
        BookDTO book = bookService.findById(id);
        return ResponseEntity.ok(book);
    }

    @Operation(
            summary = "Search book by title",
            description = "Search for books by title (supports partial matching). Returns paginated results."
    )
    @GetMapping
    public ResponseEntity<Page<BookDTO>> searchBookByTitle(
            @Parameter(description = "Book title to search", example = "The Fellowship")
            @RequestParam(name = "title", required = false) String title,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Page<BookDTO> books = bookService.searchByTitle(title, page, size);
        return ResponseEntity.ok(books);
    }
}
