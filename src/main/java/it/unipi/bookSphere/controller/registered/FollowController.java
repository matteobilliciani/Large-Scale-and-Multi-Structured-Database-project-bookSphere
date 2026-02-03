package it.unipi.bookSphere.controller.registered;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/follow")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Follow (Registered)", description = "User follow/unfollow endpoints")
public class FollowController {

    // TODO: Inject FollowService when implemented
    // private final FollowService followService;

    @Operation(
            summary = "Follow a user",
            description = "Start following another user. Creates FOLLOWS relationship in Neo4j."
    )
    @PostMapping("/{username}")
    public ResponseEntity<?> followUser(
            @Parameter(description = "Username of the user to follow", example = "User_12345")
            @PathVariable String username
    ) {
        // TODO: Implement service call
        // followService.followUser(currentUserId, username);
        // return ResponseEntity.ok(Map.of("message", "Successfully followed " + username));
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "Unfollow a user",
            description = "Stop following a user. Removes FOLLOWS relationship in Neo4j."
    )
    @DeleteMapping("/{username}")
    public ResponseEntity<?> unfollowUser(
            @Parameter(description = "Username of the user to unfollow", example = "User_12345")
            @PathVariable String username
    ) {
        // TODO: Implement service call
        // followService.unfollowUser(currentUserId, username);
        // return ResponseEntity.ok(Map.of("message", "Successfully unfollowed " + username));
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
