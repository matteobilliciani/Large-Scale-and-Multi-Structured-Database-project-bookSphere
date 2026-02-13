package it.unipi.bookSphere.open;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.service.open.AuthService;
import it.unipi.bookSphere.TestProfile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional Test for Authentication APIs (OPEN - No authentication required)
 * Tests:
 * - User Registration (POST /api/v1/auth/register)
 * - User Login (POST /api/v1/auth/login)
 * - Validation and error handling
 * - MongoDB + Neo4j consistency
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthControllerTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private UserNodeRepository userNodeRepository;

    private static final String TEST_PREFIX = "AuthTest_";
    private static String testUserId;

    @Test
    @Order(1)
    @DisplayName("01. Cleanup - Remove previous test data")
    void test01_Cleanup() {
        System.out.println("\n=== TEST 01: Cleanup ===");
        
        // Remove all test users from MongoDB
        userRepository.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().toLowerCase().startsWith(TEST_PREFIX.toLowerCase()))
                .forEach(user -> {
                    userRepository.deleteById(user.getId());
                    System.out.println("Deleted test user: " + user.getUsername());
                });

        // Remove all test users from Neo4j
        userNodeRepository.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().toLowerCase().startsWith(TEST_PREFIX.toLowerCase()))
                .forEach(user -> {
                    userNodeRepository.deleteByMongoId(user.getMongoId());
                    System.out.println("Deleted test user node: " + user.getUsername());
                });

        System.out.println("Cleanup completed");
    }

    @Test
    @Order(2)
    @DisplayName("02. Register new user - Success")
    void test02_RegisterUser_Success() {
        System.out.println("\n=== TEST 02: Register User ===");

        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(TEST_PREFIX + "User1");
        registerDTO.setEmail(TEST_PREFIX + "user1@test.com");
        registerDTO.setPassword("SecurePass123!");
        registerDTO.setCountry("IT");

        UserDTO result = authService.register(registerDTO);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(TEST_PREFIX + "User1", result.getUsername());
        assertEquals("IT", result.getCountry());
        assertEquals("active", result.getStatus());

        testUserId = result.getId();

        // Verify in MongoDB
        RegisteredUser userMongo = userRepository.findById(testUserId).orElse(null);
        assertNotNull(userMongo);
        assertEquals(TEST_PREFIX + "User1", userMongo.getUsername());
        assertEquals(TEST_PREFIX + "user1@test.com", userMongo.getEmail());
        assertNotNull(userMongo.getPasswordHashed());

        // Verify in Neo4j
        var userNodeOpt = userNodeRepository.findByMongoId(testUserId);
        assertTrue(userNodeOpt.isPresent());
        var userNode = userNodeOpt.get();
        assertEquals(TEST_PREFIX + "User1", userNode.getUsername());
        assertEquals("IT", userNode.getCountry());

        System.out.println("User registered successfully: " + result.getUsername());
    }

    @Test
    @Order(3)
    @DisplayName("03. Register user - Duplicate username")
    void test03_RegisterUser_DuplicateUsername() {
        System.out.println("\n=== TEST 03: Register with Duplicate Username ===");

        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(TEST_PREFIX + "User1"); // Same as test02
        registerDTO.setEmail(TEST_PREFIX + "different@test.com");
        registerDTO.setPassword("SecurePass123!");
        registerDTO.setCountry("US");

        Exception exception = assertThrows(Exception.class, () -> {
            authService.register(registerDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(4)
    @DisplayName("04. Register user - Duplicate email")
    void test04_RegisterUser_DuplicateEmail() {
        System.out.println("\n=== TEST 04: Register with Duplicate Email ===");

        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(TEST_PREFIX + "DifferentUser");
        registerDTO.setEmail(TEST_PREFIX + "user1@test.com"); // Same as test02
        registerDTO.setPassword("SecurePass123!");
        registerDTO.setCountry("US");

        Exception exception = assertThrows(Exception.class, () -> {
            authService.register(registerDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("05. Login - Success with username")
    void test05_Login_SuccessWithUsername() {
        System.out.println("\n=== TEST 05: Login with Username ===");

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsernameOrEmail(TEST_PREFIX + "User1");
        loginDTO.setPassword("SecurePass123!");

        UserDTO result = authService.login(loginDTO);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(TEST_PREFIX + "User1", result.getUsername());
        assertEquals("active", result.getStatus());

        System.out.println("Login successful: " + result.getUsername());
    }

    @Test
    @Order(6)
    @DisplayName("06. Login - Success with email")
    void test06_Login_SuccessWithEmail() {
        System.out.println("\n=== TEST 06: Login with Email ===");

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsernameOrEmail(TEST_PREFIX + "user1@test.com");
        loginDTO.setPassword("SecurePass123!");

        UserDTO result = authService.login(loginDTO);

        assertNotNull(result);
        assertEquals(testUserId, result.getId());
        assertEquals(TEST_PREFIX + "User1", result.getUsername());

        System.out.println("Login successful with email: " + result.getUsername());
    }

    @Test
    @Order(7)
    @DisplayName("07. Login - Invalid password")
    void test07_Login_InvalidPassword() {
        System.out.println("\n=== TEST 07: Login with Invalid Password ===");

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsernameOrEmail(TEST_PREFIX + "User1");
        loginDTO.setPassword("WrongPassword");

        Exception exception = assertThrows(Exception.class, () -> {
            authService.login(loginDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(8)
    @DisplayName("08. Login - Non-existent user")
    void test08_Login_NonExistentUser() {
        System.out.println("\n=== TEST 08: Login with Non-existent User ===");

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsernameOrEmail(TEST_PREFIX + "NonExistent");
        loginDTO.setPassword("SomePassword");

        Exception exception = assertThrows(Exception.class, () -> {
            authService.login(loginDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        // Remove from MongoDB
        if (testUserId != null) {
            userRepository.deleteById(testUserId);
            System.out.println("Deleted test user from MongoDB: " + testUserId);
        }

        // Remove from Neo4j
        if (testUserId != null) {
            userNodeRepository.deleteByMongoId(testUserId);
            System.out.println("Deleted test user from Neo4j: " + testUserId);
        }

        System.out.println("Final cleanup completed - All test data removed");
    }
}
