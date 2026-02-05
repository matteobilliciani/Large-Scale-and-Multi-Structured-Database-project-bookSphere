package it.unipi.bookSphere.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.UserDTO;
import it.unipi.bookSphere.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users (Open)", description = "Public user profile endpoints")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get user by username",
            description = "View public user profile and activity by username"
    )
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDTO> getUserByUsername(
            @Parameter(description = "Username", example = "User_12345")
            @PathVariable String username
    ) {
        UserDTO user = userService.findByUsername(username);
        return ResponseEntity.ok(user);
    }

    @Operation(
            summary = "Get user by ID",
            description = "View public user profile and activity by MongoDB ObjectId"
    )
    @GetMapping("/id/{id}")
    public ResponseEntity<UserDTO> getUserById(
            @Parameter(description = "MongoDB ObjectId of the user", example = "65d1...")
            @PathVariable String id
    ) {
        UserDTO user = userService.findById(id);
        return ResponseEntity.ok(user);
    }
}
