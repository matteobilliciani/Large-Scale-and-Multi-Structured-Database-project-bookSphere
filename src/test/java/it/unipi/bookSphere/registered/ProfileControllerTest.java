package it.unipi.bookSphere.registered;

import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.service.ProfileService;
import it.unipi.bookSphere.utils.UserPrincipal;
import it.unipi.bookSphere.TestProfile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional Test for Profile APIs (REGISTERED - Requires authentication)
 * Tests:
 * - Update username (PATCH /api/v1/me/username)
 * - Delete account (DELETE /api/v1/me/account)
 * - MongoDB + Neo4j consistency
 * - Validation checks
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProfileControllerTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private UserNodeRepository userNodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_PREFIX = "ProfileTest_";
    private static String testUserId;
    private static String originalUsername;

    @Test
    @Order(1)
    @DisplayName("01. Cleanup - Remove previous test data")
    void test01_Cleanup() {
        System.out.println("\n=== TEST 01: Cleanup ===");

        // Remove test users by username prefix
        userRepository.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().toLowerCase().startsWith(TEST_PREFIX.toLowerCase()))
                .forEach(user -> {
                    userRepository.deleteById(user.getId());
                    userNodeRepository.deleteByMongoId(user.getId());
                    System.out.println("Deleted test user: " + user.getUsername());
                });

        // Also remove by email to catch users whose username was changed
        userRepository.findByEmail(TEST_PREFIX + "user@test.com").ifPresent(user -> {
            userRepository.deleteById(user.getId());
            userNodeRepository.deleteByMongoId(user.getId());
            System.out.println("Deleted test user by email: " + user.getEmail());
        });

        System.out.println("Cleanup completed");
    }

    @Test
    @Order(2)
    @DisplayName("02. Setup - Create test user and authenticate")
    void test02_Setup() {
        System.out.println("\n=== TEST 02: Setup Test Data ===");

        // Create test user
        RegisteredUser user = new RegisteredUser();
        originalUsername = TEST_PREFIX + "User";
        user.setUsername(originalUsername.toLowerCase());
        user.setEmail(TEST_PREFIX + "user@test.com");
        user.setPasswordHashed(passwordEncoder.encode("password"));
        user.setCountry("IT");
        user.setStatus("active");
        user.setReviews(new ArrayList<>());
        user.setBookshelf(new ArrayList<>());
        user = userRepository.save(user);
        testUserId = user.getId();
        System.out.println("Created test user: " + user.getUsername());

        // Create user node in Neo4j
        userNodeRepository.getOrCreate(testUserId, user.getUsername(), user.getCountry());

        // Setup authentication
        setupAuthentication(testUserId, originalUsername);
    }

    @Test
    @Order(3)
    @DisplayName("03. Update username - Success")
    void test03_UpdateUsername_Success() {
        System.out.println("\n=== TEST 03: Update Username ===");

        // Re-setup authentication
        setupAuthentication(testUserId, originalUsername);

        String newUsername = TEST_PREFIX + "UpdatedUser";
        profileService.updateUsername(newUsername);

        // Verify in MongoDB
        RegisteredUser userMongo = userRepository.findById(testUserId).orElse(null);
        assertNotNull(userMongo);
        assertEquals(newUsername.toLowerCase(), userMongo.getUsername());

        // Verify in Neo4j
        var userNodeOpt = userNodeRepository.findByMongoId(testUserId);
        assertTrue(userNodeOpt.isPresent());
        assertEquals(newUsername.toLowerCase(), userNodeOpt.get().getUsername());

        // Update authentication for next tests
        setupAuthentication(testUserId, newUsername);

        System.out.println("Username updated successfully to: " + newUsername);
    }

    @Test
    @Order(4)
    @DisplayName("04. Update username - Invalid format (should fail)")
    void test04_UpdateUsername_InvalidFormat() {
        System.out.println("\n=== TEST 04: Update Username - Invalid Format ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "UpdatedUser");

        // Empty username
        Exception exception1 = assertThrows(Exception.class, () -> {
            profileService.updateUsername("");
        });
        System.out.println("Expected error for empty username: " + exception1.getMessage());

        // Username with spaces
        Exception exception2 = assertThrows(Exception.class, () -> {
            profileService.updateUsername("User Name With Spaces");
        });
        System.out.println("Expected error for username with spaces: " + exception2.getMessage());

        // Username too short
        Exception exception3 = assertThrows(Exception.class, () -> {
            profileService.updateUsername("ab");
        });
        System.out.println("Expected error for short username: " + exception3.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("05. Update username - Duplicate (should fail)")
    void test05_UpdateUsername_Duplicate() {
        System.out.println("\n=== TEST 05: Update Username - Duplicate ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "UpdatedUser");

        // Create another user
        RegisteredUser anotherUser = new RegisteredUser();
        String existingUsername = TEST_PREFIX + "ExistingUser";
        anotherUser.setUsername(existingUsername.toLowerCase());
        anotherUser.setEmail(TEST_PREFIX + "existing@test.com");
        anotherUser.setPasswordHashed(passwordEncoder.encode("password"));
        anotherUser.setCountry("IT");
        anotherUser.setStatus("active");
        anotherUser.setReviews(new ArrayList<>());
        anotherUser.setBookshelf(new ArrayList<>());
        RegisteredUser savedUser = userRepository.save(anotherUser);

        // Try to update to existing username
        Exception exception = assertThrows(Exception.class, () -> {
            profileService.updateUsername(existingUsername);
        });

        System.out.println("Expected error caught: " + exception.getMessage());

        // Cleanup the temporary user
        userRepository.deleteById(savedUser.getId());
        userNodeRepository.deleteByMongoId(savedUser.getId());
    }

    @Test
    @Order(6)
    @DisplayName("06. Delete account - Success")
    void test06_DeleteAccount_Success() {
        System.out.println("\n=== TEST 06: Delete Account ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "UpdatedUser");

        // Delete the account
        profileService.deleteAccount();

        // Wait for async operations
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify in MongoDB
        RegisteredUser userMongo = userRepository.findById(testUserId).orElse(null);
        assertNotNull(userMongo, "User should still exist in MongoDB");
        assertEquals("deleted", userMongo.getStatus(), "Status should be 'deleted'");
        assertNull(userMongo.getUsername(), "Username should be null");
        assertNull(userMongo.getEmail(), "Email should be null");

        // Verify in Neo4j - User node should be deleted
        var userNodeOpt = userNodeRepository.findByMongoId(testUserId);
        assertFalse(userNodeOpt.isPresent(), "User node should be deleted from Neo4j");

        System.out.println("Account deleted successfully");
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        // Clean up any remaining test users
        userRepository.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().startsWith(TEST_PREFIX))
                .forEach(user -> {
                    userRepository.deleteById(user.getId());
                    userNodeRepository.deleteByMongoId(user.getId());
                    System.out.println("Deleted test user: " + user.getUsername());
                });

        System.out.println("Final cleanup completed - All test data removed");
    }

    private void setupAuthentication(String userId, String username) {
        UserPrincipal principal = new UserPrincipal(userId, username, "USER", "active");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        System.out.println("Authentication setup for user: " + username);
    }
}
