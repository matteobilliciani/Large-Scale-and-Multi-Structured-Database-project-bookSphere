package it.unipi.bookSphere.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.service.AuthorService;
import it.unipi.bookSphere.validation.ValidObjectId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authors (Open)", description = "Public author endpoints accessible to all users")
public class AuthorController {

    private final AuthorService authorService;

    @Operation(
            summary = "Get author by ID",
            description = "Retrieve author profile and published works"
    )
    @GetMapping("/{id}")
    public ResponseEntity<AuthorDTO> getAuthorById(
            @Parameter(description = "MongoDB ObjectId of the author", example = "65b3a...")
            @ValidObjectId
            @PathVariable String id
    ) {
        AuthorDTO author = authorService.findById(id);
        return ResponseEntity.ok(author);
    }

    @Operation(
            summary = "Search author by name",
            description = "Search for authors by name (supports partial matching). Returns paginated results."
    )
    @GetMapping
    public ResponseEntity<Page<AuthorDTO>> searchAuthorByName(
            @Parameter(description = "Author name to search", example = "Tolkien")
            @RequestParam(name = "author_name", required = false) String authorName,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Page<AuthorDTO> authors = authorService.searchByName(authorName, page, size);
        return ResponseEntity.ok(authors);
    }
}
