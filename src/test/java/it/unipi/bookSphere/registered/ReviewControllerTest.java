package it.unipi.bookSphere.registered;

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
import it.unipi.bookSphere.service.ReviewService;
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
 * Functional Test for Review APIs (REGISTERED - Requires authentication)
 * Tests:
 * - Create review (POST /api/v1/me/reviews)
 * - Update review (PATCH /api/v1/me/reviews/{id})
 * - Delete review (DELETE /api/v1/me/reviews/{id})
 * - MongoDB + Neo4j consistency
 * - Authorization checks
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReviewControllerTest {

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

    private static final String TEST_PREFIX = "ReviewTest_";
    private static String testUserId;
    private static String testBookId;
    private static String testAuthorId;
    private static String testReviewId;

    @Test
    @Order(1)
    @DisplayName("01. Cleanup - Remove previous test data")
    void test01_Cleanup() {
        System.out.println("\n=== TEST 01: Cleanup ===");

        // Remove test reviews
        reviewRepository.findAll().stream()
                .filter(r -> r.getUsername() != null && r.getUsername().startsWith(TEST_PREFIX))
                .forEach(review -> {
                    reviewRepository.deleteById(review.getId());
                    reviewNodeRepository.deleteByMongoId(review.getId());
                    System.out.println("Deleted test review: " + review.getId());
                });

        // Remove test books
        bookRepository.findAll().stream()
                .filter(b -> b.getTitle() != null && b.getTitle().startsWith(TEST_PREFIX))
                .forEach(book -> {
                    bookRepository.deleteById(book.getId());
                    System.out.println("Deleted test book: " + book.getTitle());
                });

        // Remove test authors
        authorRepository.findAll().stream()
                .filter(a -> a.getName() != null && a.getName().startsWith(TEST_PREFIX))
                .forEach(author -> {
                    authorRepository.deleteById(author.getId());
                    System.out.println("Deleted test author: " + author.getName());
                });

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
    @DisplayName("02. Setup - Create test data and authenticate")
    void test02_Setup() {
        System.out.println("\n=== TEST 02: Setup Test Data ===");

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

        // Setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");
    }

    @Test
    @Order(3)
    @DisplayName("03. Create review - Success")
    void test03_CreateReview_Success() {
        System.out.println("\n=== TEST 03: Create Review ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setBookId(testBookId);
        reviewDTO.setRating(5);
        reviewDTO.setText("This is a test review. Great book!");
        reviewDTO.setSummary("Great!");

        ReviewDTO result = reviewService.createReview(reviewDTO);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getBookId());
        assertEquals(5, result.getRating());
        assertEquals("This is a test review. Great book!", result.getText());
        assertEquals(TEST_PREFIX + "User", result.getUsername());

        testReviewId = result.getId();

        // Verify in MongoDB
        Review reviewMongo = reviewRepository.findById(testReviewId).orElse(null);
        assertNotNull(reviewMongo);
        assertEquals(5, reviewMongo.getRating());
        assertEquals("This is a test review. Great book!", reviewMongo.getText());
        assertEquals(TEST_PREFIX + "User", reviewMongo.getUsername());

        // Verify in Neo4j
        var reviewNodeOpt = reviewNodeRepository.findByMongoId(testReviewId);
        assertTrue(reviewNodeOpt.isPresent());
        assertEquals(5, reviewNodeOpt.get().getRating());

        // Verify book's review array is updated
        BookDocument bookAfter = bookRepository.findById(testBookId).orElse(null);
        assertNotNull(bookAfter);
        assertTrue(bookAfter.getReviews().contains(testReviewId));

        System.out.println("Review created successfully: " + testReviewId);
    }

    @Test
    @Order(4)
    @DisplayName("04. Create review - Duplicate (should fail)")
    void test04_CreateReview_Duplicate() {
        System.out.println("\n=== TEST 04: Create Duplicate Review ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setBookId(testBookId); // Same book as test03
        reviewDTO.setRating(4);
        reviewDTO.setText("Another review for same book");

        Exception exception = assertThrows(Exception.class, () -> {
            reviewService.createReview(reviewDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("05. Update review - Success")
    void test05_UpdateReview_Success() {
        System.out.println("\n=== TEST 05: Update Review ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        ReviewDTO updateDTO = new ReviewDTO();
        updateDTO.setRating(4);
        updateDTO.setText("Updated review text. Still good but not perfect.");
        updateDTO.setSummary("Good");

        ReviewDTO result = reviewService.updateReview(testReviewId, updateDTO);

        assertNotNull(result);
        assertEquals(testReviewId, result.getId());
        assertEquals(4, result.getRating());
        assertEquals("Updated review text. Still good but not perfect.", result.getText());
        assertEquals("Good", result.getSummary());

        // Verify in MongoDB
        Review reviewMongo = reviewRepository.findById(testReviewId).orElse(null);
        assertNotNull(reviewMongo);
        assertEquals(4, reviewMongo.getRating());
        assertEquals("Updated review text. Still good but not perfect.", reviewMongo.getText());

        // Verify in Neo4j
        var reviewNodeOpt = reviewNodeRepository.findByMongoId(testReviewId);
        assertTrue(reviewNodeOpt.isPresent());
        assertEquals(4, reviewNodeOpt.get().getRating());

        // Verify book's review array still contains the review
        BookDocument bookAfter = bookRepository.findById(testBookId).orElse(null);
        assertNotNull(bookAfter);
        assertTrue(bookAfter.getReviews().contains(testReviewId));

        System.out.println("Review updated successfully");
    }

    @Test
    @Order(6)
    @DisplayName("06. Update review - Non-existent (should fail)")
    void test06_UpdateReview_NotFound() {
        System.out.println("\n=== TEST 06: Update Non-existent Review ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        String fakeId = "000000000000000000000000";
        ReviewDTO updateDTO = new ReviewDTO();
        updateDTO.setRating(3);
        updateDTO.setText("Test");

        Exception exception = assertThrows(Exception.class, () -> {
            reviewService.updateReview(fakeId, updateDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(7)
    @DisplayName("07. Delete review - Success")
    void test07_DeleteReview_Success() {
        System.out.println("\n=== TEST 07: Delete Review ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        reviewService.deleteReview(testReviewId);

        // Verify deleted from MongoDB
        assertFalse(reviewRepository.existsById(testReviewId));

        // Verify deleted from Neo4j
        var reviewNodeOpt = reviewNodeRepository.findByMongoId(testReviewId);
        assertFalse(reviewNodeOpt.isPresent());

        // Verify book's review array is updated
        BookDocument bookAfter = bookRepository.findById(testBookId).orElse(null);
        assertNotNull(bookAfter);
        assertFalse(bookAfter.getReviews().contains(testReviewId));

        System.out.println("Review deleted successfully");
    }

    @Test
    @Order(8)
    @DisplayName("08. Delete review - Non-existent (should fail)")
    void test08_DeleteReview_NotFound() {
        System.out.println("\n=== TEST 08: Delete Non-existent Review ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        String fakeId = "000000000000000000000000";

        Exception exception = assertThrows(Exception.class, () -> {
            reviewService.deleteReview(fakeId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        // Review already deleted in test07
        
        // Remove test book
        if (testBookId != null) {
            bookRepository.deleteById(testBookId);
            System.out.println("Deleted test book");
        }

        // Remove test author
        if (testAuthorId != null) {
            authorRepository.deleteById(testAuthorId);
            System.out.println("Deleted test author");
        }

        // Remove test user
        if (testUserId != null) {
            userRepository.deleteById(testUserId);
            userNodeRepository.deleteByMongoId(testUserId);
            System.out.println("Deleted test user");
        }

        System.out.println("Final cleanup completed - All test data removed");
    }

    // Helper method to setup authentication
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
