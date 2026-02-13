package it.unipi.bookSphere.registered;

import it.unipi.bookSphere.dto.UserDTO;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.service.registered.FollowService;
import it.unipi.bookSphere.utils.UserPrincipal;
import it.unipi.bookSphere.TestProfile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional Test for Follow APIs (REGISTERED - Requires authentication)
 * Tests:
 * - Follow user (POST /api/v1/me/follow)
 * - Unfollow user (DELETE /api/v1/me/unfollow/{userId})
 * - Get followed users (GET /api/v1/me/friends)
 * - Neo4j FOLLOWS relationship management
 * - Pagination tests
 * - Duplicate follow handling
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FollowControllerTest {

    @Autowired
    private FollowService followService;

    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private UserNodeRepository userNodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_PREFIX = "FollowTest_";
    private static String testUserId;
    private static String testUser2Id;
    private static String testUser3Id;

    @Test
    @Order(1)
    @DisplayName("01. Cleanup - Remove previous test data")
    void test01_Cleanup() {
        System.out.println("\n=== TEST 01: Cleanup ===");

        // Remove test users
        userRepository.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().startsWith(TEST_PREFIX))
                .forEach(user -> {
                    userRepository.deleteById(user.getId());
                    userNodeRepository.deleteByMongoId(user.getId());
                    System.out.println("Deleted test user: " + user.getUsername());
                });

        System.out.println("Cleanup completed");
    }

    @Test
    @Order(2)
    @DisplayName("02. Setup - Create test users and authenticate")
    void test02_Setup() {
        System.out.println("\n=== TEST 02: Setup Test Data ===");

        // Create test user 1 (the follower)
        RegisteredUser user1 = new RegisteredUser();
        user1.setUsername(TEST_PREFIX + "User1");
        user1.setEmail(TEST_PREFIX + "user1@test.com");
        user1.setPasswordHashed(passwordEncoder.encode("password"));
        user1.setCountry("IT");
        user1.setStatus("active");
        user1.setReviews(new ArrayList<>());
        user1.setBookshelf(new ArrayList<>());
        user1 = userRepository.save(user1);
        testUserId = user1.getId();
        System.out.println("Created test user 1: " + user1.getUsername());

        // Create user node in Neo4j
        userNodeRepository.getOrCreate(testUserId, user1.getUsername(), user1.getCountry());

        // Create test user 2 (to be followed)
        RegisteredUser user2 = new RegisteredUser();
        user2.setUsername(TEST_PREFIX + "User2");
        user2.setEmail(TEST_PREFIX + "user2@test.com");
        user2.setPasswordHashed(passwordEncoder.encode("password"));
        user2.setCountry("US");
        user2.setStatus("active");
        user2.setReviews(new ArrayList<>());
        user2.setBookshelf(new ArrayList<>());
        user2 = userRepository.save(user2);
        testUser2Id = user2.getId();
        System.out.println("Created test user 2: " + user2.getUsername());

        // Create user node in Neo4j
        userNodeRepository.getOrCreate(testUser2Id, user2.getUsername(), user2.getCountry());

        // Create test user 3 (another user to follow)
        RegisteredUser user3 = new RegisteredUser();
        user3.setUsername(TEST_PREFIX + "User3");
        user3.setEmail(TEST_PREFIX + "user3@test.com");
        user3.setPasswordHashed(passwordEncoder.encode("password"));
        user3.setCountry("UK");
        user3.setStatus("active");
        user3.setReviews(new ArrayList<>());
        user3.setBookshelf(new ArrayList<>());
        user3 = userRepository.save(user3);
        testUser3Id = user3.getId();
        System.out.println("Created test user 3: " + user3.getUsername());

        // Create user node in Neo4j
        userNodeRepository.getOrCreate(testUser3Id, user3.getUsername(), user3.getCountry());

        // Setup authentication for user 1
        setupAuthentication(testUserId, TEST_PREFIX + "User1");
    }

    @Test
    @Order(3)
    @DisplayName("03. Follow user - Success")
    void test03_FollowUser_Success() {
        System.out.println("\n=== TEST 03: Follow User ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User1");

        followService.followUser(testUser2Id);

        // Verify followed users
        Page<UserDTO> followedUsers = followService.getFollowedUsers(0, 20);
        assertNotNull(followedUsers);
        assertTrue(followedUsers.stream().anyMatch(u -> u.getId().equals(testUser2Id)));

        System.out.println("User followed successfully");
    }

    @Test
    @Order(4)
    @DisplayName("04. Follow user - Duplicate (should fail)")
    void test04_FollowUser_Duplicate() {
        System.out.println("\n=== TEST 04: Follow User Duplicate ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User1");

        Exception exception = assertThrows(Exception.class, () -> {
            followService.followUser(testUser2Id);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("05. Follow self (should fail)")
    void test05_FollowSelf() {
        System.out.println("\n=== TEST 05: Follow Self ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User1");

        Exception exception = assertThrows(Exception.class, () -> {
            followService.followUser(testUserId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(6)
    @DisplayName("06. Follow another user - Success")
    void test06_FollowAnotherUser() {
        System.out.println("\n=== TEST 06: Follow Another User ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User1");

        followService.followUser(testUser3Id);

        // Verify both users are followed
        Page<UserDTO> followedUsers = followService.getFollowedUsers(0, 20);
        assertNotNull(followedUsers);
        assertEquals(2, followedUsers.getTotalElements());
        assertTrue(followedUsers.stream().anyMatch(u -> u.getId().equals(testUser2Id)));
        assertTrue(followedUsers.stream().anyMatch(u -> u.getId().equals(testUser3Id)));

        System.out.println("Another user followed successfully");
    }

    @Test
    @Order(7)
    @DisplayName("07. Get followed users - Pagination test")
    void test07_GetFollowedUsers_Pagination() {
        System.out.println("\n=== TEST 07: Get Followed Users Pagination ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User1");

        // Request page with size 1
        Page<UserDTO> page1 = followService.getFollowedUsers(0, 1);
        assertNotNull(page1);
        assertEquals(1, page1.getContent().size());
        assertEquals(2, page1.getTotalElements());
        assertTrue(page1.getTotalPages() >= 2);

        // Request second page
        Page<UserDTO> page2 = followService.getFollowedUsers(1, 1);
        assertNotNull(page2);
        assertEquals(1, page2.getContent().size());

        // Ensure pages contain different users
        assertNotEquals(page1.getContent().get(0).getId(), page2.getContent().get(0).getId());

        System.out.println("Pagination test completed");
    }

    @Test
    @Order(8)
    @DisplayName("08. Unfollow user - Success")
    void test08_UnfollowUser_Success() {
        System.out.println("\n=== TEST 08: Unfollow User ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User1");

        followService.unfollowUser(testUser2Id);

        // Verify user is no longer followed
        Page<UserDTO> followedUsers = followService.getFollowedUsers(0, 20);
        assertNotNull(followedUsers);
        assertEquals(1, followedUsers.getTotalElements());
        assertFalse(followedUsers.stream().anyMatch(u -> u.getId().equals(testUser2Id)));
        assertTrue(followedUsers.stream().anyMatch(u -> u.getId().equals(testUser3Id)));

        System.out.println("User unfollowed successfully");
    }

    @Test
    @Order(9)
    @DisplayName("09. Unfollow user - Non-existent relationship (should fail)")
    void test09_UnfollowUser_NotFollowed() {
        System.out.println("\n=== TEST 09: Unfollow Non-followed User ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User1");

        Exception exception = assertThrows(Exception.class, () -> {
            followService.unfollowUser(testUser2Id); // Already unfollowed in test08
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(10)
    @DisplayName("10. Get followed users - Empty list after unfollow all")
    void test10_GetFollowedUsers_EmptyAfterUnfollowAll() {
        System.out.println("\n=== TEST 10: Empty Followed Users List ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User1");

        // Unfollow remaining user
        followService.unfollowUser(testUser3Id);

        // Verify empty list
        Page<UserDTO> followedUsers = followService.getFollowedUsers(0, 20);
        assertNotNull(followedUsers);
        assertEquals(0, followedUsers.getTotalElements());
        assertTrue(followedUsers.getContent().isEmpty());

        System.out.println("Empty list verified");
    }

    @Test
    @Order(11)
    @DisplayName("11. Follow non-existent user (should fail)")
    void test11_FollowNonExistentUser() {
        System.out.println("\n=== TEST 11: Follow Non-existent User ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User1");

        String fakeUserId = "000000000000000000000000";

        Exception exception = assertThrows(Exception.class, () -> {
            followService.followUser(fakeUserId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        if (testUserId != null) {
            userRepository.deleteById(testUserId);
            userNodeRepository.deleteByMongoId(testUserId);
            System.out.println("Deleted test user 1");
        }

        if (testUser2Id != null) {
            userRepository.deleteById(testUser2Id);
            userNodeRepository.deleteByMongoId(testUser2Id);
            System.out.println("Deleted test user 2");
        }

        if (testUser3Id != null) {
            userRepository.deleteById(testUser3Id);
            userNodeRepository.deleteByMongoId(testUser3Id);
            System.out.println("Deleted test user 3");
        }

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
