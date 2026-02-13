package it.unipi.bookSphere.registered;

import it.unipi.bookSphere.dto.RecommendationDTO;
import it.unipi.bookSphere.dto.WrappedDTO;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.model.mongodb.Review;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.mongo.ReviewRepository;
import it.unipi.bookSphere.repository.neo4j.*;
import it.unipi.bookSphere.service.registered.UserFeaturesService;
import it.unipi.bookSphere.utils.UserPrincipal;
import it.unipi.bookSphere.TestProfile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional Test for User Features APIs (REGISTERED - Requires authentication)
 * Tests:
 * - Get personalized recommendations (GET /api/v1/me/recommendations)
 * - Get yearly wrapped (GET /api/v1/me/wrapped)
 * - Neo4j Query 1 (Recommendations)
 * - MongoDB Query 2 (Yearly Wrapped)
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserFeaturesControllerTest {

    @Autowired
    private UserFeaturesService userFeaturesService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserNodeRepository userNodeRepository;

    @Autowired
    private BookNodeRepository bookNodeRepository;

    @Autowired
    private AuthorNodeRepository authorNodeRepository;

    @Autowired
    private GenreNodeRepository genreNodeRepository;

    @Autowired
    private ReviewNodeRepository reviewNodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_PREFIX = "UserFeaturesTest_";
    private static String testUserId;
    private static String testFriendId;
    private static String testAuthorId;
    private static String testBookId;
    private static String testBook2Id;
    private static String testReviewId;
    private static String testReview2Id;
    private static final String TEST_GENRE = "UserFeaturesTestGenre";

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

        // Remove test genre
        genreNodeRepository.deleteByName(TEST_GENRE);

        System.out.println("Cleanup completed");
    }

    @Test
    @Order(2)
    @DisplayName("02. Setup - Create test data and authenticate")
    void test02_Setup() {
        System.out.println("\n=== TEST 02: Setup Test Data ===");

        // Create test user (main user)
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

        // Create friend user
        RegisteredUser friend = new RegisteredUser();
        friend.setUsername(TEST_PREFIX + "Friend");
        friend.setEmail(TEST_PREFIX + "friend@test.com");
        friend.setPasswordHashed(passwordEncoder.encode("password"));
        friend.setCountry("US");
        friend.setStatus("active");
        friend.setReviews(new ArrayList<>());
        friend.setBookshelf(new ArrayList<>());
        friend = userRepository.save(friend);
        testFriendId = friend.getId();
        System.out.println("Created friend user: " + friend.getUsername());

        // Create friend node in Neo4j
        userNodeRepository.getOrCreate(testFriendId, friend.getUsername(), friend.getCountry());

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

        // Create genre node
        genreNodeRepository.getOrCreate(TEST_GENRE);

        // Create test book 1
        BookDocument book1 = new BookDocument();
        book1.setTitle(TEST_PREFIX + "Book 1");
        book1.setAuthor(new BookDocument.Author(testAuthorId, author.getName()));
        book1.setGenres(List.of(TEST_GENRE));
        book1.setPublicationYear(LocalDate.now().getYear());
        book1.setReviews(new ArrayList<>());
        book1.setStatsPerYear(new ArrayList<>());
        book1.setAvailability("ACTIVE");
        book1 = bookRepository.save(book1);
        testBookId = book1.getId();
        System.out.println("Created test book 1: " + book1.getTitle());

        // Create book node in Neo4j
        bookNodeRepository.getOrCreate(testBookId, book1.getTitle(), LocalDate.now().getYear());

        // Create test book 2
        BookDocument book2 = new BookDocument();
        book2.setTitle(TEST_PREFIX + "Book 2");
        book2.setAuthor(new BookDocument.Author(testAuthorId, author.getName()));
        book2.setGenres(List.of(TEST_GENRE));
        book2.setPublicationYear(LocalDate.now().getYear());
        book2.setReviews(new ArrayList<>());
        book2.setStatsPerYear(new ArrayList<>());
        book2.setAvailability("ACTIVE");
        book2 = bookRepository.save(book2);
        testBook2Id = book2.getId();
        System.out.println("Created test book 2: " + book2.getTitle());

        // Create book node in Neo4j
        bookNodeRepository.getOrCreate(testBook2Id, book2.getTitle(), LocalDate.now().getYear());

        // Create review from main user (for wrapped)
        Review review1 = new Review();
        review1.setUserId(testUserId);
        review1.setUsername(user.getUsername());
        review1.setBookSnapshot(new Review.BookSnapshot(testBookId, book1.getTitle()));
        review1.setRating(5);
        review1.setText("Great book!");
        review1.setSummary("Excellent");
        // Set to current year for yearly wrapped test
        review1.setCreatedAt(java.time.LocalDate.of(LocalDate.now().getYear(), 6, 15).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        review1.setLikesCount(0);
        review1 = reviewRepository.save(review1);
        testReviewId = review1.getId();
        System.out.println("Created review 1: " + testReviewId);

        // Create review node in Neo4j
        it.unipi.bookSphere.model.neo4j.ReviewNode reviewNode1 = new it.unipi.bookSphere.model.neo4j.ReviewNode();
        reviewNode1.setMongoId(testReviewId);
        reviewNode1.setRating(5);
        reviewNode1.setCreatedAt(java.time.LocalDateTime.now());
        reviewNodeRepository.save(reviewNode1);

        // Create review from main user for book 2 (for wrapped)
        Review review2 = new Review();
        review2.setUserId(testUserId);
        review2.setUsername(user.getUsername());
        review2.setBookSnapshot(new Review.BookSnapshot(testBook2Id, book2.getTitle()));
        review2.setRating(3);
        review2.setText("Average book");
        review2.setSummary("OK");
        // Set to current year for yearly wrapped test
        review2.setCreatedAt(java.time.LocalDate.of(LocalDate.now().getYear(), 8, 20).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        review2.setLikesCount(0);
        review2 = reviewRepository.save(review2);
        testReview2Id = review2.getId();
        System.out.println("Created review 2: " + testReview2Id);

        // Create review node in Neo4j
        it.unipi.bookSphere.model.neo4j.ReviewNode reviewNode2 = new it.unipi.bookSphere.model.neo4j.ReviewNode();
        reviewNode2.setMongoId(testReview2Id);
        reviewNode2.setRating(3);
        reviewNode2.setCreatedAt(java.time.LocalDateTime.now());
        reviewNodeRepository.save(reviewNode2);

        // Add books to user's bookshelf with "read" status for yearly wrapped test
        // This is required because getYearlyWrapped() queries the bookshelf field
        Instant book1AddedInstant = LocalDate.of(LocalDate.now().getYear(), 6, 15).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant book2AddedInstant = LocalDate.of(LocalDate.now().getYear(), 8, 20).atStartOfDay(ZoneId.systemDefault()).toInstant();
        
        RegisteredUser userToUpdate = userRepository.findById(testUserId).orElseThrow();
        List<RegisteredUser.BookshelfItem> bookshelf = new ArrayList<>();
        
        RegisteredUser.BookshelfItem item1 = new RegisteredUser.BookshelfItem();
        item1.setBookId(testBookId);
        item1.setStatus("read");
        item1.setAddedAt(book1AddedInstant);
        bookshelf.add(item1);
        
        RegisteredUser.BookshelfItem item2 = new RegisteredUser.BookshelfItem();
        item2.setBookId(testBook2Id);
        item2.setStatus("read");
        item2.setAddedAt(book2AddedInstant);
        bookshelf.add(item2);
        
        userToUpdate.setBookshelf(bookshelf);
        userRepository.save(userToUpdate);
        System.out.println("Added 2 books to user bookshelf with 'read' status");

        // Setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");
    }

    @Test
    @Order(3)
    @DisplayName("03. Get recommendations - Verify call works")
    void test03_GetRecommendations_VerifyCall() {
        System.out.println("\n=== TEST 03: Get Recommendations Verify Call ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        // Simply verify the API works without errors
        List<RecommendationDTO> recommendations = userFeaturesService.getRecommendations(10);

        assertNotNull(recommendations);
        System.out.println("Recommendations count: " + recommendations.size());

        // Verify structure if recommendations exist
        if (!recommendations.isEmpty()) {
            RecommendationDTO rec = recommendations.get(0);
            assertNotNull(rec.getBookId());
            assertNotNull(rec.getTitle());
            System.out.println("Sample recommendation: " + rec.getTitle() + " (score: " + rec.getScore() + ")");
        }
    }

    @Test
    @Order(4)
    @DisplayName("04. Get recommendations - With limit")
    void test04_GetRecommendations_WithLimit() {
        System.out.println("\n=== TEST 04: Get Recommendations (With Limit) ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        List<RecommendationDTO> recommendations = userFeaturesService.getRecommendations(5);

        assertNotNull(recommendations);
        assertTrue(recommendations.size() <= 5, "Recommendations should respect limit");
        System.out.println("Recommendations with limit 5: " + recommendations.size());
    }

    @Test
    @Order(5)
    @DisplayName("05. Get recommendations - Invalid limit (should use default)")
    void test05_GetRecommendations_InvalidLimit() {
        System.out.println("\n=== TEST 05: Get Recommendations (Invalid Limit) ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        // Null limit should use default
        List<RecommendationDTO> recommendations = userFeaturesService.getRecommendations(null);

        assertNotNull(recommendations);
        System.out.println("Recommendations with null limit: " + recommendations.size());
    }

    @Test
    @Order(6)
    @DisplayName("06. Get yearly wrapped - Success")
    void test06_GetYearlyWrapped_Success() {
        System.out.println("\n=== TEST 06: Get Yearly Wrapped ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        WrappedDTO wrapped = userFeaturesService.getYearlyWrapped();

        assertNotNull(wrapped);
        System.out.println("Wrapped data retrieved successfully");

        // Verify structure
        assertNotNull(wrapped.getYear());
        // Check for current year since we set review dates to current year
        assertEquals(LocalDate.now().getYear(), wrapped.getYear());

        // Log the results
        System.out.println("Year: " + wrapped.getYear());
        System.out.println("Best book: " + (wrapped.getBestBook() != null ? wrapped.getBestBook().getTitle() : "none"));
        System.out.println("Worst book: " + (wrapped.getWorstBook() != null ? wrapped.getWorstBook().getTitle() : "none"));
        System.out.println("Top authors: " + (wrapped.getTopAuthors() != null ? wrapped.getTopAuthors().size() : 0));
        System.out.println("Top genres: " + (wrapped.getTopGenres() != null ? wrapped.getTopGenres().size() : 0));
        System.out.println("Total books read: " + wrapped.getTotalBooksRead());
    }

    @Test
    @Order(7)
    @DisplayName("07. Get yearly wrapped - Verify content")
    void test07_GetYearlyWrapped_VerifyContent() {
        System.out.println("\n=== TEST 07: Verify Wrapped Content ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        WrappedDTO wrapped = userFeaturesService.getYearlyWrapped();

        assertNotNull(wrapped);

        // Verify best book (should be the one with rating 5)
        if (wrapped.getBestBook() != null) {
            System.out.println("Best book: " + wrapped.getBestBook().getTitle() + " (rating: " + wrapped.getBestBook().getRating() + ")");
            assertEquals(5, wrapped.getBestBook().getRating());
        }

        // Verify worst book (should be the one with rating 3)
        if (wrapped.getWorstBook() != null) {
            System.out.println("Worst book: " + wrapped.getWorstBook().getTitle() + " (rating: " + wrapped.getWorstBook().getRating() + ")");
            assertEquals(3, wrapped.getWorstBook().getRating());
        }

        // Verify top authors
        if (wrapped.getTopAuthors() != null && !wrapped.getTopAuthors().isEmpty()) {
            System.out.println("Top authors count: " + wrapped.getTopAuthors().size());
            boolean hasTestAuthor = wrapped.getTopAuthors().stream()
                    .filter(a -> a.getName() != null)  // Filter out null names
                    .anyMatch(a -> a.getName().startsWith(TEST_PREFIX));
            System.out.println("Contains test author: " + hasTestAuthor);
        }

        // Verify top genres
        if (wrapped.getTopGenres() != null && !wrapped.getTopGenres().isEmpty()) {
            System.out.println("Top genres count: " + wrapped.getTopGenres().size());
            boolean hasTestGenre = wrapped.getTopGenres().stream()
                    .anyMatch(g -> g.getName().equals(TEST_GENRE));
            System.out.println("Contains test genre: " + hasTestGenre);
        }

        // Verify total books read
        assertNotNull(wrapped.getTotalBooksRead());
        assertTrue(wrapped.getTotalBooksRead() >= 2, "Should have read at least 2 books (our test data)");
        System.out.println("Total books read verified: " + wrapped.getTotalBooksRead());
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        if (testReviewId != null) {
            reviewRepository.deleteById(testReviewId);
            reviewNodeRepository.deleteByMongoId(testReviewId);
            System.out.println("Deleted test review 1");
        }

        if (testReview2Id != null) {
            reviewRepository.deleteById(testReview2Id);
            reviewNodeRepository.deleteByMongoId(testReview2Id);
            System.out.println("Deleted test review 2");
        }

        if (testBookId != null) {
            bookRepository.deleteById(testBookId);
            System.out.println("Deleted test book 1");
        }

        if (testBook2Id != null) {
            bookRepository.deleteById(testBook2Id);
            System.out.println("Deleted test book 2");
        }

        if (testAuthorId != null) {
            authorRepository.deleteById(testAuthorId);
            System.out.println("Deleted test author");
        }

        if (testUserId != null) {
            RegisteredUser testUser = userRepository.findById(testUserId).orElse(null);
            if (testUser != null && !"deleted".equals(testUser.getStatus()) && !"BANNED".equals(testUser.getStatus())) {
                userRepository.deleteById(testUserId);
                userNodeRepository.deleteByMongoId(testUserId);
                System.out.println("Deleted test user");
            } else if (testUser != null) {
                System.out.println("Test user left as soft-deleted (" + testUser.getStatus() + ")");
            }
        }

        if (testFriendId != null) {
            RegisteredUser friendUser = userRepository.findById(testFriendId).orElse(null);
            if (friendUser != null && !"deleted".equals(friendUser.getStatus()) && !"BANNED".equals(friendUser.getStatus())) {
                userRepository.deleteById(testFriendId);
                userNodeRepository.deleteByMongoId(testFriendId);
                System.out.println("Deleted friend user");
            } else if (friendUser != null) {
                System.out.println("Friend user left as soft-deleted (" + friendUser.getStatus() + ")");
            }
        }

        genreNodeRepository.deleteByName(TEST_GENRE);
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
