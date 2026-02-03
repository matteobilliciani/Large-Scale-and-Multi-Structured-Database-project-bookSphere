package it.unipi.bookSphere.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.AuthorDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authors")
@RequiredArgsConstructor
@Tag(name = "Authors (Open)", description = "Public author endpoints accessible to all users")
public class AuthorController {

    // TODO: Inject AuthorService when implemented
    // private final AuthorService authorService;

    @Operation(
            summary = "Get author by ID",
            description = "Retrieve author profile and published works"
    )
    @GetMapping("/{id}")
    public ResponseEntity<AuthorDTO> getAuthorById(
            @Parameter(description = "MongoDB ObjectId of the author", example = "65b3a...")
            @PathVariable String id
    ) {
        // TODO: Implement service call
        // AuthorDTO author = authorService.findById(id);
        // return ResponseEntity.ok(author);
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Search author by name",
            description = "Search for authors by name (supports partial matching)"
    )
    @GetMapping
    public ResponseEntity<?> searchAuthorByName(
            @Parameter(description = "Author name to search", example = "Tolkien")
            @RequestParam(name = "author_name", required = false) String authorName
    ) {
        // TODO: Implement service call
        // List<AuthorDTO> authors = authorService.searchByName(authorName);
        // return ResponseEntity.ok(authors);
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
