package it.unipi.bookSphere.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.unipi.bookSphere.dto.AuthResponseDTO;
import it.unipi.bookSphere.dto.LoginDTO;
import it.unipi.bookSphere.dto.RegisterDTO;
import it.unipi.bookSphere.dto.UserDTO;
import it.unipi.bookSphere.service.AuthService;
import it.unipi.bookSphere.utils.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication (Open)", description = "User authentication endpoints")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @Operation(
            summary = "Register new user",
            description = "Create a new user account and return JWT token"
    )
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterDTO registerDTO
    ) {
        // Create new user account
        UserDTO user = authService.register(registerDTO);
        
        // Generate JWT token with status (new users are always "active")
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), "USER", "active");
        
        // Return response with token and user info
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponseDTO(token, user.getId(), user.getUsername(), "USER"));
    }

    @Operation(
            summary = "User login",
            description = "Authenticate user and return JWT token"
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginDTO loginDTO
    ) {
        // Authenticate user (AuthService verifies status is "active")
        UserDTO user = authService.login(loginDTO);
        
        // Generate JWT token with status
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), "USER", user.getStatus());
        
        // Return response with token and user info
        return ResponseEntity.ok(new AuthResponseDTO(token, user.getId(), user.getUsername(), "USER"));
    }
}
