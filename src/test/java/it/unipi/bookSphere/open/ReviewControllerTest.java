package it.unipi.bookSphere.open;

import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.mongodb.Review;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.mongo.ReviewRepository;
import it.unipi.bookSphere.service.ReviewService;
import it.unipi.bookSphere.TestProfile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional Test for Review APIs (OPEN - No authentication required)
 * Tests:
 * - Get reviews by IDs (GET /api/v1/reviews?review=...&review=...)
 * - Error handling for non-existent reviews
 * - Maximum limit handling
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

    private static final String TEST_PREFIX = "ReviewTest_";
    private static String testReviewId1;
    private static String testReviewId2;
    private static String testReviewId3;
    private static String testBookId;
    private static String testAuthorId;
    private static String testUserId;

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
                    System.out.println("Deleted test user: " + user.getUsername());
                });

        System.out.println("Cleanup completed");
    }

    @Test
    @Order(2)
    @DisplayName("02. Setup - Create test data")
    void test02_Setup() {
        System.out.println("\n=== TEST 02: Setup Test Data ===");

        // Create test user
        RegisteredUser user = new RegisteredUser();
        user.setUsername(TEST_PREFIX + "User");
        user.setEmail(TEST_PREFIX + "user@test.com");
        user.setPasswordHashed("hashedpassword");
        user.setCountry("IT");
        user.setStatus("active");
        user.setJoinedAt(Instant.now());
        user = userRepository.save(user);
        testUserId = user.getId();
        System.out.println("Created test user: " + user.getUsername());

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

        // Create test review 1
        Review review1 = new Review();
        review1.setUserId(testUserId);
        review1.setUsername(TEST_PREFIX + "User");
        review1.setRating(85);
        review1.setText(TEST_PREFIX + "Great book!");
        review1.setSummary("Excellent");
        review1.setSource("test");
        review1.setLikesCount(0);
        review1.setCreatedAt(Instant.now());
        Review.BookSnapshot bookSnapshot1 = new Review.BookSnapshot();
        bookSnapshot1.setBookId(testBookId);
        bookSnapshot1.setTitle(TEST_PREFIX + "Test Book");
        review1.setBookSnapshot(bookSnapshot1);
        review1 = reviewRepository.save(review1);
        testReviewId1 = review1.getId();
        System.out.println("Created test review 1: " + review1.getId());

        // Create test review 2
        Review review2 = new Review();
        review2.setUserId(testUserId);
        review2.setUsername(TEST_PREFIX + "User");
        review2.setRating(90);
        review2.setText(TEST_PREFIX + "Amazing read!");
        review2.setSummary("Fantastic");
        review2.setSource("test");
        review2.setLikesCount(5);
        review2.setCreatedAt(Instant.now());
        Review.BookSnapshot bookSnapshot2 = new Review.BookSnapshot();
        bookSnapshot2.setBookId(testBookId);
        bookSnapshot2.setTitle(TEST_PREFIX + "Test Book");
        review2.setBookSnapshot(bookSnapshot2);
        review2 = reviewRepository.save(review2);
        testReviewId2 = review2.getId();
        System.out.println("Created test review 2: " + review2.getId());

        // Create test review 3
        Review review3 = new Review();
        review3.setUserId(testUserId);
        review3.setUsername(TEST_PREFIX + "User");
        review3.setRating(75);
        review3.setText(TEST_PREFIX + "Good but could be better");
        review3.setSummary("Decent");
        review3.setSource("test");
        review3.setLikesCount(2);
        review3.setCreatedAt(Instant.now());
        Review.BookSnapshot bookSnapshot3 = new Review.BookSnapshot();
        bookSnapshot3.setBookId(testBookId);
        bookSnapshot3.setTitle(TEST_PREFIX + "Test Book");
        review3.setBookSnapshot(bookSnapshot3);
        review3 = reviewRepository.save(review3);
        testReviewId3 = review3.getId();
        System.out.println("Created test review 3: " + review3.getId());
    }

    @Test
    @Order(3)
    @DisplayName("03. Get reviews by IDs - Single review")
    void test03_GetReviewsByIds_Single() {
        System.out.println("\n=== TEST 03: Get Single Review by ID ===");

        List<ReviewDTO> reviews = reviewService.getReviewsByIds(List.of(testReviewId1));

        assertNotNull(reviews);
        assertEquals(1, reviews.size());
        assertEquals(testReviewId1, reviews.get(0).getId());
        assertEquals(TEST_PREFIX + "User", reviews.get(0).getUsername());
        assertEquals(85, reviews.get(0).getRating());
        assertTrue(reviews.get(0).getText().contains("Great book!"));

        System.out.println("Retrieved 1 review successfully");
    }

    @Test
    @Order(4)
    @DisplayName("04. Get reviews by IDs - Multiple reviews")
    void test04_GetReviewsByIds_Multiple() {
        System.out.println("\n=== TEST 04: Get Multiple Reviews by IDs ===");

        List<ReviewDTO> reviews = reviewService.getReviewsByIds(
                Arrays.asList(testReviewId1, testReviewId2, testReviewId3)
        );

        assertNotNull(reviews);
        assertEquals(3, reviews.size());
        
        // Verify all reviews are present
        List<String> retrievedIds = reviews.stream().map(ReviewDTO::getId).toList();
        assertTrue(retrievedIds.contains(testReviewId1));
        assertTrue(retrievedIds.contains(testReviewId2));
        assertTrue(retrievedIds.contains(testReviewId3));

        System.out.println("Retrieved 3 reviews successfully");
    }

    @Test
    @Order(5)
    @DisplayName("05. Get reviews by IDs - Non-existent review")
    void test05_GetReviewsByIds_NonExistent() {
        System.out.println("\n=== TEST 05: Get Non-existent Review ===");

        String fakeId = "000000000000000000000000";
        List<ReviewDTO> reviews = reviewService.getReviewsByIds(List.of(fakeId));

        assertNotNull(reviews);
        assertEquals(0, reviews.size());

        System.out.println("Non-existent review handled correctly - empty list returned");
    }

    @Test
    @Order(6)
    @DisplayName("06. Get reviews by IDs - Mixed existing and non-existent")
    void test06_GetReviewsByIds_MixedExistence() {
        System.out.println("\n=== TEST 06: Get Reviews with Mixed Existence ===");

        String fakeId = "000000000000000000000000";
        List<ReviewDTO> reviews = reviewService.getReviewsByIds(
                Arrays.asList(testReviewId1, fakeId, testReviewId2)
        );

        assertNotNull(reviews);
        assertEquals(2, reviews.size()); // Only existing reviews should be returned
        
        List<String> retrievedIds = reviews.stream().map(ReviewDTO::getId).toList();
        assertTrue(retrievedIds.contains(testReviewId1));
        assertTrue(retrievedIds.contains(testReviewId2));
        assertFalse(retrievedIds.contains(fakeId));

        System.out.println("Mixed existence handled correctly - 2 valid reviews returned");
    }

    @Test
    @Order(7)
    @DisplayName("07. Get reviews by IDs - Empty list")
    void test07_GetReviewsByIds_EmptyList() {
        System.out.println("\n=== TEST 07: Get Reviews with Empty List ===");

        List<ReviewDTO> reviews = reviewService.getReviewsByIds(new ArrayList<>());

        assertNotNull(reviews);
        assertEquals(0, reviews.size());

        System.out.println("Empty list handled correctly");
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        // Remove test reviews
        if (testReviewId1 != null) {
            reviewRepository.deleteById(testReviewId1);
            System.out.println("Deleted test review 1");
        }
        if (testReviewId2 != null) {
            reviewRepository.deleteById(testReviewId2);
            System.out.println("Deleted test review 2");
        }
        if (testReviewId3 != null) {
            reviewRepository.deleteById(testReviewId3);
            System.out.println("Deleted test review 3");
        }

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
            System.out.println("Deleted test user");
        }

        System.out.println("Final cleanup completed - All test data removed");
    }
}
