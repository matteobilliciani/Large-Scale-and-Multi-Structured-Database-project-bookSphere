package it.unipi.bookSphere.open;

import it.unipi.bookSphere.dto.UserDTO;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.service.open.UserService;
import it.unipi.bookSphere.TestProfile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional Test for User APIs (OPEN - No authentication required)
 * Tests:
 * - Get user by username (GET /api/v1/users/username/{username})
 * - Get user by ID (GET /api/v1/users/id/{id})
 * - Error handling for non-existent users
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserControllerTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private UserNodeRepository userNodeRepository;

    private static final String TEST_PREFIX = "UserTest_";
    private static String testUserId1;
    private static String testUserId2;

    @Test
    @Order(1)
    @DisplayName("01. Cleanup - Remove previous test data")
    void test01_Cleanup() {
        System.out.println("\n=== TEST 01: Cleanup ===");

        // Remove all test users from MongoDB
        userRepository.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().startsWith(TEST_PREFIX))
                .forEach(user -> {
                    userRepository.deleteById(user.getId());
                    System.out.println("Deleted test user: " + user.getUsername());
                });

        // Remove all test users from Neo4j
        userNodeRepository.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().startsWith(TEST_PREFIX))
                .forEach(user -> {
                    userNodeRepository.deleteByMongoId(user.getMongoId());
                    System.out.println("Deleted test user node: " + user.getUsername());
                });

        System.out.println("Cleanup completed");
    }

    @Test
    @Order(2)
    @DisplayName("02. Setup - Create test data")
    void test02_Setup() {
        System.out.println("\n=== TEST 02: Setup Test Data ===");

        // Create test user 1
        RegisteredUser user1 = new RegisteredUser();
        user1.setUsername(TEST_PREFIX + "User1");
        user1.setEmail(TEST_PREFIX + "user1@test.com");
        user1.setPasswordHashed("hashedpassword");
        user1.setCountry("IT");
        user1.setStatus("active");
        user1.setJoinedAt(Instant.now());
        user1.setReviews(new ArrayList<>());
        user1.setBookshelf(new ArrayList<>());
        user1 = userRepository.save(user1);
        testUserId1 = user1.getId();
        System.out.println("Created test user 1: " + user1.getUsername());

        // Create test user 2
        RegisteredUser user2 = new RegisteredUser();
        user2.setUsername(TEST_PREFIX + "User2");
        user2.setEmail(TEST_PREFIX + "user2@test.com");
        user2.setPasswordHashed("hashedpassword");
        user2.setCountry("US");
        user2.setStatus("active");
        user2.setJoinedAt(Instant.now());
        user2.setReviews(new ArrayList<>());
        user2.setBookshelf(new ArrayList<>());
        user2 = userRepository.save(user2);
        testUserId2 = user2.getId();
        System.out.println("Created test user 2: " + user2.getUsername());
    }

    @Test
    @Order(3)
    @DisplayName("03. Get user by ID - Success")
    void test03_GetUserById_Success() {
        System.out.println("\n=== TEST 03: Get User by ID ===");

        UserDTO user = userService.findById(testUserId1);

        assertNotNull(user);
        assertEquals(testUserId1, user.getId());
        assertEquals(TEST_PREFIX + "User1", user.getUsername());
        assertEquals("IT", user.getCountry());
        assertEquals("active", user.getStatus());

        System.out.println("User retrieved by ID: " + user.getUsername());
    }

    @Test
    @Order(4)
    @DisplayName("04. Get user by ID - Not found")
    void test04_GetUserById_NotFound() {
        System.out.println("\n=== TEST 04: Get Non-existent User by ID ===");

        String fakeId = "000000000000000000000000";

        Exception exception = assertThrows(Exception.class, () -> {
            userService.findById(fakeId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("05. Get user by username - Success")
    void test05_GetUserByUsername_Success() {
        System.out.println("\n=== TEST 05: Get User by Username ===");

        UserDTO user = userService.findByUsername(TEST_PREFIX + "User1");

        assertNotNull(user);
        assertEquals(testUserId1, user.getId());
        assertEquals(TEST_PREFIX + "User1", user.getUsername());
        assertEquals("IT", user.getCountry());

        System.out.println("User retrieved by username: " + user.getUsername());
    }

    @Test
    @Order(6)
    @DisplayName("06. Get user by username - Not found")
    void test06_GetUserByUsername_NotFound() {
        System.out.println("\n=== TEST 06: Get Non-existent User by Username ===");

        String fakeUsername = TEST_PREFIX + "NonExistentUser";

        Exception exception = assertThrows(Exception.class, () -> {
            userService.findByUsername(fakeUsername);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(7)
    @DisplayName("07. Get user by username - Case sensitivity")
    void test07_GetUserByUsername_CaseSensitive() {
        System.out.println("\n=== TEST 07: Get User by Username - Case Sensitivity ===");

        // Try with lowercase (should work if system is case-insensitive or fail if case-sensitive)
        String lowercaseUsername = (TEST_PREFIX + "User1").toLowerCase();

        try {
            UserDTO user = userService.findByUsername(lowercaseUsername);
            System.out.println("System is case-insensitive: found user " + user.getUsername());
        } catch (Exception e) {
            System.out.println("System is case-sensitive: user not found with lowercase username");
            // This is expected behavior in case-sensitive systems
        }
    }

    @Test
    @Order(8)
    @DisplayName("08. Get multiple users - Verify independence")
    void test08_GetMultipleUsers_VerifyIndependence() {
        System.out.println("\n=== TEST 08: Get Multiple Users ===");

        UserDTO user1 = userService.findById(testUserId1);
        UserDTO user2 = userService.findById(testUserId2);

        assertNotNull(user1);
        assertNotNull(user2);
        assertNotEquals(user1.getId(), user2.getId());
        assertNotEquals(user1.getUsername(), user2.getUsername());
        assertNotEquals(user1.getEmail(), user2.getEmail());

        System.out.println("Retrieved 2 independent users successfully");
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        // Remove test users from MongoDB
        if (testUserId1 != null) {
            userRepository.deleteById(testUserId1);
            System.out.println("Deleted test user 1 from MongoDB");
        }
        if (testUserId2 != null) {
            userRepository.deleteById(testUserId2);
            System.out.println("Deleted test user 2 from MongoDB");
        }

        // Remove test users from Neo4j
        if (testUserId1 != null) {
            userNodeRepository.deleteByMongoId(testUserId1);
            System.out.println("Deleted test user 1 from Neo4j");
        }
        if (testUserId2 != null) {
            userNodeRepository.deleteByMongoId(testUserId2);
            System.out.println("Deleted test user 2 from Neo4j");
        }

        System.out.println("Final cleanup completed - All test data removed");
    }
}
