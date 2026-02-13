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
import it.unipi.bookSphere.service.open.ReviewService;
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

        // Remove test users (skip soft-deleted users)
        userRepository.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().startsWith(TEST_PREFIX))
                .filter(u -> !"deleted".equals(u.getStatus()) && !"BANNED".equals(u.getStatus()))
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
    @Order(9)
    @DisplayName("09. Create review on archived book - Should fail")
    void test09_CreateReview_ArchivedBook() {
        System.out.println("\n=== TEST 09: Create Review on Archived Book ===");

        // Create an archived book
        BookDocument archivedBook = new BookDocument();
        archivedBook.setTitle(TEST_PREFIX + "Archived Book");
        archivedBook.setAuthor(new BookDocument.Author(testAuthorId, TEST_PREFIX + "Author"));
        archivedBook.setGenres(List.of("Fiction"));
        archivedBook.setPublicationYear(2020);
        archivedBook.setReviews(new ArrayList<>());
        archivedBook.setStatsPerYear(new ArrayList<>());
        archivedBook.setAvailability("ARCHIVED");
        archivedBook = bookRepository.save(archivedBook);
        String archivedBookId = archivedBook.getId();

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setBookId(archivedBookId);
        reviewDTO.setRating(5);
        reviewDTO.setText("Review on archived book");

        Exception exception = assertThrows(Exception.class, () -> {
            reviewService.createReview(reviewDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());

        // Cleanup
        bookRepository.deleteById(archivedBookId);
    }

    @Test
    @Order(10)
    @DisplayName("10. Update review - Unauthorized user (should fail)")
    void test10_UpdateReview_Unauthorized() {
        System.out.println("\n=== TEST 10: Update Review - Unauthorized User ===");

        // Create a review as first user
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        // Create a new book for this test
        BookDocument book2 = new BookDocument();
        book2.setTitle(TEST_PREFIX + "Test Book 2");
        book2.setAuthor(new BookDocument.Author(testAuthorId, TEST_PREFIX + "Author"));
        book2.setGenres(List.of("Fiction"));
        book2.setPublicationYear(2024);
        book2.setReviews(new ArrayList<>());
        book2.setStatsPerYear(new ArrayList<>());
        book2.setAvailability("ACTIVE");
        book2 = bookRepository.save(book2);
        String book2Id = book2.getId();

        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setBookId(book2Id);
        reviewDTO.setRating(5);
        reviewDTO.setText("Original user's review");

        ReviewDTO createdReview = reviewService.createReview(reviewDTO);
        String reviewId = createdReview.getId();

        // Create second user
        RegisteredUser user2 = new RegisteredUser();
        user2.setUsername(TEST_PREFIX + "User2");
        user2.setEmail(TEST_PREFIX + "user2@test.com");
        user2.setPasswordHashed(passwordEncoder.encode("password"));
        user2.setCountry("US");
        user2.setStatus("active");
        user2.setReviews(new ArrayList<>());
        user2.setBookshelf(new ArrayList<>());
        user2 = userRepository.save(user2);
        String user2Id = user2.getId();

        // Try to update review as second user
        setupAuthentication(user2Id, TEST_PREFIX + "User2");

        ReviewDTO updateDTO = new ReviewDTO();
        updateDTO.setRating(1);
        updateDTO.setText("Malicious update");

        Exception exception = assertThrows(Exception.class, () -> {
            reviewService.updateReview(reviewId, updateDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());

        // Cleanup
        reviewRepository.deleteById(reviewId);
        bookRepository.deleteById(book2Id);
        userRepository.deleteById(user2Id);
        userNodeRepository.deleteByMongoId(user2Id);
    }

    @Test
    @Order(11)
    @DisplayName("11. Delete review - Unauthorized user (should fail)")
    void test11_DeleteReview_Unauthorized() {
        System.out.println("\n=== TEST 11: Delete Review - Unauthorized User ===");

        // Create a review as first user
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        // Create a new book for this test
        BookDocument book3 = new BookDocument();
        book3.setTitle(TEST_PREFIX + "Test Book 3");
        book3.setAuthor(new BookDocument.Author(testAuthorId, TEST_PREFIX + "Author"));
        book3.setGenres(List.of("Fiction"));
        book3.setPublicationYear(2024);
        book3.setReviews(new ArrayList<>());
        book3.setStatsPerYear(new ArrayList<>());
        book3.setAvailability("ACTIVE");
        book3 = bookRepository.save(book3);
        String book3Id = book3.getId();

        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setBookId(book3Id);
        reviewDTO.setRating(4);
        reviewDTO.setText("Another user's review");

        ReviewDTO createdReview = reviewService.createReview(reviewDTO);
        String reviewId = createdReview.getId();

        // Create second user
        RegisteredUser user3 = new RegisteredUser();
        user3.setUsername(TEST_PREFIX + "User3");
        user3.setEmail(TEST_PREFIX + "user3@test.com");
        user3.setPasswordHashed(passwordEncoder.encode("password"));
        user3.setCountry("FR");
        user3.setStatus("active");
        user3.setReviews(new ArrayList<>());
        user3.setBookshelf(new ArrayList<>());
        user3 = userRepository.save(user3);
        String user3Id = user3.getId();

        // Try to delete review as second user
        setupAuthentication(user3Id, TEST_PREFIX + "User3");

        Exception exception = assertThrows(Exception.class, () -> {
            reviewService.deleteReview(reviewId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());

        // Cleanup
        setupAuthentication(testUserId, TEST_PREFIX + "User");
        reviewService.deleteReview(reviewId);
        bookRepository.deleteById(book3Id);
        userRepository.deleteById(user3Id);
        userNodeRepository.deleteByMongoId(user3Id);
    }

    @Test
    @Order(12)
    @DisplayName("12. Update review from previous year - Stats update correct year")
    void test12_UpdateReview_PreviousYear() {
        System.out.println("\n=== TEST 12: Update Review from Previous Year ===");

        // Create a book for this specific test
        BookDocument oldBook = new BookDocument();
        oldBook.setTitle(TEST_PREFIX + "Old Book");
        oldBook.setAuthor(new BookDocument.Author(testAuthorId, TEST_PREFIX + "Author"));
        oldBook.setGenres(List.of("Fiction"));
        oldBook.setPublicationYear(2020);
        oldBook.setReviews(new ArrayList<>());
        oldBook.setStatsPerYear(new ArrayList<>());
        oldBook.setAvailability("ACTIVE");
        oldBook = bookRepository.save(oldBook);
        String oldBookId = oldBook.getId();

        // Create a review dated in 2024
        Review oldReview = new Review();
        oldReview.setUserId(testUserId);
        oldReview.setUsername(TEST_PREFIX + "User");
        oldReview.setRating(50);
        oldReview.setText("Old review from 2024");
        oldReview.setCreatedAt(java.time.Instant.parse("2024-01-15T10:00:00Z"));
        oldReview.setBookSnapshot(new Review.BookSnapshot(oldBookId, TEST_PREFIX + "Old Book"));
        oldReview = reviewRepository.save(oldReview);
        String oldReviewId = oldReview.getId();

        // Manually set stats for 2024
        BookDocument.YearStat stat2024 = new BookDocument.YearStat(2024, 1, 50);
        oldBook.getStatsPerYear().add(stat2024);
        
        // Set current month score
        String currentMonth = java.time.YearMonth.now().toString();
        oldBook.setMonthScore(new BookDocument.MonthScore(1, 100, currentMonth));
        bookRepository.save(oldBook);

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        // Update the old review - should update 2024 stats, NOT current month
        ReviewDTO updateDTO = new ReviewDTO();
        updateDTO.setRating(100);
        updateDTO.setText("Updated rating from 50 to 100");

        reviewService.updateReview(oldReviewId, updateDTO);

        // Wait for async operations
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify: 2024 stats should be updated
        BookDocument bookAfter = bookRepository.findById(oldBookId).orElse(null);
        assertNotNull(bookAfter);
        
        BookDocument.YearStat stat2024After = bookAfter.getStatsPerYear().stream()
                .filter(s -> s.getYear().equals(2024))
                .findFirst().orElse(null);
        assertNotNull(stat2024After, "2024 stat should exist");
        assertEquals(1, stat2024After.getRatingsCount(), "2024 rating count should be 1");
        assertEquals(100, stat2024After.getSumRating(), "2024 sum should be updated to 100");

        // Verify: current month score should NOT be touched
        assertEquals(1, bookAfter.getMonthScore().getRatingCount(), "Month score count should not change");
        assertEquals(100, bookAfter.getMonthScore().getSumRating(), "Month score sum should not change");

        System.out.println("✓ Old review stats updated correctly: 2024 stats updated, month score untouched");

        // Cleanup
        reviewRepository.deleteById(oldReviewId);
        bookRepository.deleteById(oldBookId);
    }

    @Test
    @Order(13)
    @DisplayName("13. Delete only review of a year - Year entry removed")
    void test13_DeleteReview_OnlyReviewOfYear() {
        System.out.println("\n=== TEST 13: Delete Only Review of a Year ===");

        // Create a book for this specific test
        BookDocument yearBook = new BookDocument();
        yearBook.setTitle(TEST_PREFIX + "Year Book");
        yearBook.setAuthor(new BookDocument.Author(testAuthorId, TEST_PREFIX + "Author"));
        yearBook.setGenres(List.of("Fiction"));
        yearBook.setPublicationYear(2020);
        yearBook.setReviews(new ArrayList<>());
        yearBook.setStatsPerYear(new ArrayList<>());
        yearBook.setAvailability("ACTIVE");
        yearBook = bookRepository.save(yearBook);
        String yearBookId = yearBook.getId();

        // Create a review dated in 2023
        Review yearReview = new Review();
        yearReview.setUserId(testUserId);
        yearReview.setUsername(TEST_PREFIX + "User");
        yearReview.setRating(75);
        yearReview.setText("Only review in 2023");
        yearReview.setCreatedAt(java.time.Instant.parse("2023-06-15T10:00:00Z"));
        yearReview.setBookSnapshot(new Review.BookSnapshot(yearBookId, TEST_PREFIX + "Year Book"));
        yearReview = reviewRepository.save(yearReview);
        String yearReviewId = yearReview.getId();

        // Manually set stats for 2023
        BookDocument.YearStat stat2023 = new BookDocument.YearStat(2023, 1, 75);
        yearBook.getStatsPerYear().add(stat2023);
        yearBook.getReviews().add(yearReviewId);
        bookRepository.save(yearBook);

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        // Delete the only review of 2023
        reviewService.deleteReview(yearReviewId);

        // Wait for async operations
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify: 2023 entry should be removed from stats_per_year
        BookDocument bookAfter = bookRepository.findById(yearBookId).orElse(null);
        assertNotNull(bookAfter);
        
        boolean has2023 = bookAfter.getStatsPerYear().stream()
                .anyMatch(s -> s.getYear().equals(2023));
        assertFalse(has2023, "2023 entry should be removed from stats_per_year");

        System.out.println("✓ Year entry correctly removed when last review deleted");

        // Cleanup
        bookRepository.deleteById(yearBookId);
    }

    @Test
    @Order(14)
    @DisplayName("14. Update review text only - No stats recalculation")
    void test14_UpdateReview_TextOnly() {
        System.out.println("\n=== TEST 14: Update Review Text Only ===");

        // Create a book and review for this test
        BookDocument textBook = new BookDocument();
        textBook.setTitle(TEST_PREFIX + "Text Book");
        textBook.setAuthor(new BookDocument.Author(testAuthorId, TEST_PREFIX + "Author"));
        textBook.setGenres(List.of("Fiction"));
        textBook.setPublicationYear(2024);
        textBook.setReviews(new ArrayList<>());
        textBook.setStatsPerYear(new ArrayList<>());
        textBook.setAvailability("ACTIVE");
        textBook = bookRepository.save(textBook);
        String textBookId = textBook.getId();

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        // Create a review
        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setBookId(textBookId);
        reviewDTO.setRating(80);
        reviewDTO.setText("Initial text for this review");
        reviewDTO.setSummary("Initial");

        ReviewDTO createdReview = reviewService.createReview(reviewDTO);
        String textReviewId = createdReview.getId();

        // Wait for async operations
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Get stats BEFORE text-only update
        BookDocument bookBefore = bookRepository.findById(textBookId).orElse(null);
        assertNotNull(bookBefore);
        int countBefore = bookBefore.getStatsPerYear().get(0).getRatingsCount();
        int sumBefore = bookBefore.getStatsPerYear().get(0).getSumRating();

        // Update ONLY text (no rating change)
        ReviewDTO updateDTO = new ReviewDTO();
        updateDTO.setText("Updated text only, rating unchanged");
        updateDTO.setSummary("Updated");

        reviewService.updateReview(textReviewId, updateDTO);

        // Wait for async operations
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify: stats should NOT change
        BookDocument bookAfter = bookRepository.findById(textBookId).orElse(null);
        assertNotNull(bookAfter);
        assertEquals(countBefore, bookAfter.getStatsPerYear().get(0).getRatingsCount(), 
                    "Rating count should NOT change when only text is updated");
        assertEquals(sumBefore, bookAfter.getStatsPerYear().get(0).getSumRating(),
                    "Rating sum should NOT change when only text is updated");

        // Verify text WAS updated
        Review reviewAfter = reviewRepository.findById(textReviewId).orElse(null);
        assertNotNull(reviewAfter);
        assertEquals("Updated text only, rating unchanged", reviewAfter.getText());

        System.out.println("✓ Text-only update: stats unchanged, text updated correctly");

        // Cleanup
        reviewRepository.deleteById(textReviewId);
        bookRepository.deleteById(textBookId);
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
