package it.unipi.bookSphere.registered;

import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.model.mongodb.RegisteredUser;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.mongo.RegisteredUserRepository;
import it.unipi.bookSphere.repository.neo4j.UserNodeRepository;
import it.unipi.bookSphere.service.registered.BookshelfService;
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
 * Functional Test for Bookshelf APIs (REGISTERED - Requires authentication)
 * Tests:
 * - Add book to bookshelf (POST /api/v1/me/bookshelf)
 * - Update book status (PATCH /api/v1/me/bookshelf/{bookID})
 * - Remove book from bookshelf (DELETE /api/v1/me/bookshelf/{bookId})
 * - Validation of status values
 * - Duplicate book handling
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookshelfControllerTest {

    @Autowired
    private BookshelfService bookshelfService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private RegisteredUserRepository userRepository;

    @Autowired
    private UserNodeRepository userNodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_PREFIX = "BookshelfTest_";
    private static String testUserId;
    private static String testBookId;
    private static String testBook2Id;
    private static String testAuthorId;

    @Test
    @Order(1)
    @DisplayName("01. Cleanup - Remove previous test data")
    void test01_Cleanup() {
        System.out.println("\n=== TEST 01: Cleanup ===");

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

        // Create test book 1
        BookDocument book1 = new BookDocument();
        book1.setTitle(TEST_PREFIX + "Test Book 1");
        book1.setAuthor(new BookDocument.Author(testAuthorId, author.getName()));
        book1.setGenres(List.of("Fiction"));
        book1.setPublicationYear(2024);
        book1.setReviews(new ArrayList<>());
        book1.setStatsPerYear(new ArrayList<>());
        book1.setAvailability("ACTIVE");
        book1 = bookRepository.save(book1);
        testBookId = book1.getId();
        System.out.println("Created test book 1: " + book1.getTitle());

        // Create test book 2
        BookDocument book2 = new BookDocument();
        book2.setTitle(TEST_PREFIX + "Test Book 2");
        book2.setAuthor(new BookDocument.Author(testAuthorId, author.getName()));
        book2.setGenres(List.of("Science"));
        book2.setPublicationYear(2024);
        book2.setReviews(new ArrayList<>());
        book2.setStatsPerYear(new ArrayList<>());
        book2.setAvailability("ACTIVE");
        book2 = bookRepository.save(book2);
        testBook2Id = book2.getId();
        System.out.println("Created test book 2: " + book2.getTitle());

        // Setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");
    }

    @Test
    @Order(3)
    @DisplayName("03. Add book to bookshelf - to_read status - Success")
    void test03_AddBookToBookshelf_ToRead() {
        System.out.println("\n=== TEST 03: Add Book to Bookshelf (to_read) ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        bookshelfService.addBookToBookshelf(testBookId, "to_read");

        // Verify in MongoDB
        RegisteredUser user = userRepository.findById(testUserId).orElse(null);
        assertNotNull(user);
        assertNotNull(user.getBookshelf());
        assertTrue(user.getBookshelf().stream()
                .anyMatch(b -> b.getBookId().equals(testBookId) && "to_read".equals(b.getStatus())));

        System.out.println("Book added to bookshelf with status: to_read");
    }

    @Test
    @Order(4)
    @DisplayName("04. Add book to bookshelf - Duplicate (should fail)")
    void test04_AddBookToBookshelf_Duplicate() {
        System.out.println("\n=== TEST 04: Add Duplicate Book to Bookshelf ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        Exception exception = assertThrows(Exception.class, () -> {
            bookshelfService.addBookToBookshelf(testBookId, "reading");
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("05. Add another book - reading status - Success")
    void test05_AddAnotherBook_Reading() {
        System.out.println("\n=== TEST 05: Add Another Book (reading) ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        bookshelfService.addBookToBookshelf(testBook2Id, "reading");

        // Verify in MongoDB
        RegisteredUser user = userRepository.findById(testUserId).orElse(null);
        assertNotNull(user);
        assertEquals(2, user.getBookshelf().size());
        assertTrue(user.getBookshelf().stream()
                .anyMatch(b -> b.getBookId().equals(testBook2Id) && "reading".equals(b.getStatus())));

        System.out.println("Another book added to bookshelf with status: reading");
    }

    @Test
    @Order(6)
    @DisplayName("06. Update book status - to_read -> reading - Success")
    void test06_UpdateBookStatus_ToReadToReading() {
        System.out.println("\n=== TEST 06: Update Book Status (to_read -> reading) ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        bookshelfService.updateBookStatus(testBookId, "reading");

        // Verify in MongoDB
        RegisteredUser user = userRepository.findById(testUserId).orElse(null);
        assertNotNull(user);
        var bookEntry = user.getBookshelf().stream()
                .filter(b -> b.getBookId().equals(testBookId))
                .findFirst();
        assertTrue(bookEntry.isPresent());
        assertEquals("reading", bookEntry.get().getStatus());

        System.out.println("Book status updated to: reading");
    }

    @Test
    @Order(7)
    @DisplayName("07. Update book status - reading -> read - Success")
    void test07_UpdateBookStatus_ReadingToRead() {
        System.out.println("\n=== TEST 07: Update Book Status (reading -> read) ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        bookshelfService.updateBookStatus(testBookId, "read");

        // Verify in MongoDB
        RegisteredUser user = userRepository.findById(testUserId).orElse(null);
        assertNotNull(user);
        var bookEntry = user.getBookshelf().stream()
                .filter(b -> b.getBookId().equals(testBookId))
                .findFirst();
        assertTrue(bookEntry.isPresent());
        assertEquals("read", bookEntry.get().getStatus());

        System.out.println("Book status updated to: read");
    }

    @Test
    @Order(8)
    @DisplayName("08. Update book status - Invalid status (should fail)")
    void test08_UpdateBookStatus_InvalidStatus() {
        System.out.println("\n=== TEST 08: Update Book Status - Invalid ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        Exception exception = assertThrows(Exception.class, () -> {
            bookshelfService.updateBookStatus(testBookId, "invalid_status");
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(9)
    @DisplayName("09. Update book status - Book not in bookshelf (should fail)")
    void test09_UpdateBookStatus_NotInBookshelf() {
        System.out.println("\n=== TEST 09: Update Status - Book Not In Bookshelf ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        String fakeBookId = "000000000000000000000000";

        Exception exception = assertThrows(Exception.class, () -> {
            bookshelfService.updateBookStatus(fakeBookId, "read");
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(10)
    @DisplayName("10. Remove book from bookshelf - Success")
    void test10_RemoveBookFromBookshelf_Success() {
        System.out.println("\n=== TEST 10: Remove Book from Bookshelf ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        bookshelfService.removeBookFromBookshelf(testBookId);

        // Verify removed from MongoDB
        RegisteredUser user = userRepository.findById(testUserId).orElse(null);
        assertNotNull(user);
        assertEquals(1, user.getBookshelf().size());
        assertFalse(user.getBookshelf().stream()
                .anyMatch(b -> b.getBookId().equals(testBookId)));

        System.out.println("Book removed from bookshelf successfully");
    }

    @Test
    @Order(11)
    @DisplayName("11. Remove book - Not in bookshelf (should fail)")
    void test11_RemoveBookFromBookshelf_NotInBookshelf() {
        System.out.println("\n=== TEST 11: Remove Book Not In Bookshelf ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        Exception exception = assertThrows(Exception.class, () -> {
            bookshelfService.removeBookFromBookshelf(testBookId); // Already removed
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(12)
    @DisplayName("12. Add book with invalid status (should fail)")
    void test12_AddBookWithInvalidStatus() {
        System.out.println("\n=== TEST 12: Add Book With Invalid Status ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        Exception exception = assertThrows(Exception.class, () -> {
            bookshelfService.addBookToBookshelf(testBookId, "completed");
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(13)
    @DisplayName("13. Add non-existent book (should fail)")
    void test13_AddNonExistentBook() {
        System.out.println("\n=== TEST 13: Add Non-existent Book ===");

        // Re-setup authentication
        setupAuthentication(testUserId, TEST_PREFIX + "User");

        String fakeBookId = "000000000000000000000000";

        Exception exception = assertThrows(Exception.class, () -> {
            bookshelfService.addBookToBookshelf(fakeBookId, "to_read");
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

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
            userRepository.deleteById(testUserId);
            userNodeRepository.deleteByMongoId(testUserId);
            System.out.println("Deleted test user");
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
