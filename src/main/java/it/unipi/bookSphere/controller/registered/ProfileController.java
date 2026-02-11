package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.service.ProfileService;
import it.unipi.bookSphere.validation.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Profile (Registered)", description = "User profile management endpoints")
public class ProfileController {

    private final ProfileService profileService;

    @Operation(
            summary = "Update username",
            description = "Change the user's username. Updates both MongoDB and Neo4j."
    )
    @PatchMapping("/username")
    public ResponseEntity<Map<String, String>> updateUsername(
            @Parameter(description = "New username", example = "NewUsername123")
            @RequestBody Map<String, String> requestBody
    ) {
        String newUsername = ValidationUtils.extractAndValidateField(requestBody, "username");
        ValidationUtils.validateUsername(newUsername);
        profileService.updateUsername(newUsername);
        return ResponseEntity.ok(Map.of("message", "Username updated successfully", "newUsername", newUsername));
    }

    @Operation(
            summary = "Delete account",
            description = "Permanently delete the user account. Removes data from both MongoDB and Neo4j."
    )
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount() {
        profileService.deleteAccount();
        return ResponseEntity.noContent().build();
    }
}
