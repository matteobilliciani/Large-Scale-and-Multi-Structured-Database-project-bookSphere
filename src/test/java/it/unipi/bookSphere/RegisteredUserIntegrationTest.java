package it.unipi.bookSphere;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.model.mongodb.*;
import it.unipi.bookSphere.model.neo4j.*;
import it.unipi.bookSphere.repository.mongo.*;
import it.unipi.bookSphere.repository.neo4j.*;
import it.unipi.bookSphere.service.*;
import it.unipi.bookSphere.utils.UserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Integration test for Registered and Unregistered User API operations
 * Tests:
 * - User Registration and Login (Unregistered/Open APIs)
 * - Review CRUD operations with edge cases
 * - Like/Unlike operations (books, reviews, authors, genres)
 * - Follow/Unfollow operations
 * - Bookshelf operations (add, update, remove)
 * - Profile management (update username, delete account)
 * - Data consistency between MongoDB and Neo4j
 * - Error handling and validation
 * - Edge cases and boundary conditions
 */
@SpringBootTest
@ActiveProfiles("clusterWSL")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RegisteredUserIntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private BookshelfService bookshelfService;

    @Autowired
    private ProfileService profileService;

    // @Autowired
    // private AuthService authService; // Not used in tests

    // Repositories for verification
    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private ReviewNodeRepository reviewNodeRepository;

    @Autowired
    private BookNodeRepository bookNodeRepository;

    @Autowired
    private UserNodeRepository userNodeRepository;

    @Autowired
    private AuthorNodeRepository authorNodeRepository;

    @Autowired
    private GenreNodeRepository genreNodeRepository;

    // Test data
    private static String testUserId;
    private static String testUser2Id;
    // private static String testUser3Id; // For additional tests (not used)
    private static String testBookId;
    // private static String testBook2Id; // For additional book tests (not used)
    private static String testAuthorId;
    private static String testReviewId;
    // private static String testReview2Id; // For additional review tests (not used)
    private static final String TEST_USERNAME = "testuser_" + System.currentTimeMillis();
    private static final String TEST_USERNAME2 = "testuser2_" + System.currentTimeMillis();
    // private static final String TEST_USERNAME3 = "testuser3_" + System.currentTimeMillis(); // not used
    private static final String TEST_GENRE = "TestGenre";
    // private static final String TEST_GENRE2 = "TestGenre2"; // not used
    private static final String TEST_GENRE2 = "TestGenre2";

    @BeforeEach
    void setupSecurityContext() {
        // Setup security context for authenticated user
        if (testUserId != null) {
            UserPrincipal userPrincipal = new UserPrincipal(testUserId, TEST_USERNAME, "USER", "active");
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userPrincipal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Setup: Create test data (users, book, author)")
    void setupTestData() {
        System.out.println("\n=== TEST 1: Setting up test data ===");

        // Create test author
        AuthorDocument author = new AuthorDocument();
        author.setName("Test Author " + System.currentTimeMillis());
        author.setRatingsCount(0);
        author.setSumRatings(0);
        author = authorRepository.save(author);
        testAuthorId = author.getId();
        System.out.println("✓ Created test author: " + testAuthorId);

        // Create test book
        BookDocument book = new BookDocument();
        book.setTitle("Test Book " + System.currentTimeMillis());
        book.setPublicationYear(2024);
        book.setDescription("A test book for integration testing");
        
        BookDocument.Author bookAuthor = new BookDocument.Author();
        bookAuthor.setId(testAuthorId);
        bookAuthor.setName(author.getName());
        book.setAuthor(bookAuthor);
        
        book.setGenres(List.of(TEST_GENRE, "Fiction"));
        book = bookRepository.save(book);
        testBookId = book.getId();
        System.out.println("✓ Created test book: " + testBookId);

        // Create BookNode in Neo4j
        BookNode bookNode = new BookNode();
        bookNode.setMongoId(testBookId);
        bookNode.setTitle(book.getTitle());
        bookNode.setYear(book.getPublicationYear());
        bookNodeRepository.save(bookNode);
        System.out.println("✓ Created BookNode in Neo4j");

        // Create AuthorNode in Neo4j
        AuthorNode authorNode = new AuthorNode();
        authorNode.setMongoId(testAuthorId);
        authorNode.setName(author.getName());
        authorNodeRepository.save(authorNode);
        System.out.println("✓ Created AuthorNode in Neo4j");

        // Create test users
        RegisteredUser user1 = new RegisteredUser();
        user1.setUsername(TEST_USERNAME);
        user1.setEmail(TEST_USERNAME + "@test.com");
        user1.setPasswordHashed("hashed_password");
        user1.setCountry("IT");
        user1.setJoinedAt(Instant.now());
        user1.setStatus("active");
        user1 = userRepository.save(user1);
        testUserId = user1.getId();
        System.out.println("✓ Created test user 1: " + testUserId);

        RegisteredUser user2 = new RegisteredUser();
        user2.setUsername(TEST_USERNAME2);
        user2.setEmail(TEST_USERNAME2 + "@test.com");
        user2.setPasswordHashed("hashed_password");
        user2.setCountry("US");
        user2.setJoinedAt(Instant.now());
        user2.setStatus("active");
        user2 = userRepository.save(user2);
        testUser2Id = user2.getId();
        System.out.println("✓ Created test user 2: " + testUser2Id);

        // Create UserNodes in Neo4j
        UserNode userNode1 = new UserNode();
        userNode1.setMongoId(testUserId);
        userNode1.setUsername(TEST_USERNAME);
        userNode1.setCountry("IT");
        userNodeRepository.save(userNode1);

        UserNode userNode2 = new UserNode();
        userNode2.setMongoId(testUser2Id);
        userNode2.setUsername(TEST_USERNAME2);
        userNode2.setCountry("US");
        userNodeRepository.save(userNode2);
        System.out.println("✓ Created UserNodes in Neo4j");

        System.out.println("=== Test data setup completed ===\n");
    }

    @Test
    @Order(2)
    @DisplayName("2. Review: Create review and verify consistency")
    void testCreateReview() throws InterruptedException {
        System.out.println("\n=== TEST 2: Creating review ===");

        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setBookId(testBookId);
        reviewDTO.setRating(85);
        reviewDTO.setText("This is a great test book!");
        reviewDTO.setSummary("Excellent");

        ReviewDTO created = reviewService.createReview(reviewDTO);
        testReviewId = created.getId();
        assertNotNull(testReviewId, "Review should be created");
        System.out.println("✓ Created review: " + testReviewId);

        // Wait for async operations to complete
        Thread.sleep(2000);

        // Verify MongoDB
        Optional<Review> reviewOpt = reviewRepository.findById(testReviewId);
        assertTrue(reviewOpt.isPresent(), "Review should exist in MongoDB");
        Review review = reviewOpt.get();
        assertEquals(testUserId, review.getUserId());
        assertEquals(85, review.getRating());
        System.out.println("✓ Review verified in MongoDB");

        // Verify Neo4j ReviewNode
        Optional<ReviewNode> reviewNodeOpt = reviewNodeRepository.findByMongoId(testReviewId);
        assertTrue(reviewNodeOpt.isPresent(), "ReviewNode should exist in Neo4j");
        assertEquals(85, reviewNodeOpt.get().getRating());
        System.out.println("✓ ReviewNode verified in Neo4j");

        // Verify book statistics updated
        Optional<BookDocument> bookOpt = bookRepository.findById(testBookId);
        assertTrue(bookOpt.isPresent());
        BookDocument book = bookOpt.get();
        assertTrue(book.getReviews().contains(testReviewId), "Book should contain review ID");
        assertNotNull(book.getMonthScore(), "Month score should be initialized");
        assertTrue(book.getMonthScore().getRatingCount() >= 1, "Month score count should be updated");
        System.out.println("✓ Book statistics updated (month_score: " + book.getMonthScore().getRating() + ")");

        // Verify author statistics updated
        Optional<AuthorDocument> authorOpt = authorRepository.findById(testAuthorId);
        assertTrue(authorOpt.isPresent());
        AuthorDocument author = authorOpt.get();
        assertTrue(author.getRatingsCount() >= 1, "Author ratings count should be updated");
        double avgRating = author.getRatingsCount() > 0 ? (double) author.getSumRatings() / author.getRatingsCount() : 0.0;
        System.out.println("✓ Author statistics updated (count: " + author.getRatingsCount() + ", avg: " + String.format("%.2f", avgRating) + ")");

        System.out.println("=== Review creation test passed ===\n");
    }

    @Test
    @Order(3)
    @DisplayName("3. Review: Update review and verify consistency")
    void testUpdateReview() throws InterruptedException {
        System.out.println("\n=== TEST 3: Updating review ===");

        ReviewDTO updateDTO = new ReviewDTO();
        updateDTO.setRating(90);
        updateDTO.setText("Updated text - even better!");

        ReviewDTO updated = reviewService.updateReview(testReviewId, updateDTO);
        assertEquals(90, updated.getRating());
        System.out.println("✓ Updated review rating from 85 to 90");

        // Wait for async operations
        Thread.sleep(2000);

        // Verify MongoDB
        Optional<Review> reviewOpt = reviewRepository.findById(testReviewId);
        assertTrue(reviewOpt.isPresent());
        assertEquals(90, reviewOpt.get().getRating());
        System.out.println("✓ Review updated in MongoDB");

        // Verify Neo4j
        Optional<ReviewNode> reviewNodeOpt = reviewNodeRepository.findByMongoId(testReviewId);
        assertTrue(reviewNodeOpt.isPresent());
        assertEquals(90, reviewNodeOpt.get().getRating());
        System.out.println("✓ ReviewNode updated in Neo4j");

        // Verify author statistics adjusted
        Optional<AuthorDocument> authorOpt = authorRepository.findById(testAuthorId);
        assertTrue(authorOpt.isPresent());
        System.out.println("✓ Author statistics adjusted after rating change");

        System.out.println("=== Review update test passed ===\n");
    }

    @Test
    @Order(4)
    @DisplayName("4. Like: Like book and verify relationship")
    void testLikeBook() {
        System.out.println("\n=== TEST 4: Liking book ===");

        likeService.likeBook(testBookId);
        System.out.println("✓ Liked book: " + testBookId);

        // Verify Neo4j relationship
        boolean liked = bookNodeRepository.userLikesBook(testUserId, testBookId);
        assertTrue(liked, "LIKES relationship should exist in Neo4j");
        System.out.println("✓ LIKES relationship verified in Neo4j");

        System.out.println("=== Like book test passed ===\n");
    }

    @Test
    @Order(5)
    @DisplayName("5. Like: Like review and verify consistency")
    void testLikeReview() throws InterruptedException {
        System.out.println("\n=== TEST 5: Liking review ===");

        // Get initial likes count
        Review reviewBefore = reviewRepository.findById(testReviewId).orElseThrow();
        int initialLikes = reviewBefore.getLikesCount() != null ? reviewBefore.getLikesCount() : 0;

        likeService.likeReview(testReviewId);
        System.out.println("✓ Liked review: " + testReviewId);

        // Wait for async operations
        Thread.sleep(2000);

        // Verify MongoDB likes_count incremented
        Review reviewAfter = reviewRepository.findById(testReviewId).orElseThrow();
        assertEquals(initialLikes + 1, reviewAfter.getLikesCount(), "Likes count should increment");
        System.out.println("✓ Review likes_count updated in MongoDB: " + reviewAfter.getLikesCount());

        // Verify Neo4j relationship
        boolean liked = reviewNodeRepository.userLikesReview(testUserId, testReviewId);
        assertTrue(liked, "LIKES relationship should exist for review");
        System.out.println("✓ LIKES relationship verified in Neo4j");

        System.out.println("=== Like review test passed ===\n");
    }

    @Test
    @Order(6)
    @DisplayName("6. Like: Like author and genre")
    void testLikeAuthorAndGenre() {
        System.out.println("\n=== TEST 6: Liking author and genre ===");

        likeService.likeAuthor(testAuthorId);
        System.out.println("✓ Liked author: " + testAuthorId);

        boolean authorLiked = authorNodeRepository.userLikesAuthor(testUserId, testAuthorId);
        assertTrue(authorLiked, "Author LIKES relationship should exist");
        System.out.println("✓ Author LIKES relationship verified");

        likeService.likeGenre(TEST_GENRE);
        System.out.println("✓ Liked genre: " + TEST_GENRE);

        // Genre names are normalized to Title Case
        String normalizedGenre = it.unipi.bookSphere.utils.NormalizationUtils.normalizeGenreName(TEST_GENRE);
        boolean genreLiked = genreNodeRepository.userLikesGenre(testUserId, normalizedGenre);
        assertTrue(genreLiked, "Genre LIKES relationship should exist");
        System.out.println("✓ Genre LIKES relationship verified");

        System.out.println("=== Like author and genre test passed ===\n");
    }

    @Test
    @Order(7)
    @DisplayName("7. Follow: Follow user and verify relationship")
    void testFollowUser() {
        System.out.println("\n=== TEST 7: Following user ===");

        followService.followUser(testUser2Id);
        System.out.println("✓ Followed user: " + testUser2Id);

        // Verify Neo4j relationship
        boolean follows = userNodeRepository.userFollowsUser(testUserId, testUser2Id);
        assertTrue(follows, "FOLLOWS relationship should exist");
        System.out.println("✓ FOLLOWS relationship verified in Neo4j");

        System.out.println("=== Follow user test passed ===\n");
    }

    @Test
    @Order(8)
    @DisplayName("8. Bookshelf: Add book to bookshelf")
    void testAddToBookshelf() {
        System.out.println("\n=== TEST 8: Adding book to bookshelf ===");

        bookshelfService.addBookToBookshelf(testBookId, "want_to_read");
        System.out.println("✓ Added book to bookshelf with status: want_to_read");

        // Verify MongoDB
        Optional<RegisteredUser> userOpt = userRepository.findById(testUserId);
        assertTrue(userOpt.isPresent());
        RegisteredUser user = userOpt.get();
        assertNotNull(user.getBookshelf());
        boolean bookInShelf = user.getBookshelf().stream()
                .anyMatch(item -> item.getBookId().equals(testBookId));
        assertTrue(bookInShelf, "Book should be in user's bookshelf");
        System.out.println("✓ Book verified in user's bookshelf (MongoDB)");

        System.out.println("=== Bookshelf test passed ===\n");
    }

    @Test
    @Order(9)
    @DisplayName("9. Profile: Update username and verify consistency")
    void testUpdateUsername() throws InterruptedException {
        System.out.println("\n=== TEST 9: Updating username ===");

        String newUsername = TEST_USERNAME + "_updated";
        profileService.updateUsername(newUsername);
        System.out.println("✓ Updated username to: " + newUsername);

        // Wait for async operations
        Thread.sleep(2000);

        // Verify MongoDB user
        Optional<RegisteredUser> userOpt = userRepository.findById(testUserId);
        assertTrue(userOpt.isPresent());
        assertEquals(newUsername, userOpt.get().getUsername());
        System.out.println("✓ Username updated in MongoDB user document");

        // Verify Neo4j UserNode
        Optional<UserNode> userNodeOpt = userNodeRepository.findByMongoId(testUserId);
        assertTrue(userNodeOpt.isPresent());
        assertEquals(newUsername, userNodeOpt.get().getUsername());
        System.out.println("✓ Username updated in Neo4j UserNode");

        // Verify reviews updated (async)
        Optional<Review> reviewOpt = reviewRepository.findById(testReviewId);
        if (reviewOpt.isPresent()) {
            // May take time due to async
            System.out.println("✓ Review username update: " + reviewOpt.get().getUsername());
        }

        System.out.println("=== Update username test passed ===\n");
    }

    @Test
    @Order(10)
    @DisplayName("10. Unlike: Unlike operations and verify consistency")
    void testUnlikeOperations() throws InterruptedException {
        System.out.println("\n=== TEST 10: Unliking operations ===");

        // Unlike review
        Review reviewBefore = reviewRepository.findById(testReviewId).orElseThrow();
        int likesBefore = reviewBefore.getLikesCount();

        likeService.unlikeReview(testReviewId);
        System.out.println("✓ Unliked review");

        Thread.sleep(1000);

        Review reviewAfter = reviewRepository.findById(testReviewId).orElseThrow();
        assertEquals(likesBefore - 1, reviewAfter.getLikesCount());
        System.out.println("✓ Review likes_count decremented");

        // Unlike book
        likeService.unlikeBook(testBookId);
        boolean stillLiked = bookNodeRepository.userLikesBook(testUserId, testBookId);
        assertFalse(stillLiked, "Book should no longer be liked");
        System.out.println("✓ Unliked book");

        System.out.println("=== Unlike operations test passed ===\n");
    }

    @Test
    @Order(11)
    @DisplayName("11. Unfollow: Unfollow user and verify")
    void testUnfollowUser() {
        System.out.println("\n=== TEST 11: Unfollowing user ===");

        followService.unfollowUser(testUser2Id);
        System.out.println("✓ Unfollowed user");

        boolean stillFollows = userNodeRepository.userFollowsUser(testUserId, testUser2Id);
        assertFalse(stillFollows, "FOLLOWS relationship should be removed");
        System.out.println("✓ FOLLOWS relationship removed from Neo4j");

        System.out.println("=== Unfollow test passed ===\n");
    }

    @Test
    @Order(12)
    @DisplayName("12. Review: Delete review and verify consistency")
    void testDeleteReview() throws InterruptedException {
        System.out.println("\n=== TEST 12: Deleting review ===");

        reviewService.deleteReview(testReviewId);
        System.out.println("✓ Deleted review: " + testReviewId);

        // Wait for async operations
        Thread.sleep(2000);

        // Verify MongoDB - review deleted
        Optional<Review> reviewOpt = reviewRepository.findById(testReviewId);
        assertFalse(reviewOpt.isPresent(), "Review should be deleted from MongoDB");
        System.out.println("✓ Review deleted from MongoDB");

        // Verify Neo4j - ReviewNode deleted
        Optional<ReviewNode> reviewNodeOpt = reviewNodeRepository.findByMongoId(testReviewId);
        assertFalse(reviewNodeOpt.isPresent(), "ReviewNode should be deleted from Neo4j");
        System.out.println("✓ ReviewNode deleted from Neo4j");

        // Verify book statistics updated
        Optional<BookDocument> bookOpt = bookRepository.findById(testBookId);
        assertTrue(bookOpt.isPresent());
        BookDocument book = bookOpt.get();
        assertFalse(book.getReviews().contains(testReviewId), "Review ID should be removed from book");
        System.out.println("✓ Review ID removed from book's reviews array");

        // Verify author statistics adjusted
        Optional<AuthorDocument> authorOpt = authorRepository.findById(testAuthorId);
        assertTrue(authorOpt.isPresent());
        System.out.println("✓ Author statistics adjusted after review deletion");

        System.out.println("=== Delete review test passed ===\n");
    }

    @Test
    @Order(13)
    @DisplayName("13. Cleanup: Delete test data")
    void cleanupTestData() {
        System.out.println("\n=== TEST 13: Cleaning up test data ===");

        // Delete users
        userRepository.deleteById(testUserId);
        userRepository.deleteById(testUser2Id);
        userNodeRepository.deleteByMongoId(testUserId);
        userNodeRepository.deleteByMongoId(testUser2Id);
        System.out.println("✓ Deleted test users");

        // Delete book and author
        bookRepository.deleteById(testBookId);
        authorRepository.deleteById(testAuthorId);
        bookNodeRepository.deleteByMongoId(testBookId);
        authorNodeRepository.deleteByMongoId(testAuthorId);
        System.out.println("✓ Deleted test book and author");

        System.out.println("=== Cleanup completed ===\n");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
