package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Follow (Registered)", description = "User follow/unfollow endpoints")
public class FollowController {

    // TODO: Inject FollowService when implemented
    // private final FollowService followService;

    @Operation(
            summary = "Follow a user",
            description = "Start following another user. Creates FOLLOWS relationship in Neo4j. Accepts username or user ID in request body."
    )
    @PostMapping("/follow")
    public ResponseEntity<?> followUser(
            @Parameter(description = "Username or user ID to follow")
            @RequestBody String userIdentifier
    ) {
        // TODO: Implement service call
        // followService.followUser(currentUserId, userIdentifier);
        // return ResponseEntity.ok(Map.of("message", "Successfully followed " + userIdentifier));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Unfollow a user",
            description = "Stop following a user. Removes FOLLOWS relationship in Neo4j."
    )
    @DeleteMapping("/unfollow/{userId}")
    public ResponseEntity<?> unfollowUser(
            @Parameter(description = "User ID to unfollow", example = "65d1...")
            @PathVariable String userId
    ) {
        // TODO: Implement service call
        // followService.unfollowUser(currentUserId, userId);
        // return ResponseEntity.ok(Map.of("message", "Successfully unfollowed user"));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Get followed users",
            description = "Retrieve all users followed by the current user (friends). Queries Neo4j for FOLLOWS relationships."
    )
    @GetMapping("/friends")
    public ResponseEntity<?> getFollowedUsers() {
        // TODO: Implement service call
        // List<UserDTO> followedUsers = followService.getFollowedUsers(currentUserId);
        // return ResponseEntity.ok(followedUsers);
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
