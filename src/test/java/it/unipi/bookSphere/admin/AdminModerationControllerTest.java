package it.unipi.bookSphere.admin;

import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.mongodb.Review;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.mongo.ReviewRepository;
import it.unipi.bookSphere.repository.neo4j.ReviewNodeRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.service.AdminModerationService;
import it.unipi.bookSphere.service.ReviewService;
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
 * Functional Test for Admin Moderation APIs (ADMIN - Requires admin authentication)
 * Tests:
 * - Delete review as admin (DELETE /api/v1/admin/reviews/{id})
 * - Ban user (PATCH /api/v1/admin/users/{id}/ban)
 * - Get all users (GET /api/v1/admin/users)
 * - Get all reviews (GET /api/v1/admin/reviews)
 * - MongoDB + Neo4j consistency
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminModerationControllerTest {

    @Autowired
    private AdminModerationService adminModerationService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private ReviewNodeRepository reviewNodeRepository;

    @Autowired
    private UserNodeRepository userNodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_PREFIX = "AdminModTest_";
    private static String testUserId;
    private static String testBookId;
    private static String testAuthorId;
    private static String testReviewId;

    @Test
    @Order(1)
    @DisplayName("01. Cleanup - Remove previous test data")
    void test01_Cleanup() {
        System.out.println("\n=== TEST 01: Cleanup ===");

        reviewRepository.findAll().stream()
                .filter(r -> r.getUsername() != null && r.getUsername().startsWith(TEST_PREFIX))
                .forEach(review -> {
                    reviewRepository.deleteById(review.getId());
                    reviewNodeRepository.deleteByMongoId(review.getId());
                    System.out.println("Deleted test review: " + review.getId());
                });

        bookRepository.findAll().stream()
                .filter(b -> b.getTitle() != null && b.getTitle().startsWith(TEST_PREFIX))
                .forEach(book -> {
                    bookRepository.deleteById(book.getId());
                    System.out.println("Deleted test book: " + book.getTitle());
                });

        authorRepository.findAll().stream()
                .filter(a -> a.getName() != null && a.getName().startsWith(TEST_PREFIX))
                .forEach(author -> {
                    authorRepository.deleteById(author.getId());
                    System.out.println("Deleted test author: " + author.getName());
                });

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
    @DisplayName("02. Setup - Create test data")
    void test02_Setup() {
        System.out.println("\n=== TEST 02: Setup Test Data ===");

        // Setup admin authentication first
        setupAdminAuthentication();

        // Create test user
        RegisteredUser user = new RegisteredUser();
        user.setUsername(TEST_PREFIX + "User");
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

        // Create test author
        AuthorDocument author = new AuthorDocument();
        author.setName(TEST_PREFIX + "Author");
        author.setRatingsCount(0);
        author.setSumRatings(0);
        author.setStatus("ACTIVE");
        author = authorRepository.save(author);
        testAuthorId = author.getId();
        System.out.println("Created test author: " + author.getName());

        // Create test book
        BookDocument book = new BookDocument();
        book.setTitle(TEST_PREFIX + "Test Book");
        book.setAuthor(new BookDocument.Author(testAuthorId, author.getName()));
        book.setGenres(List.of("Fiction"));
        book.setPublicationYear(2024);
        book.setReviews(new ArrayList<>());
        book.setStatsPerYear(new ArrayList<>());
        book.setAvailability("ACTIVE");
        book = bookRepository.save(book);
        testBookId = book.getId();
        System.out.println("Created test book: " + book.getTitle());

        // Switch to user authentication to create review
        setupUserAuthentication(testUserId, TEST_PREFIX + "User");

        // Create test review
        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setBookId(testBookId);
        reviewDTO.setRating(5);
        reviewDTO.setText("Test review for moderation");
        reviewDTO.setSummary("Test");

        ReviewDTO review = reviewService.createReview(reviewDTO);
        testReviewId = review.getId();
        System.out.println("Created test review: " + testReviewId);

        // Switch back to admin
        setupAdminAuthentication();
    }

    @Test
    @Order(3)
    @DisplayName("03. Get all users - Success")
    void test03_GetAllUsers_Success() {
        System.out.println("\n=== TEST 03: Get All Users ===");

        Page<?> users = adminModerationService.getAllUsers(0, 20);

        assertNotNull(users);
        assertTrue(users.getTotalElements() > 0);
        System.out.println("Retrieved " + users.getTotalElements() + " users");
    }

    @Test
    @Order(4)
    @DisplayName("04. Get all reviews - Success")
    void test04_GetAllReviews_Success() {
        System.out.println("\n=== TEST 04: Get All Reviews ===");

        Page<?> reviews = adminModerationService.getAllReviews(0, 20);

        assertNotNull(reviews);
        assertTrue(reviews.getTotalElements() > 0);
        System.out.println("Retrieved " + reviews.getTotalElements() + " reviews");
    }

    @Test
    @Order(5)
    @DisplayName("05. Delete review - Success")
    void test05_DeleteReview_Success() {
        System.out.println("\n=== TEST 05: Delete Review (Admin) ===");

        // Ensure admin authentication is active
        setupAdminAuthentication();

        adminModerationService.deleteReview(testReviewId);

        // Verify deleted from MongoDB
        assertFalse(reviewRepository.existsById(testReviewId));

        // Verify deleted from Neo4j
        var reviewNodeOpt = reviewNodeRepository.findByMongoId(testReviewId);
        assertFalse(reviewNodeOpt.isPresent());

        System.out.println("Review deleted successfully by admin");
    }

    @Test
    @Order(6)
    @DisplayName("06. Delete review - Non-existent (should fail)")
    void test06_DeleteReview_NotFound() {
        System.out.println("\n=== TEST 06: Delete Non-existent Review ===");

        String fakeId = "000000000000000000000000";

        Exception exception = assertThrows(Exception.class, () -> {
            adminModerationService.deleteReview(fakeId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(7)
    @DisplayName("07. Ban user - Success")
    void test07_BanUser_Success() {
        System.out.println("\n=== TEST 07: Ban User ===");

        adminModerationService.banUser(testUserId);

        // Verify user is banned in MongoDB
        RegisteredUser user = userRepository.findById(testUserId).orElse(null);
        assertNotNull(user);
        assertEquals("BANNED", user.getStatus());

        System.out.println("User banned successfully");
    }

    @Test
    @Order(8)
    @DisplayName("08. Ban user - Already banned")
    void test08_BanUser_AlreadyBanned() {
        System.out.println("\n=== TEST 08: Ban Already Banned User ===");

        // Try to ban again - should handle gracefully
        try {
            adminModerationService.banUser(testUserId);
            System.out.println("Ban operation completed (user already banned)");
        } catch (Exception e) {
            System.out.println("Expected behavior: " + e.getMessage());
        }
    }

    @Test
    @Order(9)
    @DisplayName("09. Test pagination - Get users with different page sizes")
    void test09_GetUsers_Pagination() {
        System.out.println("\n=== TEST 09: Test User Pagination ===");

        // Get first page with size 5
        Page<?> page1 = adminModerationService.getAllUsers(0, 5);
        assertNotNull(page1);
        System.out.println("Page 1: " + page1.getNumberOfElements() + " users");

        // Get second page if exists
        if (page1.getTotalPages() > 1) {
            Page<?> page2 = adminModerationService.getAllUsers(1, 5);
            assertNotNull(page2);
            System.out.println("Page 2: " + page2.getNumberOfElements() + " users");
        }

        System.out.println("Pagination test completed");
    }

    @Test
    @Order(10)
    @DisplayName("10. Test pagination - Get reviews with different page sizes")
    void test10_GetReviews_Pagination() {
        System.out.println("\n=== TEST 10: Test Review Pagination ===");

        // Get first page with size 5
        Page<?> page1 = adminModerationService.getAllReviews(0, 5);
        assertNotNull(page1);
        System.out.println("Page 1: " + page1.getNumberOfElements() + " reviews");

        // Get second page if exists
        if (page1.getTotalPages() > 1) {
            Page<?> page2 = adminModerationService.getAllReviews(1, 5);
            assertNotNull(page2);
            System.out.println("Page 2: " + page2.getNumberOfElements() + " reviews");
        }

        System.out.println("Pagination test completed");
    }

    @Test
    @Order(11)
    @DisplayName("11. Ban user with invalid ID - Should fail")
    void test11_BanUser_InvalidId() {
        System.out.println("\n=== TEST 11: Ban User with Invalid ID ===");

        String invalidId = "invalid-id-format";

        Exception exception = assertThrows(Exception.class, () -> {
            adminModerationService.banUser(invalidId);
        });

        System.out.println("Expected error caught for invalid ID");
    }

    @Test
    @Order(12)
    @DisplayName("12. Delete review with invalid ID - Should fail")
    void test12_DeleteReview_InvalidId() {
        System.out.println("\n=== TEST 12: Delete Review with Invalid ID ===");

        String invalidId = "invalid-id-format";

        Exception exception = assertThrows(Exception.class, () -> {
            adminModerationService.deleteReview(invalidId);
        });

        System.out.println("Expected error caught for invalid ID");
    }

    @Test
    @Order(13)
    @DisplayName("13. Create second user and review for additional testing")
    void test13_CreateSecondUserAndReview() {
        System.out.println("\n=== TEST 13: Create Second User and Review ===");

        // Create second test user
        RegisteredUser user2 = new RegisteredUser();
        user2.setUsername(TEST_PREFIX + "User2");
        user2.setEmail(TEST_PREFIX + "user2@test.com");
        user2.setPasswordHashed(passwordEncoder.encode("password"));
        user2.setCountry("US");
        user2.setStatus("active");
        user2.setReviews(new ArrayList<>());
        user2.setBookshelf(new ArrayList<>());
        user2 = userRepository.save(user2);
        String testUserId2 = user2.getId();
        System.out.println("Created second test user: " + user2.getUsername());

        // Create user node in Neo4j
        userNodeRepository.getOrCreate(testUserId2, user2.getUsername(), user2.getCountry());

        // Switch to user2 authentication to create review
        setupUserAuthentication(testUserId2, TEST_PREFIX + "User2");

        // Create second test review
        ReviewDTO reviewDTO2 = new ReviewDTO();
        reviewDTO2.setBookId(testBookId);
        reviewDTO2.setRating(4);
        reviewDTO2.setText("Second test review for moderation");
        reviewDTO2.setSummary("Test 2");

        ReviewDTO review2 = reviewService.createReview(reviewDTO2);
        System.out.println("Created second test review: " + review2.getId());

        // Verify we now have more reviews
        setupAdminAuthentication();
        Page<?> reviews = adminModerationService.getAllReviews(0, 20);
        System.out.println("Total reviews in system: " + reviews.getTotalElements());

        // Cleanup second user immediately
        userRepository.deleteById(testUserId2);
        userNodeRepository.deleteByMongoId(testUserId2);
        reviewRepository.deleteById(review2.getId());
        reviewNodeRepository.deleteByMongoId(review2.getId());
        System.out.println("Cleaned up second user and review");
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        // Review already deleted in test07

        if (testBookId != null) {
            bookRepository.deleteById(testBookId);
            System.out.println("Deleted test book");
        }

        if (testAuthorId != null) {
            authorRepository.deleteById(testAuthorId);
            System.out.println("Deleted test author");
        }

        if (testUserId != null) {
            userRepository.deleteById(testUserId);
            userNodeRepository.deleteByMongoId(testUserId);
            System.out.println("Deleted test user");
        }

        System.out.println("Final cleanup completed - All test data removed");
    }

    private void setupAdminAuthentication() {
        UserPrincipal principal = new UserPrincipal("admin-id", "admin", "ADMIN", "active");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        System.out.println("Admin authentication setup");
    }

    private void setupUserAuthentication(String userId, String username) {
        UserPrincipal principal = new UserPrincipal(userId, username, "USER", "active");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        System.out.println("User authentication setup for: " + username);
    }
}
