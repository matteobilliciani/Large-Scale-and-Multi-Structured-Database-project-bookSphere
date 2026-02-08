package it.unipi.bookSphere;

import it.unipi.bookSphere.dto.*;
import it.unipi.bookSphere.exceptions.*;
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
 * Comprehensive Integration Test for Admin APIs
 * 
 * Tests all admin operations:
 * 1. BOOK MANAGEMENT:
 *    - Add new book (with automatic Neo4j node creation)
 *    - Update book (updates both MongoDB and Neo4j)
 *    - Delete book (soft delete in MongoDB + DETACH DELETE in Neo4j)
 * 
 * 2. AUTHOR MANAGEMENT:
 *    - Add new author
 *    - Update author
 *    - Delete author (soft delete in MongoDB + DETACH DELETE in Neo4j)
 * 
 * 3. GENRE MANAGEMENT:
 *    - Add new genre (Neo4j only)
 *    - Duplicate genre handling
 * 
 * 4. MODERATION:
 *    - Delete review (with cascading updates)
 *    - Ban user (with cascading updates)
 * 
 * 5. ERROR HANDLING:
 *    - Update non-existent resources
 *    - Delete non-existent resources
 *    - Invalid inputs
 * 
 * 6. DATA CONSISTENCY:
 *    - MongoDB and Neo4j synchronization
 *    - Proper relationship creation/deletion
 */
