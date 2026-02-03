package it.unipi.bookSphere.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.AuthResponseDTO;
import it.unipi.bookSphere.dto.LoginDTO;
import it.unipi.bookSphere.dto.RegisterDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication (Open)", description = "User authentication endpoints")
public class AuthController {

    // TODO: Inject AuthService when implemented
    // private final AuthService authService;

    @Operation(
            summary = "Register new user",
            description = "Create a new user account and return JWT token"
    )
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterDTO registerDTO
    ) {
        // TODO: Implement service call
        // The service should:
        // 1. Validate that username/email doesn't exist
        // 2. Hash the password using BCryptPasswordEncoder
        // 3. Create user in MongoDB with default role "USER"
        // 4. Create user node in Neo4j
        // 5. Generate JWT token using JwtUtil
        // 6. Return AuthResponseDTO with token and user info
        
        // Example implementation:
        // UserDTO user = authService.register(registerDTO);
        // String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        // return ResponseEntity.status(HttpStatus.CREATED)
        //     .body(new AuthResponseDTO(token, user.getId(), user.getUsername(), user.getRole()));
        
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "User login",
            description = "Authenticate user and return JWT token"
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginDTO loginDTO
    ) {
        // TODO: Implement service call
        // The service should:
        // 1. Find user by username/email
        // 2. Verify password using BCryptPasswordEncoder
        // 3. Generate JWT token using JwtUtil
        // 4. Return AuthResponseDTO with token and user info
        
        // Example implementation:
        // UserDTO user = authService.login(loginDTO);
        // String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        // return ResponseEntity.ok(new AuthResponseDTO(token, user.getId(), user.getUsername(), user.getRole()));
        
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
