package it.unipi.bookSphere.controller.open;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
            description = "Create a new user account"
    )
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterDTO registerDTO
    ) {
        // TODO: Implement service call
        // UserDTO user = authService.register(registerDTO);
        // return ResponseEntity.status(HttpStatus.CREATED).body(user);
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Operation(
            summary = "User login",
            description = "Authenticate user and return JWT token"
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginDTO loginDTO
    ) {
        // TODO: Implement service call
        // String token = authService.login(loginDTO);
        // return ResponseEntity.ok(Map.of("token", token));
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
