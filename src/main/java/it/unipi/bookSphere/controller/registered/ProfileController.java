package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Profile (Registered)", description = "User profile management endpoints")
public class ProfileController {

    // TODO: Inject ProfileService when implemented
    // private final ProfileService profileService;

    @Operation(
            summary = "Update username",
            description = "Change the user's username. Updates both MongoDB and Neo4j."
    )
    @PatchMapping("/username")
    public ResponseEntity<?> updateUsername(
            @Parameter(description = "New username", example = "NewUsername123")
            @RequestBody String newUsername
    ) {
        // TODO: Implement service call
        // Get current user ID from JWT token
        // String currentUserId = SecurityUtils.getCurrentUserId();
        // profileService.updateUsername(currentUserId, newUsername);
        // return ResponseEntity.ok(Map.of("message", "Username updated successfully", "newUsername", newUsername));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Delete account",
            description = "Permanently delete the user account. Removes data from both MongoDB and Neo4j."
    )
    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount() {
        // TODO: Implement service call
        // Get current user ID from JWT token
        // String currentUserId = SecurityUtils.getCurrentUserId();
        // profileService.deleteAccount(currentUserId);
        // return ResponseEntity.noContent().build();
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
