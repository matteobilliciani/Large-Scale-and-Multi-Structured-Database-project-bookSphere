package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.UserDTO;
import it.unipi.bookSphere.service.FollowService;
import it.unipi.bookSphere.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Follow (Registered)", description = "User follow/unfollow endpoints")
public class FollowController {

    private final FollowService followService;

    @Operation(
            summary = "Follow a user",
            description = "Start following another user by their user ID. Creates FOLLOWS relationship in Neo4j."
    )
    @PostMapping("/follow")
    public ResponseEntity<Map<String, String>> followUser(
            @Parameter(description = "User ID to follow (MongoDB ObjectId)")
            @RequestBody Map<String, String> requestBody
    ) {
        String userId = ValidationUtils.extractAndValidateField(requestBody, "userId");
        ValidationUtils.validateObjectId(userId, "userId");
        followService.followUser(userId);
        return ResponseEntity.ok(Map.of("message", "Successfully followed user"));
    }

    @Operation(
            summary = "Unfollow a user",
            description = "Stop following a user. Removes FOLLOWS relationship in Neo4j."
    )
    @DeleteMapping("/unfollow/{userId}")
    public ResponseEntity<Map<String, String>> unfollowUser(
            @Parameter(description = "User ID to unfollow", example = "65d1...")
            @PathVariable String userId
    ) {
        ValidationUtils.validateObjectId(userId, "userId");
        followService.unfollowUser(userId);
        return ResponseEntity.ok(Map.of("message", "Successfully unfollowed user"));
    }

    @Operation(
            summary = "Get followed users",
            description = "Retrieve all users followed by the current user (friends). Queries Neo4j for FOLLOWS relationships."
    )
    @GetMapping("/friends")
    public ResponseEntity<List<UserDTO>> getFollowedUsers() {
        List<UserDTO> followedUsers = followService.getFollowedUsers();
        return ResponseEntity.ok(followedUsers);
    }
}
