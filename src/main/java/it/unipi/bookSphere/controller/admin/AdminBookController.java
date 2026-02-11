package it.unipi.bookSphere.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.BookDTO;
import it.unipi.bookSphere.service.AdminBookService;
import it.unipi.bookSphere.validation.ValidationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/books")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Books", description = "Admin endpoints for book catalog management")
public class AdminBookController {

    private final AdminBookService adminBookService;

    @Operation(
            summary = "Add new book",
            description = "Add a new book to the catalog. Creates entries in both MongoDB and Neo4j."
    )
    @PostMapping
    public ResponseEntity<?> addBook(
            @Valid @RequestBody BookDTO bookDTO
    ) {
        BookDTO created = adminBookService.addBook(bookDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Update book",
            description = "Update book information. Updates both MongoDB and Neo4j."
    )
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBook(
            @Parameter(description = "MongoDB ObjectId of the book", example = "65b3f...")
            @PathVariable String id,
            @Valid @RequestBody BookDTO bookDTO
    ) {
        ValidationUtils.validateObjectId(id, "id");
        BookDTO updated = adminBookService.updateBook(id, bookDTO);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete book",
            description = "Remove a book from the system. Deletes from both MongoDB and Neo4j, including all related reviews and relationships."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(
            @Parameter(description = "MongoDB ObjectId of the book", example = "65b3f...")
            @PathVariable String id
    ) {
        ValidationUtils.validateObjectId(id, "id");
        adminBookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}

