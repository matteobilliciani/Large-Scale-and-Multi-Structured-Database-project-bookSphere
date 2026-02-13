package it.unipi.bookSphere.registered;

import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.dto.BookDTO;
import it.unipi.bookSphere.dto.GenreDTO;
import it.unipi.bookSphere.dto.ReviewDTO;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.mongodb.Review;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.mongo.ReviewRepository;
import it.unipi.bookSphere.repository.neo4j.*;
import it.unipi.bookSphere.service.registered.LikeService;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional Test for Like APIs (REGISTERED - Requires authentication)
 * Tests:
 * - Like/Unlike book (POST/DELETE /api/v1/me/like/book)
 * - Like/Unlike author (POST/DELETE /api/v1/me/likes/authors)
 * - Like/Unlike genre (POST/DELETE /api/v1/me/likes/genres)
 * - Like/Unlike review (POST/DELETE /api/v1/me/like/review)
 * - Get liked items (GET /api/v1/me/liked/*)
 * - Neo4j relationship management
 * - Duplicate like handling
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LikeControllerTest {

    @Autowired
    private LikeService likeService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookNodeRepository bookNodeRepository;

    @Autowired
    private UserNodeRepository userNodeRepository;

    @Autowired
    private AuthorNodeRepository authorNodeRepository;

    @Autowired
    private GenreNodeRepository genreNodeRepository;

    @Autowired
    private ReviewNodeRepository reviewNodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_PREFIX = "LikeTest_";
    private static String testUserId;
    private static String testBookId;
    private static String testAuthorId;
    private static String testReviewId;
    private static String testGenreName = "LikeTestGenre";

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

        // Remove test genre from Neo4j
        genreNodeRepository.deleteByName(testGenreName);

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

        // Create author node in Neo4j
        authorNodeRepository.getOrCreate(testAuthorId, author.getName());

        // Create test book
        BookDocument book = new BookDocument();
        book.setTitle(TEST_PREFIX + "Test Book");
        book.setAuthor(new BookDocument.Author(testAuthorId, author.getName()));
        book.setGenres(List.of(testGenreName));
        book.setPublicationYear(2024);
        book.setReviews(new ArrayList<>());
        book.setStatsPerYear(new ArrayList<>());
        book.setAvailability("ACTIVE");
        book = bookRepository.save(book);
        testBookId = book.getId();
        System.out.println("Created test book: " + book.getTitle());

        // Create book node in Neo4j
        bookNodeRepository.getOrCreate(testBookId, book.getTitle(), 2024);

        // Create genre node in Neo4j
        genreNodeRepository.getOrCreate(testGenreName);

        // Create test review
        Review review = new Review();
        review.setUserId(testUserId);
        review.setUsername(user.getUsername());
        review.setBookSnapshot(new Review.BookSnapshot(testBookId, book.getTitle()));
        review.setRating(5);
        review.setText("Test review for likes");
        review.setSummary("Good");
        review.setCreatedAt(java.time.Instant.now());
        review.setLikesCount(0);
        review = reviewRepository.save(review);
        testReviewId = review.getId();
        System.out.println("Created test review: " + testReviewId);

        // Create review node in Neo4j
        it.unipi.bookSphere.model.neo4j.ReviewNode reviewNode = new it.unipi.bookSphere.model.neo4j.ReviewNode();
        reviewNode.setMongoId(testReviewId);
        reviewNode.setRating(5);
        reviewNode.setCreatedAt(java.time.LocalDateTime.now());
        reviewNodeRepository.save(reviewNode);

        // Setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");
    }

    @Test
    @Order(3)
    @DisplayName("03. Like book - Success")
    void test03_LikeBook_Success() {
        System.out.println("\n=== TEST 03: Like Book ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        likeService.likeBook(testBookId);

        // Verify in Neo4j - relationship should exist
        var likes = bookNodeRepository.getLikedBooksByUser(testUserId);
        assertNotNull(likes);
        assertTrue(likes.stream().anyMatch(b -> b.bookId().equals(testBookId)));

        System.out.println("Book liked successfully");
    }

    @Test
    @Order(4)
    @DisplayName("04. Like book - Duplicate (should fail)")
    void test04_LikeBook_Duplicate() {
        System.out.println("\n=== TEST 04: Like Book Duplicate ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        Exception exception = assertThrows(Exception.class, () -> {
            likeService.likeBook(testBookId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("05. Unlike book - Success")
    void test05_UnlikeBook_Success() {
        System.out.println("\n=== TEST 05: Unlike Book ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        likeService.unlikeBook(testBookId);

        // Verify in Neo4j - relationship should not exist
        var likes = bookNodeRepository.getLikedBooksByUser(testUserId);
        assertNotNull(likes);
        assertFalse(likes.stream().anyMatch(b -> b.bookId().equals(testBookId)));

        System.out.println("Book unliked successfully");
    }

    @Test
    @Order(6)
    @DisplayName("06. Like author - Success")
    void test06_LikeAuthor_Success() {
        System.out.println("\n=== TEST 06: Like Author ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        likeService.likeAuthor(testAuthorId);

        // Verify liked authors with pagination
        Page<AuthorDTO> likes = likeService.getLikedAuthors(0, 20);
        assertNotNull(likes);
        assertTrue(likes.stream().anyMatch(a -> a.getId().equals(testAuthorId)));

        System.out.println("Author liked successfully");
    }

    @Test
    @Order(7)
    @DisplayName("07. Like author - Duplicate (should fail)")
    void test07_LikeAuthor_Duplicate() {
        System.out.println("\n=== TEST 07: Like Author Duplicate ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        Exception exception = assertThrows(Exception.class, () -> {
            likeService.likeAuthor(testAuthorId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(8)
    @DisplayName("08. Unlike author - Success")
    void test08_UnlikeAuthor_Success() {
        System.out.println("\n=== TEST 08: Unlike Author ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        likeService.unlikeAuthor(testAuthorId);

        // Verify author no longer liked
        Page<AuthorDTO> likes = likeService.getLikedAuthors(0, 20);
        assertNotNull(likes);
        assertFalse(likes.stream().anyMatch(a -> a.getId().equals(testAuthorId)));

        System.out.println("Author unliked successfully");
    }

    @Test
    @Order(9)
    @DisplayName("09. Like genre - Success")
    void test09_LikeGenre_Success() {
        System.out.println("\n=== TEST 09: Like Genre ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        likeService.likeGenre(testGenreName);

        // Verify liked genres with pagination
        Page<GenreDTO> likes = likeService.getLikedGenres(0, 20);
        assertNotNull(likes);
        // Genre names are normalized to Title Case, so "LikeTestGenre" becomes "Liketestgenre"
        String normalizedName = "Liketestgenre";
        assertTrue(likes.stream().anyMatch(g -> g.getName().equals(normalizedName)), 
                   "Should find liked genre with normalized name: " + normalizedName);

        System.out.println("Genre liked successfully");
    }

    @Test
    @Order(10)
    @DisplayName("10. Like genre - Duplicate (should fail)")
    void test10_LikeGenre_Duplicate() {
        System.out.println("\n=== TEST 10: Like Genre Duplicate ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        Exception exception = assertThrows(Exception.class, () -> {
            likeService.likeGenre(testGenreName);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(11)
    @DisplayName("11. Unlike genre - Success")
    void test11_UnlikeGenre_Success() {
        System.out.println("\n=== TEST 11: Unlike Genre ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        likeService.unlikeGenre(testGenreName);

        // Verify genre no longer liked
        Page<GenreDTO> likes = likeService.getLikedGenres(0, 20);
        assertNotNull(likes);
        assertFalse(likes.stream().anyMatch(g -> g.getName().equals(testGenreName)));

        System.out.println("Genre unliked successfully");
    }

    @Test
    @Order(12)
    @DisplayName("12. Like review - Success")
    void test12_LikeReview_Success() {
        System.out.println("\n=== TEST 12: Like Review ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        likeService.likeReview(testReviewId);

        // Verify liked reviews with pagination
        Page<ReviewDTO> likes = likeService.getLikedReviews(0, 20);
        assertNotNull(likes);
        assertTrue(likes.stream().anyMatch(r -> r.getId().equals(testReviewId)));

        // Verify likes_count in MongoDB is updated
        Review review = reviewRepository.findById(testReviewId).orElse(null);
        assertNotNull(review);
        assertEquals(1, review.getLikesCount());

        System.out.println("Review liked successfully");
    }

    @Test
    @Order(13)
    @DisplayName("13. Like review - Duplicate (should fail)")
    void test13_LikeReview_Duplicate() {
        System.out.println("\n=== TEST 13: Like Review Duplicate ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        Exception exception = assertThrows(Exception.class, () -> {
            likeService.likeReview(testReviewId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(14)
    @DisplayName("14. Unlike review - Success")
    void test14_UnlikeReview_Success() {
        System.out.println("\n=== TEST 14: Unlike Review ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        likeService.unlikeReview(testReviewId);

        // Verify review no longer liked
        Page<ReviewDTO> likes = likeService.getLikedReviews(0, 20);
        assertNotNull(likes);
        assertFalse(likes.stream().anyMatch(r -> r.getId().equals(testReviewId)));

        // Verify likes_count in MongoDB is decremented
        Review review = reviewRepository.findById(testReviewId).orElse(null);
        assertNotNull(review);
        assertEquals(0, review.getLikesCount());

        System.out.println("Review unliked successfully");
    }

    @Test
    @Order(15)
    @DisplayName("15. Get liked books - Pagination test")
    void test15_GetLikedBooks_Pagination() {
        System.out.println("\n=== TEST 15: Get Liked Books Pagination ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        // First like a book
        likeService.likeBook(testBookId);

        // Test pagination
        Page<BookDTO> page = likeService.getLikedBooks(0, 10);
        assertNotNull(page);
        assertTrue(page.getContent().size() <= 10);
        assertTrue(page.stream().anyMatch(b -> b.getId().equals(testBookId)));

        // Unlike for cleanup
        likeService.unlikeBook(testBookId);

        System.out.println("Pagination test completed");
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        if (testReviewId != null) {
            reviewRepository.deleteById(testReviewId);
            reviewNodeRepository.deleteByMongoId(testReviewId);
            System.out.println("Deleted test review");
        }

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

        genreNodeRepository.deleteByName(testGenreName);
        System.out.println("Deleted test genre");

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
