package it.unipi.bookSphere.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.dto.GenreDTO;
import it.unipi.bookSphere.service.AdminCatalogService;
import it.unipi.bookSphere.utils.ValidationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Catalog", description = "Admin endpoints for author and genre management")
public class AdminCatalogController {

    private final AdminCatalogService adminCatalogService;

    // ========== AUTHOR MANAGEMENT ==========
    
    @Operation(
            summary = "Add new author",
            description = "Add a new author to the system. Creates entries in both MongoDB and Neo4j."
    )
    @PostMapping("/authors")
    public ResponseEntity<?> addAuthor(
            @Valid @RequestBody AuthorDTO authorDTO
    ) {
        AuthorDTO created = adminCatalogService.addAuthor(authorDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
            summary = "Update author",
            description = "Update author information. Updates both MongoDB and Neo4j."
    )
    @PutMapping("/authors/{id}")
    public ResponseEntity<?> updateAuthor(
            @Parameter(description = "MongoDB ObjectId of the author", example = "65b3a...")
            @PathVariable String id,
            @Valid @RequestBody AuthorDTO authorDTO
    ) {
        ValidationUtils.validateObjectId(id, "id");
        AuthorDTO updated = adminCatalogService.updateAuthor(id, authorDTO);
        return ResponseEntity.ok(updated);
    }

    @Operation(
            summary = "Delete author",
            description = "Remove an author from the system. Deletes from both MongoDB and Neo4j, including all related books and relationships."
    )
    @DeleteMapping("/authors/{id}")
    public ResponseEntity<?> deleteAuthor(
            @Parameter(description = "MongoDB ObjectId of the author", example = "65b3a...")
            @PathVariable String id
    ) {
        ValidationUtils.validateObjectId(id, "id");
        adminCatalogService.deleteAuthor(id);
        return ResponseEntity.noContent().build();
    }

    // ========== GENRE MANAGEMENT ==========
    
    @Operation(
            summary = "Add new genre",
            description = "Add a new genre to the system. Creates entry in Neo4j."
    )
    @PostMapping("/genres")
    public ResponseEntity<?> addGenre(
            @Valid @RequestBody GenreDTO genreDTO
    ) {
        GenreDTO created = adminCatalogService.addGenre(genreDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