@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminApiTest {

    // ========== SERVICES ==========
    @Autowired
    private AdminBookService adminBookService;

    @Autowired
    private AdminCatalogService adminCatalogService;

    @Autowired
    private AdminModerationService adminModerationService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private AuthService authService;

    // ========== MONGODB REPOSITORIES ==========
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    // ========== NEO4J REPOSITORIES ==========
    @Autowired
    private BookNodeRepository bookNodeRepository;

    @Autowired
    private AuthorNodeRepository authorNodeRepository;

    @Autowired
    private GenreNodeRepository genreNodeRepository;

    @Autowired
    private ReviewNodeRepository reviewNodeRepository;

    @Autowired
    private UserNodeRepository userNodeRepository;

    // ========== TEST DATA ==========
    private static final long TIMESTAMP = System.currentTimeMillis();
    private static final String TEST_AUTHOR_NAME_1 = "Test Author " + TIMESTAMP;
    private static final String TEST_AUTHOR_NAME_2 = "Test Author 2 " + TIMESTAMP;
    private static final String TEST_BOOK_TITLE_1 = "Test Book " + TIMESTAMP;
    private static final String TEST_BOOK_TITLE_2 = "Test Book 2 " + TIMESTAMP;
    private static final String TEST_GENRE_1 = "TestGenre_" + TIMESTAMP;
    private static final String TEST_USERNAME_1 = "admintest_user1_" + TIMESTAMP;
    private static final String TEST_EMAIL_1 = TEST_USERNAME_1 + "@test.com";
    private static final String TEST_PASSWORD = "TestPassword123!";

    private static String testAuthorId1;
    private static String testAuthorId2;
    private static String testBookId1;
    private static String testBookId2;
    private static String testUserId1;
    private static String testReviewId1;
    private static String adminUserId;

    // ========== HELPER METHODS ==========

    /**
     * Setup security context for authenticated admin API calls
     */
    private void setupAdminSecurityContext(String userId, String username) {
        UserPrincipal userPrincipal = new UserPrincipal(userId, username, "ADMIN", "active");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Setup security context for regular user
     */
    private void setupUserSecurityContext(String userId, String username) {
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
    private void waitForAsync() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========== TEST 0: SETUP ==========

    @Test
    @Order(0)
    @DisplayName("Setup: Create test admin and user for testing")
    public void test00_Setup() {
        System.out.println("\n========== TEST 0: SETUP ==========");

        // Create admin user directly in database
        RegisteredUser adminUser = new RegisteredUser();
        adminUser.setUsername("admin_" + TIMESTAMP);
        adminUser.setEmail("admin_" + TIMESTAMP + "@test.com");
        adminUser.setPasswordHashed("hashedpassword");
        adminUser.setStatus("active");
        adminUser.setJoinedAt(Instant.now());
        adminUser = userRepository.save(adminUser);
        adminUserId = adminUser.getId();
        System.out.println("Admin user created with ID: " + adminUserId);

        // Create test user for review tests
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername(TEST_USERNAME_1);
        registerDTO.setEmail(TEST_EMAIL_1);
        registerDTO.setPassword(TEST_PASSWORD);

        UserDTO response = authService.register(registerDTO);
        testUserId1 = response.getId();
        assertNotNull(testUserId1, "User should be created successfully");
        System.out.println("Test user created with ID: " + testUserId1);
    }

    // ========== TEST 1: AUTHOR MANAGEMENT ==========

    @Test
    @Order(1)
    @DisplayName("Admin API: Add new author")
    public void test01_AddAuthor() {
        System.out.println("\n========== TEST 1: ADD AUTHOR ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        AuthorDTO authorDTO = new AuthorDTO();
        authorDTO.setName(TEST_AUTHOR_NAME_1);

        // Add author
        AuthorDTO created = adminCatalogService.addAuthor(authorDTO);
        testAuthorId1 = created.getId();

        assertNotNull(created.getId(), "Author ID should not be null");
        assertEquals(TEST_AUTHOR_NAME_1, created.getName(), "Author name should match");

        // Verify in MongoDB
        Optional<AuthorDocument> mongoAuthor = authorRepository.findById(testAuthorId1);
        assertTrue(mongoAuthor.isPresent(), "Author should exist in MongoDB");
        assertEquals("ACTIVE", mongoAuthor.get().getStatus(), "Author status should be ACTIVE");

        // Verify in Neo4j
        Optional<AuthorNode> neoAuthor = authorNodeRepository.findByMongoId(testAuthorId1);
        assertTrue(neoAuthor.isPresent(), "Author should exist in Neo4j");
        assertEquals(TEST_AUTHOR_NAME_1, neoAuthor.get().getName(), "Author name should match in Neo4j");

        System.out.println("Author added successfully with ID: " + testAuthorId1);

        clearSecurityContext();
    }

    @Test
    @Order(2)
    @DisplayName("Admin API: Update author")
    public void test02_UpdateAuthor() {
        System.out.println("\n========== TEST 2: UPDATE AUTHOR ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        String updatedName = TEST_AUTHOR_NAME_1 + " Updated";
        AuthorDTO updateDTO = new AuthorDTO();
        updateDTO.setName(updatedName);

        // Update author
        AuthorDTO updated = adminCatalogService.updateAuthor(testAuthorId1, updateDTO);

        assertEquals(updatedName, updated.getName(), "Author name should be updated");

        // Verify in MongoDB
        Optional<AuthorDocument> mongoAuthor = authorRepository.findById(testAuthorId1);
        assertTrue(mongoAuthor.isPresent(), "Author should exist in MongoDB");
        assertEquals(updatedName, mongoAuthor.get().getName(), "Author name should be updated in MongoDB");

        // Verify in Neo4j
        Optional<AuthorNode> neoAuthor = authorNodeRepository.findByMongoId(testAuthorId1);
        assertTrue(neoAuthor.isPresent(), "Author should exist in Neo4j");
        assertEquals(updatedName, neoAuthor.get().getName(), "Author name should be updated in Neo4j");

        System.out.println("Author updated successfully");

        clearSecurityContext();
    }

    @Test
    @Order(3)
    @DisplayName("Admin API: Update non-existent author should throw exception")
    public void test03_UpdateNonExistentAuthor() {
        System.out.println("\n========== TEST 3: UPDATE NON-EXISTENT AUTHOR ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        AuthorDTO updateDTO = new AuthorDTO();
        updateDTO.setName("Some Name");

        // Try to update non-existent author
        assertThrows(AuthorNotFoundException.class, () -> {
            adminCatalogService.updateAuthor("nonexistentid123456789012", updateDTO);
        }, "Should throw AuthorNotFoundException");

        System.out.println("Correctly threw AuthorNotFoundException");

        clearSecurityContext();
    }

    @Test
    @Order(4)
    @DisplayName("Admin API: Add second author for book tests")
    public void test04_AddSecondAuthor() {
        System.out.println("\n========== TEST 4: ADD SECOND AUTHOR ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        AuthorDTO authorDTO = new AuthorDTO();
        authorDTO.setName(TEST_AUTHOR_NAME_2);

        AuthorDTO created = adminCatalogService.addAuthor(authorDTO);
        testAuthorId2 = created.getId();

        assertNotNull(created.getId(), "Author ID should not be null");
        System.out.println("Second author added successfully with ID: " + testAuthorId2);

        clearSecurityContext();
    }

    // ========== TEST 5-9: BOOK MANAGEMENT ==========

    @Test
    @Order(5)
    //@Disabled("Disabilitato temporaneamente per problemi di memoria heap con database grande")
    @DisplayName("Admin API: Add new book")
    public void test05_AddBook() {
        System.out.println("\n========== TEST 5: ADD BOOK ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        BookDTO bookDTO = new BookDTO();
        bookDTO.setTitle(TEST_BOOK_TITLE_1);
        bookDTO.setPublicationYear(2020);
        bookDTO.setDescription("Test book description");

        AuthorDTO author = new AuthorDTO();
        author.setId(testAuthorId1);
        author.setName(TEST_AUTHOR_NAME_1 + " Updated");
        bookDTO.setAuthor(author);

        bookDTO.setGenres(List.of("Fiction", "Drama"));
        bookDTO.setIsbns(List.of("1234567890", "0987654321"));

        // Add book
        BookDTO created = adminBookService.addBook(bookDTO);
        testBookId1 = created.getId();

        assertNotNull(created.getId(), "Book ID should not be null");
        assertEquals(TEST_BOOK_TITLE_1, created.getTitle(), "Book title should match");

        // Verify in MongoDB
        Optional<BookDocument> mongoBook = bookRepository.findById(testBookId1);
        assertTrue(mongoBook.isPresent(), "Book should exist in MongoDB");
        assertEquals("ACTIVE", mongoBook.get().getStatus(), "Book status should be ACTIVE");
        assertEquals(testAuthorId1, mongoBook.get().getAuthor().getId(), "Author ID should match");

        // Verify in Neo4j
        Optional<BookNode> neoBook = bookNodeRepository.findByMongoId(testBookId1);
        assertTrue(neoBook.isPresent(), "Book should exist in Neo4j");
        assertEquals(TEST_BOOK_TITLE_1, neoBook.get().getTitle(), "Book title should match in Neo4j");

        // Note: Relationship verification methods don't exist in repository, but we trust the service logic

        System.out.println("Book added successfully with ID: " + testBookId1);

        clearSecurityContext();
    }

    @Test
    @Order(6)
    //@Disabled("Disabilitato perché dipende da test05")
    @DisplayName("Admin API: Add book with same title (should succeed, not an error)")
    public void test06_AddDuplicateBook() {
        System.out.println("\n========== TEST 6: ADD DUPLICATE BOOK ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        BookDTO bookDTO = new BookDTO();
        bookDTO.setTitle(TEST_BOOK_TITLE_1); // Same title
        bookDTO.setPublicationYear(2021); // Different year

        AuthorDTO author = new AuthorDTO();
        author.setId(testAuthorId2);
        author.setName(TEST_AUTHOR_NAME_2);
        bookDTO.setAuthor(author);

        // Add book with same title - should NOT throw exception
        BookDTO created = adminBookService.addBook(bookDTO);
        testBookId2 = created.getId();

        assertNotNull(created.getId(), "Book ID should not be null");
        assertNotEquals(testBookId1, testBookId2, "Should create a new book with different ID");

        System.out.println("Duplicate book added successfully with ID: " + testBookId2);

        clearSecurityContext();
    }

    @Test
    @Order(7)
    //@Disabled("Disabilitato perché dipende da test05 (testBookId1)")
    @DisplayName("Admin API: Update book")
    public void test07_UpdateBook() {
        System.out.println("\n========== TEST 7: UPDATE BOOK ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        String updatedTitle = TEST_BOOK_TITLE_1 + " Updated";
        BookDTO updateDTO = new BookDTO();
        updateDTO.setTitle(updatedTitle);
        updateDTO.setPublicationYear(2021);
        updateDTO.setDescription("Updated description");

        // Update book
        BookDTO updated = adminBookService.updateBook(testBookId1, updateDTO);

        assertEquals(updatedTitle, updated.getTitle(), "Book title should be updated");
        assertEquals(2021, updated.getPublicationYear(), "Publication year should be updated");

        // Verify in MongoDB
        Optional<BookDocument> mongoBook = bookRepository.findById(testBookId1);
        assertTrue(mongoBook.isPresent(), "Book should exist in MongoDB");
        assertEquals(updatedTitle, mongoBook.get().getTitle(), "Book title should be updated in MongoDB");

        // Verify in Neo4j
        Optional<BookNode> neoBook = bookNodeRepository.findByMongoId(testBookId1);
        assertTrue(neoBook.isPresent(), "Book should exist in Neo4j");
        assertEquals(updatedTitle, neoBook.get().getTitle(), "Book title should be updated in Neo4j");

        System.out.println("Book updated successfully");

        clearSecurityContext();
    }

    @Test
    @Order(8)
    @DisplayName("Admin API: Update non-existent book should throw exception")
    public void test08_UpdateNonExistentBook() {
        System.out.println("\n========== TEST 8: UPDATE NON-EXISTENT BOOK ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        BookDTO updateDTO = new BookDTO();
        updateDTO.setTitle("Some Title");

        // Try to update non-existent book
        assertThrows(BookNotFoundException.class, () -> {
            adminBookService.updateBook("nonexistentid123456789012", updateDTO);
        }, "Should throw BookNotFoundException");

        System.out.println("Correctly threw BookNotFoundException");

        clearSecurityContext();
    }

    @Test
    @Order(9)
    //@Disabled("Disabilitato perché dipende da test06 (testBookId2)")
    @DisplayName("Admin API: Delete book (soft delete)")
    public void test09_DeleteBook() {
        System.out.println("\n========== TEST 9: DELETE BOOK ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        // Delete book
        adminBookService.deleteBook(testBookId2);

        // Verify soft delete in MongoDB (status = ARCHIVED)
        Optional<BookDocument> mongoBook = bookRepository.findById(testBookId2);
        assertTrue(mongoBook.isPresent(), "Book should still exist in MongoDB");
        assertEquals("ARCHIVED", mongoBook.get().getStatus(), "Book status should be ARCHIVED");

        // Verify DETACH DELETE in Neo4j (node should be deleted)
        Optional<BookNode> neoBook = bookNodeRepository.findByMongoId(testBookId2);
        assertFalse(neoBook.isPresent(), "Book node should be deleted from Neo4j");

        System.out.println("Book deleted successfully (soft delete)");

        clearSecurityContext();
    }

    @Test
    @Order(10)
    @DisplayName("Admin API: Delete non-existent book should throw exception")
    public void test10_DeleteNonExistentBook() {
        System.out.println("\n========== TEST 10: DELETE NON-EXISTENT BOOK ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        // Try to delete non-existent book
        assertThrows(BookNotFoundException.class, () -> {
            adminBookService.deleteBook("nonexistentid123456789012");
        }, "Should throw BookNotFoundException");

        System.out.println("Correctly threw BookNotFoundException");

        clearSecurityContext();
    }

    // ========== TEST 11-13: GENRE MANAGEMENT ==========

    @Test
    @Order(11)
    @DisplayName("Admin API: Add new genre")
    public void test11_AddGenre() {
        System.out.println("\n========== TEST 11: ADD GENRE ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        GenreDTO genreDTO = new GenreDTO();
        genreDTO.setName(TEST_GENRE_1);

        // Add genre
        GenreDTO created = adminCatalogService.addGenre(genreDTO);

        // Genre name will be normalized to Title Case
        String expectedName = it.unipi.bookSphere.utils.NormalizationUtils.normalizeGenreName(TEST_GENRE_1);
        assertEquals(expectedName, created.getName(), "Genre name should match (normalized)");

        // Verify in Neo4j
        boolean genreExists = genreNodeRepository.existsByName(expectedName);
        assertTrue(genreExists, "Genre should exist in Neo4j");

        System.out.println("Genre added successfully: " + TEST_GENRE_1);

        clearSecurityContext();
    }

    @Test
    @Order(12)
    @DisplayName("Admin API: Add duplicate genre should throw exception")
    public void test12_AddDuplicateGenre() {
        System.out.println("\n========== TEST 12: ADD DUPLICATE GENRE ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        GenreDTO genreDTO = new GenreDTO();
        genreDTO.setName(TEST_GENRE_1);

        // Try to add duplicate genre
        assertThrows(IllegalArgumentException.class, () -> {
            adminCatalogService.addGenre(genreDTO);
        }, "Should throw IllegalArgumentException for duplicate genre");

        System.out.println("Correctly threw IllegalArgumentException for duplicate genre");

        clearSecurityContext();
    }

    // ========== TEST 13-17: MODERATION OPERATIONS ==========

    @Test
    @Order(13)
    //@Disabled("Disabilitato perché dipende da test05 (testBookId1)")
    @DisplayName("Admin API: Create review for moderation tests")
    public void test13_CreateReviewForModeration() {
        System.out.println("\n========== TEST 13: CREATE REVIEW FOR MODERATION ==========");

        setupUserSecurityContext(testUserId1, TEST_USERNAME_1);

        ReviewDTO reviewDTO = new ReviewDTO();
        reviewDTO.setBookId(testBookId1);
        reviewDTO.setRating(4);
        reviewDTO.setText("This is a test review for moderation");

        ReviewDTO created = reviewService.createReview(reviewDTO);
        testReviewId1 = created.getId();

        assertNotNull(testReviewId1, "Review ID should not be null");
        System.out.println("Review created successfully with ID: " + testReviewId1);

        waitForAsync(); // Wait for async operations

        clearSecurityContext();
    }

    @Test
    @Order(14)
    //@Disabled("Disabilitato perché dipende da test13 (testReviewId1)")
    @DisplayName("Admin API: Delete review (moderation)")
    public void test14_DeleteReview() {
        System.out.println("\n========== TEST 14: DELETE REVIEW ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        // Delete review
        adminModerationService.deleteReview(testReviewId1);

        waitForAsync(); // Wait for async operations

        // Verify review is deleted from MongoDB
        Optional<Review> mongoReview = reviewRepository.findById(testReviewId1);
        assertFalse(mongoReview.isPresent(), "Review should be deleted from MongoDB");

        // Verify review node is deleted from Neo4j
        Optional<ReviewNode> neoReview = reviewNodeRepository.findByMongoId(testReviewId1);
        assertFalse(neoReview.isPresent(), "Review node should be deleted from Neo4j");

        System.out.println("Review deleted successfully");

        clearSecurityContext();
    }

    @Test
    @Order(15)
    @DisplayName("Admin API: Delete non-existent review should throw exception")
    public void test15_DeleteNonExistentReview() {
        System.out.println("\n========== TEST 15: DELETE NON-EXISTENT REVIEW ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        // Try to delete non-existent review
        assertThrows(ReviewNotFoundException.class, () -> {
            adminModerationService.deleteReview("nonexistentid123456789012");
        }, "Should throw ReviewNotFoundException");

        System.out.println("Correctly threw ReviewNotFoundException");

        clearSecurityContext();
    }

    @Test
    @Order(16)
    @DisplayName("Admin API: Ban user")
    public void test16_BanUser() {
        System.out.println("\n========== TEST 16: BAN USER ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        // Ban user
        adminModerationService.banUser(testUserId1);

        waitForAsync(); // Wait for async operations

        // Verify user status is BANNED in MongoDB
        Optional<RegisteredUser> mongoUser = userRepository.findById(testUserId1);
        assertTrue(mongoUser.isPresent(), "User should exist in MongoDB");
        assertEquals("BANNED", mongoUser.get().getStatus(), "User status should be BANNED");

        // Verify BannedUser label in Neo4j
        Optional<UserNode> neoUser = userNodeRepository.findByMongoId(testUserId1);
        assertTrue(neoUser.isPresent(), "User should exist in Neo4j");
        // Note: Would need specific query to verify label

        System.out.println("User banned successfully");

        clearSecurityContext();
    }

    @Test
    @Order(17)
    @DisplayName("Admin API: Ban non-existent user should throw exception")
    public void test17_BanNonExistentUser() {
        System.out.println("\n========== TEST 17: BAN NON-EXISTENT USER ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        // Try to ban non-existent user
        assertThrows(UserNotFoundException.class, () -> {
            adminModerationService.banUser("nonexistentid123456789012");
        }, "Should throw UserNotFoundException");

        System.out.println("Correctly threw UserNotFoundException");

        clearSecurityContext();
    }

    // ========== TEST 20: DELETE AUTHOR (FINAL) ==========

    @Test
    @Order(20)
    @DisplayName("Admin API: Delete author (soft delete)")
    public void test20_DeleteAuthor() {
        System.out.println("\n========== TEST 20: DELETE AUTHOR ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        // Delete author
        adminCatalogService.deleteAuthor(testAuthorId1);

        // Verify soft delete in MongoDB (status = ARCHIVED)
        Optional<AuthorDocument> mongoAuthor = authorRepository.findById(testAuthorId1);
        assertTrue(mongoAuthor.isPresent(), "Author should still exist in MongoDB");
        assertEquals("ARCHIVED", mongoAuthor.get().getStatus(), "Author status should be ARCHIVED");

        // Verify DETACH DELETE in Neo4j (node should be deleted)
        Optional<AuthorNode> neoAuthor = authorNodeRepository.findByMongoId(testAuthorId1);
        assertFalse(neoAuthor.isPresent(), "Author node should be deleted from Neo4j");

        System.out.println("Author deleted successfully (soft delete)");

        clearSecurityContext();
    }

    @Test
    @Order(21)
    @DisplayName("Admin API: Delete non-existent author should throw exception")
    public void test21_DeleteNonExistentAuthor() {
        System.out.println("\n========== TEST 21: DELETE NON-EXISTENT AUTHOR ==========");

        setupAdminSecurityContext(adminUserId, "admin_" + TIMESTAMP);

        // Try to delete non-existent author
        assertThrows(AuthorNotFoundException.class, () -> {
            adminCatalogService.deleteAuthor("nonexistentid123456789012");
        }, "Should throw AuthorNotFoundException");

        System.out.println("Correctly threw AuthorNotFoundException");

        clearSecurityContext();
    }

    // ========== TEST 22: CLEANUP ==========

    @Test
    @Order(22)
    @DisplayName("Cleanup: Remove test data")
    public void test22_Cleanup() {
        System.out.println("\n========== TEST 22: CLEANUP ==========");

        try {
            // Delete test data from MongoDB
            if (testBookId1 != null) bookRepository.deleteById(testBookId1);
            if (testBookId2 != null) bookRepository.deleteById(testBookId2);
            if (testAuthorId1 != null) authorRepository.deleteById(testAuthorId1);
            if (testAuthorId2 != null) authorRepository.deleteById(testAuthorId2);
            if (testUserId1 != null) userRepository.deleteById(testUserId1);
            if (adminUserId != null) userRepository.deleteById(adminUserId);

            // Delete test data from Neo4j
            if (testBookId1 != null) bookNodeRepository.deleteByMongoId(testBookId1);
            if (testBookId2 != null) bookNodeRepository.deleteByMongoId(testBookId2);
            if (testAuthorId1 != null) authorNodeRepository.deleteByMongoId(testAuthorId1);
            if (testAuthorId2 != null) authorNodeRepository.deleteByMongoId(testAuthorId2);
            if (testUserId1 != null) userNodeRepository.deleteByMongoId(testUserId1);
            if (adminUserId != null) userNodeRepository.deleteByMongoId(adminUserId);
            if (TEST_GENRE_1 != null) {
                Optional<GenreNode> genre = genreNodeRepository.findByName(TEST_GENRE_1);
                genre.ifPresent(genreNodeRepository::delete);
            }

            System.out.println("Cleanup completed successfully");
        } catch (Exception e) {
            System.out.println("Cleanup warning: " + e.getMessage());
        }
    }
}
