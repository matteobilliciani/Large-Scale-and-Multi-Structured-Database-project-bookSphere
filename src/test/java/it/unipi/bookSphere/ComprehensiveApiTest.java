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


import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Integration Test for BookSphere APIs
 * 
 * Tests all API endpoints for:
 * 1. UNREGISTERED/OPEN APIs:
 *    - User registration with validation
 *    - User login with authentication
 *    - Public data access
 * 
 * 2. REGISTERED USER APIs:
 *    - Review CRUD operations (create, read, update, delete)
 *    - Like/Unlike operations (books, reviews, authors, genres)
 *    - Follow/Unfollow operations
 *    - Bookshelf management (add, update status, remove)
 *    - Profile management (update username, delete account)
 * 
 * 3. DATA CONSISTENCY:
 *    - MongoDB and Neo4j synchronization
 *    - Cascading operations
 *    - Statistics updates
 * 
 * 4. ERROR HANDLING & EDGE CASES:
 *    - Invalid inputs
 *    - Duplicate operations
 *    - Non-existent resources
 *    - Authorization checks
 */
@SpringBootTest
@ActiveProfiles("wsl")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ComprehensiveApiTest {

    // ========== SERVICES ==========
    @Autowired
    private AuthService authService;

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

    // ========== MONGODB REPOSITORIES ==========
    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    // ========== NEO4J REPOSITORIES ==========
    @Autowired
    private UserNodeRepository userNodeRepository;

    @Autowired
    private BookNodeRepository bookNodeRepository;

    @Autowired
    private AuthorNodeRepository authorNodeRepository;

    @Autowired
    private ReviewNodeRepository reviewNodeRepository;

    @Autowired
    private GenreNodeRepository genreNodeRepository;

    // ========== TEST DATA ==========
    private static final long TIMESTAMP = System.currentTimeMillis();
    private static final String TEST_USERNAME_1 = "testuser1_" + TIMESTAMP;
    private static final String TEST_USERNAME_2 = "testuser2_" + TIMESTAMP;
    private static final String TEST_USERNAME_3 = "testuser3_" + TIMESTAMP;
    private static final String TEST_EMAIL_1 = TEST_USERNAME_1 + "@test.com";
    private static final String TEST_EMAIL_2 = TEST_USERNAME_2 + "@test.com";
    private static final String TEST_EMAIL_3 = TEST_USERNAME_3 + "@test.com";
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String TEST_GENRE_1 = "TestGenre1";
    private static final String TEST_GENRE_2 = "TestGenre2";

    private static String testUserId1;
    private static String testUserId2;
    private static String testUserId3;
    private static String testBookId1;
    private static String testBookId2;
    private static String testAuthorId1;
    private static String testAuthorId2;
    private static String testReviewId1;
    private static String testReviewId2;

    // ========== HELPER METHODS ==========

    /**
     * Setup security context for authenticated API calls
     */
    private void setupSecurityContext(String userId, String username) {
        UserPrincipal userPrincipal = new UserPrincipal(userId, username, "USER", "active");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Clear security context
     */
    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Wait for async operations to complete
     */
    private void waitForAsyncOperations() throws InterruptedException {
        Thread.sleep(2000);
    }

    @AfterEach
    void cleanupSecurityContext() {
        clearSecurityContext();
    }

    // ==========================================================
    // PART 1: SETUP TEST DATA
    // ==========================================================

    @Test
    @Order(1)
    @DisplayName("01. Setup: Create test books and authors")
    void test01_setupTestData() {
        System.out.println("\n========== TEST 01: SETUP TEST DATA ==========");

        // Create test author 1
        AuthorDocument author1 = new AuthorDocument();
        author1.setName("Test Author One " + TIMESTAMP);
        author1.setRatingsCount(0);
        author1.setSumRatings(0);
        author1 = authorRepository.save(author1);
        testAuthorId1 = author1.getId();
        System.out.println("✓ Created author 1: " + author1.getName());

        // Create test author 2
        AuthorDocument author2 = new AuthorDocument();
        author2.setName("Test Author Two " + TIMESTAMP);
        author2.setRatingsCount(0);
        author2.setSumRatings(0);
        author2 = authorRepository.save(author2);
        testAuthorId2 = author2.getId();
        System.out.println("✓ Created author 2: " + author2.getName());

        // Create test book 1
        BookDocument book1 = new BookDocument();
        book1.setTitle("Test Book One " + TIMESTAMP);
        book1.setPublicationYear(2024);
        book1.setDescription("First test book for comprehensive testing");
        BookDocument.Author bookAuthor1 = new BookDocument.Author();
        bookAuthor1.setId(testAuthorId1);
        bookAuthor1.setName(author1.getName());
        book1.setAuthor(bookAuthor1);
        book1.setGenres(List.of(TEST_GENRE_1, "Fiction"));
        book1 = bookRepository.save(book1);
        testBookId1 = book1.getId();
        System.out.println("✓ Created book 1: " + book1.getTitle());

        // Create test book 2
        BookDocument book2 = new BookDocument();
        book2.setTitle("Test Book Two " + TIMESTAMP);
        book2.setPublicationYear(2024);
        book2.setDescription("Second test book for comprehensive testing");
        BookDocument.Author bookAuthor2 = new BookDocument.Author();
        bookAuthor2.setId(testAuthorId2);
        bookAuthor2.setName(author2.getName());
        book2.setAuthor(bookAuthor2);
        book2.setGenres(List.of(TEST_GENRE_2, "Mystery"));
        book2 = bookRepository.save(book2);
        testBookId2 = book2.getId();
        System.out.println("✓ Created book 2: " + book2.getTitle());

        // Create Neo4j nodes for books
        BookNode bookNode1 = new BookNode();
        bookNode1.setMongoId(testBookId1);
        bookNode1.setTitle(book1.getTitle());
        bookNode1.setYear(book1.getPublicationYear());
        bookNodeRepository.save(bookNode1);

        BookNode bookNode2 = new BookNode();
        bookNode2.setMongoId(testBookId2);
        bookNode2.setTitle(book2.getTitle());
        bookNode2.setYear(book2.getPublicationYear());
        bookNodeRepository.save(bookNode2);
        System.out.println("✓ Created BookNodes in Neo4j");

        // Create Neo4j nodes for authors
        AuthorNode authorNode1 = new AuthorNode();
        authorNode1.setMongoId(testAuthorId1);
        authorNode1.setName(author1.getName());
        authorNodeRepository.save(authorNode1);

        AuthorNode authorNode2 = new AuthorNode();
        authorNode2.setMongoId(testAuthorId2);
        authorNode2.setName(author2.getName());
        authorNodeRepository.save(authorNode2);
        System.out.println("✓ Created AuthorNodes in Neo4j");

        System.out.println("========== SETUP COMPLETED ==========\n");
    }

    // ==========================================================
    // PART 2: UNREGISTERED/OPEN API TESTS (Registration & Login)
    // ==========================================================

    @Test
    @Order(2)
    @DisplayName("02. Unregistered API: Register new user successfully")
    void test02_registerNewUser() {
        System.out.println("\n========== TEST 02: USER REGISTRATION ==========");

        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(TEST_USERNAME_1);
        registerDTO.setEmail(TEST_EMAIL_1);
        registerDTO.setPassword(TEST_PASSWORD);
        registerDTO.setCountry("IT");

        UserDTO userDTO = authService.register(registerDTO);
        testUserId1 = userDTO.getId();

        assertNotNull(testUserId1, "User ID should not be null");
        assertEquals(TEST_USERNAME_1, userDTO.getUsername());
        assertEquals(TEST_EMAIL_1, userDTO.getEmail());
        assertEquals("IT", userDTO.getCountry());
        assertEquals("active", userDTO.getStatus());
        System.out.println("✓ User registered successfully: " + testUserId1);

        // Verify MongoDB
        Optional<RegisteredUser> mongoUserOpt = userRepository.findById(testUserId1);
        assertTrue(mongoUserOpt.isPresent(), "User should exist in MongoDB");
        RegisteredUser mongoUser = mongoUserOpt.get();
        assertEquals(TEST_USERNAME_1, mongoUser.getUsername());
        assertNotNull(mongoUser.getPasswordHashed(), "Password should be hashed");
        assertNotEquals(TEST_PASSWORD, mongoUser.getPasswordHashed(), "Password should be hashed, not plain");
        System.out.println("✓ User verified in MongoDB");

        // Verify Neo4j
        Optional<UserNode> neo4jUserOpt = userNodeRepository.findByMongoId(testUserId1);
        assertTrue(neo4jUserOpt.isPresent(), "User should exist in Neo4j");
        assertEquals(TEST_USERNAME_1, neo4jUserOpt.get().getUsername());
        System.out.println("✓ UserNode verified in Neo4j");

        System.out.println("========== REGISTRATION TEST PASSED ==========\n");
    }

    @Test
    @Order(3)
    @DisplayName("03. Unregistered API: Register duplicate username (should fail)")
    void test03_registerDuplicateUsername() {
        System.out.println("\n========== TEST 03: DUPLICATE USERNAME REGISTRATION ==========");

        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(TEST_USERNAME_1); // Same username
        registerDTO.setEmail("different@test.com");
        registerDTO.setPassword(TEST_PASSWORD);
        registerDTO.setCountry("US");

        assertThrows(RuntimeException.class, () -> {
            authService.register(registerDTO);
        }, "Should throw exception for duplicate username");

        System.out.println("✓ Duplicate username registration correctly rejected");
        System.out.println("========== DUPLICATE USERNAME TEST PASSED ==========\n");
    }

    @Test
    @Order(4)
    @DisplayName("04. Unregistered API: Register duplicate email (should fail)")
    void test04_registerDuplicateEmail() {
        System.out.println("\n========== TEST 04: DUPLICATE EMAIL REGISTRATION ==========");

        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("differentuser");
        registerDTO.setEmail(TEST_EMAIL_1); // Same email
        registerDTO.setPassword(TEST_PASSWORD);
        registerDTO.setCountry("US");

        assertThrows(RuntimeException.class, () -> {
            authService.register(registerDTO);
        }, "Should throw exception for duplicate email");

        System.out.println("✓ Duplicate email registration correctly rejected");
        System.out.println("========== DUPLICATE EMAIL TEST PASSED ==========\n");
    }

    @Test
    @Order(5)
    @DisplayName("05. Unregistered API: Register second and third users")
    void test05_registerMoreUsers() {
        System.out.println("\n========== TEST 05: REGISTER ADDITIONAL USERS ==========");

        // Register user 2
        RegisterDTO registerDTO2 = new RegisterDTO();
        registerDTO2.setUsername(TEST_USERNAME_2);
        registerDTO2.setEmail(TEST_EMAIL_2);
        registerDTO2.setPassword(TEST_PASSWORD);
        registerDTO2.setCountry("US");

        UserDTO userDTO2 = authService.register(registerDTO2);
        testUserId2 = userDTO2.getId();
        assertNotNull(testUserId2);
        System.out.println("✓ User 2 registered: " + testUserId2);

        // Register user 3
        RegisterDTO registerDTO3 = new RegisterDTO();
        registerDTO3.setUsername(TEST_USERNAME_3);
        registerDTO3.setEmail(TEST_EMAIL_3);
        registerDTO3.setPassword(TEST_PASSWORD);
        registerDTO3.setCountry("UK");

        UserDTO userDTO3 = authService.register(registerDTO3);
        testUserId3 = userDTO3.getId();
        assertNotNull(testUserId3);
        System.out.println("✓ User 3 registered: " + testUserId3);

        System.out.println("========== ADDITIONAL USERS REGISTERED ==========\n");
    }

    @Test
    @Order(6)
    @DisplayName("06. Unregistered API: Login with correct credentials")
    void test06_loginSuccess() {
        System.out.println("\n========== TEST 06: SUCCESSFUL LOGIN ==========");

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsernameOrEmail(TEST_USERNAME_1);
        loginDTO.setPassword(TEST_PASSWORD);

        UserDTO userDTO = authService.login(loginDTO);
        
        assertNotNull(userDTO);
        assertEquals(testUserId1, userDTO.getId());
        assertEquals(TEST_USERNAME_1, userDTO.getUsername());
        assertEquals("active", userDTO.getStatus());
        System.out.println("✓ Login successful for user: " + userDTO.getUsername());

        System.out.println("========== LOGIN TEST PASSED ==========\n");
    }

    @Test
    @Order(7)
    @DisplayName("07. Unregistered API: Login with wrong password (should fail)")
    void test07_loginWrongPassword() {
        System.out.println("\n========== TEST 07: LOGIN WITH WRONG PASSWORD ==========");

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsernameOrEmail(TEST_USERNAME_1);
        loginDTO.setPassword("WrongPassword123!");

        assertThrows(RuntimeException.class, () -> {
            authService.login(loginDTO);
        }, "Should throw exception for wrong password");

        System.out.println("✓ Login correctly rejected for wrong password");
        System.out.println("========== WRONG PASSWORD TEST PASSED ==========\n");
    }

    @Test
    @Order(8)
    @DisplayName("08. Unregistered API: Login with non-existent user (should fail)")
    void test08_loginNonExistentUser() {
        System.out.println("\n========== TEST 08: LOGIN WITH NON-EXISTENT USER ==========");

        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsernameOrEmail("nonexistentuser");
        loginDTO.setPassword(TEST_PASSWORD);

        assertThrows(RuntimeException.class, () -> {
            authService.login(loginDTO);
        }, "Should throw exception for non-existent user");

        System.out.println("✓ Login correctly rejected for non-existent user");
        System.out.println("========== NON-EXISTENT USER TEST PASSED ==========\n");
    }

    // ==========================================================
    // PART 3: REGISTERED USER API - REVIEW OPERATIONS
    // ==========================================================

    @Test
    @Order(9)
    @DisplayName("09. Registered API: Create review successfully")
    void test09_createReview() throws InterruptedException {
        System.out.println("\n========== TEST 09: CREATE REVIEW ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setBookId(testBookId1);
        reviewDTO.setRating(85);
        reviewDTO.setText("This is an excellent book! Highly recommended.");
        reviewDTO.setSummary("Excellent read");

        ReviewDTO created = reviewService.createReview(reviewDTO);
        testReviewId1 = created.getId();
        
        assertNotNull(testReviewId1);
        assertEquals(85, created.getRating());
        assertNotNull(created.getUsername());
        System.out.println("✓ Review created: " + testReviewId1);

        waitForAsyncOperations();

        // Verify MongoDB
        Optional<Review> reviewOpt = reviewRepository.findById(testReviewId1);
        assertTrue(reviewOpt.isPresent(), "Review should exist in MongoDB");
        Review review = reviewOpt.get();
        assertEquals(testUserId1, review.getUserId());
        assertEquals(85, review.getRating());
        assertEquals("This is an excellent book! Highly recommended.", review.getText());
        System.out.println("✓ Review verified in MongoDB");

        // Verify Neo4j ReviewNode
        Optional<ReviewNode> reviewNodeOpt = reviewNodeRepository.findByMongoId(testReviewId1);
        assertTrue(reviewNodeOpt.isPresent(), "ReviewNode should exist in Neo4j");
        assertEquals(85, reviewNodeOpt.get().getRating());
        System.out.println("✓ ReviewNode verified in Neo4j");

        // Verify book statistics
        Optional<BookDocument> bookOpt = bookRepository.findById(testBookId1);
        assertTrue(bookOpt.isPresent());
        BookDocument book = bookOpt.get();
        assertTrue(book.getReviews().contains(testReviewId1));
        System.out.println("✓ Book statistics updated");

        System.out.println("========== CREATE REVIEW TEST PASSED ==========\n");
    }

    @Test
    @Order(10)
    @DisplayName("10. Registered API: Create second review by different user")
    void test10_createSecondReview() throws InterruptedException {
        System.out.println("\n========== TEST 10: CREATE SECOND REVIEW ==========");

        setupSecurityContext(testUserId2, TEST_USERNAME_2);

        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setBookId(testBookId2);
        reviewDTO.setRating(90);
        reviewDTO.setText("Amazing book! One of the best I've read.");
        reviewDTO.setSummary("Outstanding");

        ReviewDTO created = reviewService.createReview(reviewDTO);
        testReviewId2 = created.getId();
        
        assertNotNull(testReviewId2);
        assertEquals(90, created.getRating());
        assertNotNull(created.getUsername());
        System.out.println("✓ Second review created: " + testReviewId2);

        waitForAsyncOperations();

        System.out.println("========== SECOND REVIEW TEST PASSED ==========\n");
    }

    @Test
    @Order(11)
    @DisplayName("11. Registered API: Update review successfully")
    void test11_updateReview() throws InterruptedException {
        System.out.println("\n========== TEST 11: UPDATE REVIEW ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        ReviewDTO updateDTO = new ReviewDTO();
        updateDTO.setRating(95);
        updateDTO.setText("Updated: This book is absolutely fantastic!");
        updateDTO.setSummary("Perfect");

        ReviewDTO updated = reviewService.updateReview(testReviewId1, updateDTO);
        
        assertEquals(95, updated.getRating());
        assertEquals("Updated: This book is absolutely fantastic!", updated.getText());
        System.out.println("✓ Review updated from rating 85 to 95");

        waitForAsyncOperations();

        // Verify MongoDB
        Optional<Review> reviewOpt = reviewRepository.findById(testReviewId1);
        assertTrue(reviewOpt.isPresent());
        assertEquals(95, reviewOpt.get().getRating());
        System.out.println("✓ Review update verified in MongoDB");

        // Verify Neo4j
        Optional<ReviewNode> reviewNodeOpt = reviewNodeRepository.findByMongoId(testReviewId1);
        assertTrue(reviewNodeOpt.isPresent());
        assertEquals(95, reviewNodeOpt.get().getRating());
        System.out.println("✓ ReviewNode update verified in Neo4j");

        System.out.println("========== UPDATE REVIEW TEST PASSED ==========\n");
    }

    @Test
    @Order(12)
    @DisplayName("12. Registered API: Try to create duplicate review (should fail)")
    void test12_createDuplicateReview() {
        System.out.println("\n========== TEST 12: DUPLICATE REVIEW CREATION ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setBookId(testBookId1); // User1 already has a review for this book
        reviewDTO.setRating(80);
        reviewDTO.setText("Another review");

        assertThrows(RuntimeException.class, () -> {
            reviewService.createReview(reviewDTO);
        }, "Should not allow duplicate review from same user for same book");

        System.out.println("✓ Duplicate review correctly rejected");
        System.out.println("========== DUPLICATE REVIEW TEST PASSED ==========\n");
    }

    // ==========================================================
    // PART 4: REGISTERED USER API - LIKE OPERATIONS
    // ==========================================================

    @Test
    @Order(13)
    @DisplayName("13. Registered API: Like a book")
    void test13_likeBook() {
        System.out.println("\n========== TEST 13: LIKE BOOK ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        likeService.likeBook(testBookId1);
        System.out.println("✓ Book liked: " + testBookId1);

        // Verify Neo4j relationship
        boolean liked = bookNodeRepository.userLikesBook(testUserId1, testBookId1);
        assertTrue(liked, "LIKES relationship should exist in Neo4j");
        System.out.println("✓ LIKES relationship verified in Neo4j");

        System.out.println("========== LIKE BOOK TEST PASSED ==========\n");
    }

    @Test
    @Order(14)
    @DisplayName("14. Registered API: Like a review")
    void test14_likeReview() throws InterruptedException {
        System.out.println("\n========== TEST 14: LIKE REVIEW ==========");

        setupSecurityContext(testUserId2, TEST_USERNAME_2); // User2 likes User1's review

        Review reviewBefore = reviewRepository.findById(testReviewId1).orElseThrow();
        int initialLikes = reviewBefore.getLikesCount() != null ? reviewBefore.getLikesCount() : 0;

        likeService.likeReview(testReviewId1);
        System.out.println("✓ Review liked: " + testReviewId1);

        waitForAsyncOperations();

        // Verify MongoDB likes count
        Review reviewAfter = reviewRepository.findById(testReviewId1).orElseThrow();
        assertEquals(initialLikes + 1, reviewAfter.getLikesCount());
        System.out.println("✓ Likes count incremented in MongoDB: " + reviewAfter.getLikesCount());

        // Verify Neo4j relationship
        boolean liked = reviewNodeRepository.userLikesReview(testUserId2, testReviewId1);
        assertTrue(liked, "LIKES relationship should exist in Neo4j");
        System.out.println("✓ LIKES relationship verified in Neo4j");

        System.out.println("========== LIKE REVIEW TEST PASSED ==========\n");
    }

    @Test
    @Order(15)
    @DisplayName("15. Registered API: Like an author")
    void test15_likeAuthor() {
        System.out.println("\n========== TEST 15: LIKE AUTHOR ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        likeService.likeAuthor(testAuthorId1);
        System.out.println("✓ Author liked: " + testAuthorId1);

        // Verify Neo4j relationship
        boolean liked = authorNodeRepository.userLikesAuthor(testUserId1, testAuthorId1);
        assertTrue(liked, "LIKES relationship should exist for author");
        System.out.println("✓ Author LIKES relationship verified in Neo4j");

        System.out.println("========== LIKE AUTHOR TEST PASSED ==========\n");
    }

    @Test
    @Order(16)
    @DisplayName("16. Registered API: Like a genre")
    void test16_likeGenre() {
        System.out.println("\n========== TEST 16: LIKE GENRE ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        likeService.likeGenre(TEST_GENRE_1);
        System.out.println("✓ Genre liked: " + TEST_GENRE_1);

        // Verify Neo4j relationship (genre names are normalized to Title Case)
        String normalizedGenre = it.unipi.bookSphere.utils.NormalizationUtils.normalizeGenreName(TEST_GENRE_1);
        boolean liked = genreNodeRepository.userLikesGenre(testUserId1, normalizedGenre);
        assertTrue(liked, "LIKES relationship should exist for genre");
        System.out.println("✓ Genre LIKES relationship verified in Neo4j");

        System.out.println("========== LIKE GENRE TEST PASSED ==========\n");
    }

    @Test
    @Order(17)
    @DisplayName("17. Registered API: Like same book twice (should throw exception)")
    void test17_likeSameBookTwice() {
        System.out.println("\n========== TEST 17: LIKE SAME BOOK TWICE ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        // Like the same book again (should throw exception - not idempotent)
        assertThrows(Exception.class, () -> {
            likeService.likeBook(testBookId1);
        }, "Liking same book twice should throw exception");

        boolean liked = bookNodeRepository.userLikesBook(testUserId1, testBookId1);
        assertTrue(liked, "LIKES relationship should still exist from first like");
        System.out.println("✓ Duplicate like correctly rejected with exception");

        System.out.println("========== DUPLICATE LIKE TEST PASSED ==========\n");
    }

    // ==========================================================
    // PART 5: REGISTERED USER API - UNLIKE OPERATIONS
    // ==========================================================

    @Test
    @Order(18)
    @DisplayName("18. Registered API: Unlike a review")
    void test18_unlikeReview() throws InterruptedException {
        System.out.println("\n========== TEST 18: UNLIKE REVIEW ==========");

        setupSecurityContext(testUserId2, TEST_USERNAME_2);

        Review reviewBefore = reviewRepository.findById(testReviewId1).orElseThrow();
        int likesBefore = reviewBefore.getLikesCount();

        likeService.unlikeReview(testReviewId1);
        System.out.println("✓ Review unliked");

        waitForAsyncOperations();

        // Verify likes count decremented
        Review reviewAfter = reviewRepository.findById(testReviewId1).orElseThrow();
        assertEquals(likesBefore - 1, reviewAfter.getLikesCount());
        System.out.println("✓ Likes count decremented in MongoDB");

        // Verify Neo4j relationship removed
        boolean stillLiked = reviewNodeRepository.userLikesReview(testUserId2, testReviewId1);
        assertFalse(stillLiked, "LIKES relationship should be removed");
        System.out.println("✓ LIKES relationship removed from Neo4j");

        System.out.println("========== UNLIKE REVIEW TEST PASSED ==========\n");
    }

    @Test
    @Order(19)
    @DisplayName("19. Registered API: Unlike a book")
    void test19_unlikeBook() {
        System.out.println("\n========== TEST 19: UNLIKE BOOK ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        likeService.unlikeBook(testBookId1);
        System.out.println("✓ Book unliked");

        // Verify Neo4j relationship removed
        boolean stillLiked = bookNodeRepository.userLikesBook(testUserId1, testBookId1);
        assertFalse(stillLiked, "LIKES relationship should be removed");
        System.out.println("✓ Book LIKES relationship removed from Neo4j");

        System.out.println("========== UNLIKE BOOK TEST PASSED ==========\n");
    }

    @Test
    @Order(20)
    @DisplayName("20. Registered API: Unlike an author")
    void test20_unlikeAuthor() {
        System.out.println("\n========== TEST 20: UNLIKE AUTHOR ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        likeService.unlikeAuthor(testAuthorId1);
        System.out.println("✓ Author unliked");

        // Verify Neo4j relationship removed
        boolean stillLiked = authorNodeRepository.userLikesAuthor(testUserId1, testAuthorId1);
        assertFalse(stillLiked, "LIKES relationship should be removed");
        System.out.println("✓ Author LIKES relationship removed from Neo4j");

        System.out.println("========== UNLIKE AUTHOR TEST PASSED ==========\n");
    }

    @Test
    @Order(21)
    @DisplayName("21. Registered API: Unlike a genre")
    void test21_unlikeGenre() {
        System.out.println("\n========== TEST 21: UNLIKE GENRE ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        likeService.unlikeGenre(TEST_GENRE_1);
        System.out.println("✓ Genre unliked");

        // Verify Neo4j relationship removed
        boolean stillLiked = genreNodeRepository.userLikesGenre(testUserId1, TEST_GENRE_1);
        assertFalse(stillLiked, "LIKES relationship should be removed");
        System.out.println("✓ Genre LIKES relationship removed from Neo4j");

        System.out.println("========== UNLIKE GENRE TEST PASSED ==========\n");
    }

    // ==========================================================
    // PART 6: REGISTERED USER API - FOLLOW OPERATIONS
    // ==========================================================

    @Test
    @Order(22)
    @DisplayName("22. Registered API: Follow a user")
    void test22_followUser() {
        System.out.println("\n========== TEST 22: FOLLOW USER ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        followService.followUser(testUserId2);
        System.out.println("✓ User followed");

        // Verify Neo4j relationship
        boolean follows = userNodeRepository.userFollowsUser(testUserId1, testUserId2);
        assertTrue(follows, "FOLLOWS relationship should exist in Neo4j");
        System.out.println("✓ FOLLOWS relationship verified in Neo4j");

        System.out.println("========== FOLLOW USER TEST PASSED ==========\n");
    }

    @Test
    @Order(23)
    @DisplayName("23. Registered API: Follow multiple users")
    void test23_followMultipleUsers() {
        System.out.println("\n========== TEST 23: FOLLOW MULTIPLE USERS ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        followService.followUser(testUserId3);
        System.out.println("✓ User 3 followed");

        // User 2 also follows User 3
        setupSecurityContext(testUserId2, TEST_USERNAME_2);
        followService.followUser(testUserId3);
        System.out.println("✓ User 2 also follows User 3");

        // Verify relationships
        boolean user1FollowsUser3 = userNodeRepository.userFollowsUser(testUserId1, testUserId3);
        boolean user2FollowsUser3 = userNodeRepository.userFollowsUser(testUserId2, testUserId3);
        assertTrue(user1FollowsUser3, "User1 should follow User3");
        assertTrue(user2FollowsUser3, "User2 should follow User3");
        System.out.println("✓ Multiple FOLLOWS relationships verified");

        System.out.println("========== FOLLOW MULTIPLE USERS TEST PASSED ==========\n");
    }

    @Test
    @Order(24)
    @DisplayName("24. Registered API: Unfollow a user")
    void test24_unfollowUser() {
        System.out.println("\n========== TEST 24: UNFOLLOW USER ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        followService.unfollowUser(testUserId2);
        System.out.println("✓ User unfollowed");

        // Verify Neo4j relationship removed
        boolean stillFollows = userNodeRepository.userFollowsUser(testUserId1, testUserId2);
        assertFalse(stillFollows, "FOLLOWS relationship should be removed");
        System.out.println("✓ FOLLOWS relationship removed from Neo4j");

        System.out.println("========== UNFOLLOW USER TEST PASSED ==========\n");
    }

    @Test
    @Order(25)
    @DisplayName("25. Registered API: Try to follow self (should throw exception)")
    void test25_followSelf() {
        System.out.println("\n========== TEST 25: FOLLOW SELF ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        // Attempting to follow self should throw an exception
        assertThrows(Exception.class, () -> {
            followService.followUser(testUserId1);
        }, "Following self should throw an exception");

        System.out.println("✓ Follow self correctly rejected with exception");
        System.out.println("========== FOLLOW SELF TEST PASSED ==========\n");
    }

    // ==========================================================
    // PART 7: REGISTERED USER API - BOOKSHELF OPERATIONS
    // ==========================================================

    @Test
    @Order(26)
    @DisplayName("26. Registered API: Add book to bookshelf")
    void test26_addToBookshelf() {
        System.out.println("\n========== TEST 26: ADD TO BOOKSHELF ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        bookshelfService.addBookToBookshelf(testBookId1, "want_to_read");
        System.out.println("✓ Book added to bookshelf with status: want_to_read");

        // Verify MongoDB
        Optional<RegisteredUser> userOpt = userRepository.findById(testUserId1);
        assertTrue(userOpt.isPresent());
        RegisteredUser user = userOpt.get();
        assertNotNull(user.getBookshelf());
        boolean bookInShelf = user.getBookshelf().stream()
                .anyMatch(item -> item.getBookId().equals(testBookId1));
        assertTrue(bookInShelf, "Book should be in bookshelf");
        System.out.println("✓ Book verified in bookshelf (MongoDB)");

        System.out.println("========== ADD TO BOOKSHELF TEST PASSED ==========\n");
    }

    @Test
    @Order(27)
    @DisplayName("27. Registered API: Update bookshelf status")
    void test27_updateBookshelfStatus() {
        System.out.println("\n========== TEST 27: UPDATE BOOKSHELF STATUS ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        bookshelfService.updateBookStatus(testBookId1, "currently_reading");
        System.out.println("✓ Bookshelf status updated to: currently_reading");

        // Verify update
        Optional<RegisteredUser> userOpt = userRepository.findById(testUserId1);
        assertTrue(userOpt.isPresent());
        RegisteredUser user = userOpt.get();
        Optional<RegisteredUser.BookshelfItem> bookItem = user.getBookshelf().stream()
                .filter(item -> item.getBookId().equals(testBookId1))
                .findFirst();
        assertTrue(bookItem.isPresent());
        assertEquals("currently_reading", bookItem.get().getStatus());
        System.out.println("✓ Bookshelf status updated verified");

        System.out.println("========== UPDATE BOOKSHELF STATUS TEST PASSED ==========\n");
    }

    @Test
    @Order(28)
    @DisplayName("28. Registered API: Add multiple books to bookshelf")
    void test28_addMultipleBooksToBookshelf() {
        System.out.println("\n========== TEST 28: ADD MULTIPLE BOOKS ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        bookshelfService.addBookToBookshelf(testBookId2, "read");
        System.out.println("✓ Second book added with status: read");

        setupSecurityContext(testUserId2, TEST_USERNAME_2);
        bookshelfService.addBookToBookshelf(testBookId1, "want_to_read");
        bookshelfService.addBookToBookshelf(testBookId2, "currently_reading");
        System.out.println("✓ User2 added books to bookshelf");

        // Verify User1 has 2 books
        Optional<RegisteredUser> user1Opt = userRepository.findById(testUserId1);
        assertTrue(user1Opt.isPresent());
        assertEquals(2, user1Opt.get().getBookshelf().size());
        System.out.println("✓ User1 has 2 books in bookshelf");

        // Verify User2 has 2 books
        Optional<RegisteredUser> user2Opt = userRepository.findById(testUserId2);
        assertTrue(user2Opt.isPresent());
        assertEquals(2, user2Opt.get().getBookshelf().size());
        System.out.println("✓ User2 has 2 books in bookshelf");

        System.out.println("========== MULTIPLE BOOKS TEST PASSED ==========\n");
    }

    @Test
    @Order(29)
    @DisplayName("29. Registered API: Remove book from bookshelf")
    void test29_removeFromBookshelf() {
        System.out.println("\n========== TEST 29: REMOVE FROM BOOKSHELF ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        bookshelfService.removeBookFromBookshelf(testBookId2);
        System.out.println("✓ Book removed from bookshelf");

        // Verify removal
        Optional<RegisteredUser> userOpt = userRepository.findById(testUserId1);
        assertTrue(userOpt.isPresent());
        RegisteredUser user = userOpt.get();
        boolean bookStillInShelf = user.getBookshelf().stream()
                .anyMatch(item -> item.getBookId().equals(testBookId2));
        assertFalse(bookStillInShelf, "Book should not be in bookshelf");
        System.out.println("✓ Book removal verified");

        System.out.println("========== REMOVE FROM BOOKSHELF TEST PASSED ==========\n");
    }

    // ==========================================================
    // PART 8: REGISTERED USER API - PROFILE OPERATIONS
    // ==========================================================

    @Test
    @Order(30)
    @DisplayName("30. Registered API: Update username")
    void test30_updateUsername() throws InterruptedException {
        System.out.println("\n========== TEST 30: UPDATE USERNAME ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        String newUsername = TEST_USERNAME_1 + "_updated";
        profileService.updateUsername(newUsername);
        System.out.println("✓ Username updated to: " + newUsername);

        waitForAsyncOperations();

        // Verify MongoDB
        Optional<RegisteredUser> userOpt = userRepository.findById(testUserId1);
        assertTrue(userOpt.isPresent());
        assertEquals(newUsername, userOpt.get().getUsername());
        System.out.println("✓ Username verified in MongoDB");

        // Verify Neo4j
        Optional<UserNode> userNodeOpt = userNodeRepository.findByMongoId(testUserId1);
        assertTrue(userNodeOpt.isPresent());
        assertEquals(newUsername, userNodeOpt.get().getUsername());
        System.out.println("✓ Username verified in Neo4j");

        System.out.println("========== UPDATE USERNAME TEST PASSED ==========\n");
    }

    @Test
    @Order(31)
    @DisplayName("31. Registered API: Update username to existing name (should fail)")
    void test31_updateToExistingUsername() {
        System.out.println("\n========== TEST 31: UPDATE TO EXISTING USERNAME ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        assertThrows(RuntimeException.class, () -> {
            profileService.updateUsername(TEST_USERNAME_2); // Try to use User2's name
        }, "Should not allow updating to existing username");

        System.out.println("✓ Update to existing username correctly rejected");
        System.out.println("========== EXISTING USERNAME TEST PASSED ==========\n");
    }

    // ==========================================================
    // PART 9: REGISTERED USER API - DELETE REVIEW
    // ==========================================================

    @Test
    @Order(32)
    @DisplayName("32. Registered API: Delete review")
    void test32_deleteReview() throws InterruptedException {
        System.out.println("\n========== TEST 32: DELETE REVIEW ==========");

        setupSecurityContext(testUserId1, TEST_USERNAME_1);

        reviewService.deleteReview(testReviewId1);
        System.out.println("✓ Review deleted");

        waitForAsyncOperations();

        // Verify MongoDB
        Optional<Review> reviewOpt = reviewRepository.findById(testReviewId1);
        assertFalse(reviewOpt.isPresent(), "Review should be deleted from MongoDB");
        System.out.println("✓ Review deleted from MongoDB");

        // Verify Neo4j
        Optional<ReviewNode> reviewNodeOpt = reviewNodeRepository.findByMongoId(testReviewId1);
        assertFalse(reviewNodeOpt.isPresent(), "ReviewNode should be deleted from Neo4j");
        System.out.println("✓ ReviewNode deleted from Neo4j");

        // Verify book no longer has this review
        Optional<BookDocument> bookOpt = bookRepository.findById(testBookId1);
        assertTrue(bookOpt.isPresent());
        assertFalse(bookOpt.get().getReviews().contains(testReviewId1));
        System.out.println("✓ Review removed from book's review list");

        System.out.println("========== DELETE REVIEW TEST PASSED ==========\n");
    }

    // ==========================================================
    // PART 10: REGISTERED USER API - DELETE ACCOUNT
    // ==========================================================

    @Test
    @Order(33)
    @DisplayName("33. Registered API: Delete user account")
    void test33_deleteAccount() throws InterruptedException {
        System.out.println("\n========== TEST 33: DELETE ACCOUNT ==========");

        setupSecurityContext(testUserId3, TEST_USERNAME_3);

        profileService.deleteAccount();
        System.out.println("✓ Account deletion requested");

        waitForAsyncOperations();

        // Verify MongoDB - user should be marked as deleted
        Optional<RegisteredUser> userOpt = userRepository.findById(testUserId3);
        assertTrue(userOpt.isPresent(), "User document should still exist in MongoDB");
        RegisteredUser user = userOpt.get();
        assertEquals("deleted", user.getStatus(), "User status should be 'deleted'");
        assertNull(user.getUsername(), "Username should be removed from MongoDB");
        assertNull(user.getEmail(), "Email should be removed from MongoDB");
        System.out.println("✓ User anonymized in MongoDB (status=deleted, username=null)");

        // Verify Neo4j - UserNode should REMAIN but with username set to ANONYMOUS
        // According to documentation: interactions must remain accessible, node stays
        Optional<UserNode> userNodeOpt = userNodeRepository.findByMongoId(testUserId3);
        assertTrue(userNodeOpt.isPresent(), "UserNode should still exist in Neo4j (interactions must remain)");
        UserNode userNode = userNodeOpt.get();
        assertEquals("ANONYMOUS", userNode.getUsername(), "Username should be set to ANONYMOUS in Neo4j");
        System.out.println("✓ UserNode anonymized in Neo4j (username=ANONYMOUS, node preserved)");

        System.out.println("========== DELETE ACCOUNT TEST PASSED ==========\n");
    }

    // ==========================================================
    // PART 11: CLEANUP
    // ==========================================================

    @Test
    @Order(34)
    @DisplayName("34. Cleanup: Remove all test data")
    void test34_cleanup() {
        System.out.println("\n========== TEST 34: CLEANUP ==========");

        // Delete users
        userRepository.deleteById(testUserId1);
        userRepository.deleteById(testUserId2);
        if (userRepository.existsById(testUserId3)) {
            userRepository.deleteById(testUserId3);
        }
        userNodeRepository.deleteByMongoId(testUserId1);
        userNodeRepository.deleteByMongoId(testUserId2);
        userNodeRepository.deleteByMongoId(testUserId3);
        System.out.println("✓ Users deleted");

        // Delete reviews (if any still exist)
        if (reviewRepository.existsById(testReviewId2)) {
            reviewRepository.deleteById(testReviewId2);
            reviewNodeRepository.deleteByMongoId(testReviewId2);
        }
        System.out.println("✓ Reviews deleted");

        // Delete books
        bookRepository.deleteById(testBookId1);
        bookRepository.deleteById(testBookId2);
        bookNodeRepository.deleteByMongoId(testBookId1);
        bookNodeRepository.deleteByMongoId(testBookId2);
        System.out.println("✓ Books deleted");

        // Delete authors
        authorRepository.deleteById(testAuthorId1);
        authorRepository.deleteById(testAuthorId2);
        authorNodeRepository.deleteByMongoId(testAuthorId1);
        authorNodeRepository.deleteByMongoId(testAuthorId2);
        System.out.println("✓ Authors deleted");

        System.out.println("========== CLEANUP COMPLETED ==========\n");
    }
}

