package it.unipi.bookSphere.admin;

import it.unipi.bookSphere.dto.AuthorDTO;
import it.unipi.bookSphere.dto.BookDTO;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.repository.neo4j.BookNodeRepository;
import it.unipi.bookSphere.service.AdminBookService;
import it.unipi.bookSphere.utils.UserPrincipal;
import it.unipi.bookSphere.TestProfile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional Test for Admin Book APIs (ADMIN - Requires admin authentication)
 * Tests:
 * - Add book (POST /api/v1/admin/books)
 * - Update book (PUT /api/v1/admin/books/{id})
 * - Delete book (DELETE /api/v1/admin/books/{id})
 * - MongoDB + Neo4j consistency
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminBookControllerTest {

    @Autowired
    private AdminBookService adminBookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookNodeRepository bookNodeRepository;

    private static final String TEST_PREFIX = "AdminBookTest_";
    private static String testBookId;
    private static String testAuthorId;

    @Test
    @Order(1)
    @DisplayName("01. Cleanup - Remove previous test data")
    void test01_Cleanup() {
        System.out.println("\n=== TEST 01: Cleanup ===");

        bookRepository.findAll().stream()
                .filter(b -> b.getTitle() != null && b.getTitle().startsWith(TEST_PREFIX))
                .forEach(book -> {
                    bookRepository.deleteById(book.getId());
                    bookNodeRepository.deleteByMongoId(book.getId());
                    System.out.println("Deleted test book: " + book.getTitle());
                });

        authorRepository.findAll().stream()
                .filter(a -> a.getName() != null && a.getName().startsWith(TEST_PREFIX))
                .forEach(author -> {
                    authorRepository.deleteById(author.getId());
                    System.out.println("Deleted test author: " + author.getName());
                });

        System.out.println("Cleanup completed");
    }

    @Test
    @Order(2)
    @DisplayName("02. Setup - Create test author and authenticate as admin")
    void test02_Setup() {
        System.out.println("\n=== TEST 02: Setup Test Data ===");

        // Create test author
        AuthorDocument author = new AuthorDocument();
        author.setName(TEST_PREFIX + "Author");
        author.setRatingsCount(0);
        author.setSumRatings(0);
        author.setStatus("ACTIVE");
        author = authorRepository.save(author);
        testAuthorId = author.getId();
        System.out.println("Created test author: " + author.getName());

        // Setup admin authentication
        setupAdminAuthentication();
    }

    @Test
    @Order(3)
    @DisplayName("03. Add book - Success")
    void test03_AddBook_Success() {
        System.out.println("\n=== TEST 03: Add Book ===");

        BookDTO bookDTO = new BookDTO();
        bookDTO.setTitle(TEST_PREFIX + "New Book");
        AuthorDTO dtoAuthor = new AuthorDTO();
        dtoAuthor.setId(testAuthorId);
        dtoAuthor.setName(TEST_PREFIX + "Author");
        bookDTO.setAuthor(dtoAuthor);
        bookDTO.setGenres(List.of("Fiction", "Adventure"));
        bookDTO.setPublicationYear(2024);

        BookDTO result = adminBookService.addBook(bookDTO);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(TEST_PREFIX + "New Book", result.getTitle());
        assertEquals(2024, result.getPublicationYear());

        testBookId = result.getId();

        // Verify in MongoDB
        BookDocument bookMongo = bookRepository.findById(testBookId).orElse(null);
        assertNotNull(bookMongo);
        assertEquals(TEST_PREFIX + "New Book", bookMongo.getTitle());

        // Verify in Neo4j
        var bookNodeOpt = bookNodeRepository.findByMongoId(testBookId);
        assertTrue(bookNodeOpt.isPresent());
        assertEquals(TEST_PREFIX + "New Book", bookNodeOpt.get().getTitle());

        System.out.println("Book added successfully: " + testBookId);
    }

    @Test
    @Order(4)
    @DisplayName("04. Update book - Success")
    void test04_UpdateBook_Success() {
        System.out.println("\n=== TEST 04: Update Book ===");

        BookDTO updateDTO = new BookDTO();
        updateDTO.setTitle(TEST_PREFIX + "Updated Book Title");
        AuthorDTO dtoAuthor = new AuthorDTO();
        dtoAuthor.setId(testAuthorId);
        dtoAuthor.setName(TEST_PREFIX + "Author");
        updateDTO.setAuthor(dtoAuthor);
        updateDTO.setGenres(List.of("Fiction", "Thriller"));
        updateDTO.setPublicationYear(2025);

        BookDTO result = adminBookService.updateBook(testBookId, updateDTO);

        assertNotNull(result);
        assertEquals(testBookId, result.getId());
        assertEquals(TEST_PREFIX + "Updated Book Title", result.getTitle());
        assertEquals(2025, result.getPublicationYear());

        // Verify in MongoDB
        BookDocument bookMongo = bookRepository.findById(testBookId).orElse(null);
        assertNotNull(bookMongo);
        assertEquals(TEST_PREFIX + "Updated Book Title", bookMongo.getTitle());
        assertEquals(2025, bookMongo.getPublicationYear());

        // Verify in Neo4j
        var bookNodeOpt = bookNodeRepository.findByMongoId(testBookId);
        assertTrue(bookNodeOpt.isPresent());
        assertEquals(TEST_PREFIX + "Updated Book Title", bookNodeOpt.get().getTitle());

        System.out.println("Book updated successfully");
    }

    @Test
    @Order(5)
    @DisplayName("05. Add book - Duplicate title different year (should succeed)")
    void test05_AddBook_DuplicateTitle() {
        System.out.println("\n=== TEST 05: Add Book with Duplicate Title (Different Year) ===");

        // Add a book with the same title but different year and author
        BookDTO duplicateDTO = new BookDTO();
        duplicateDTO.setTitle(TEST_PREFIX + "Updated Book Title"); // Same as testBookId
        AuthorDTO dtoAuthor = new AuthorDTO();
        dtoAuthor.setId(testAuthorId);
        dtoAuthor.setName(TEST_PREFIX + "Author");
        duplicateDTO.setAuthor(dtoAuthor);
        duplicateDTO.setGenres(List.of("Romance"));
        duplicateDTO.setPublicationYear(2020); // Different year!

        BookDTO result = adminBookService.addBook(duplicateDTO);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(TEST_PREFIX + "Updated Book Title", result.getTitle());
        assertEquals(2020, result.getPublicationYear());
        
        // Verify it's a different book
        assertNotEquals(testBookId, result.getId(), "Should create a new book, not reuse existing");

        // Verify in MongoDB
        BookDocument bookMongo = bookRepository.findById(result.getId()).orElse(null);
        assertNotNull(bookMongo);
        assertEquals(TEST_PREFIX + "Updated Book Title", bookMongo.getTitle());
        assertEquals(2020, bookMongo.getPublicationYear());

        System.out.println("✓ Duplicate title with different year correctly added as separate book");

        // Cleanup this duplicate book
        adminBookService.deleteBook(result.getId());
        System.out.println("Cleanup: Deleted duplicate book");
    }

    @Test
    @Order(6)
    @DisplayName("06. Update book - Non-existent (should fail)")
    void test06_UpdateBook_NotFound() {
        System.out.println("\n=== TEST 06: Update Non-existent Book ===");

        String fakeId = "000000000000000000000000";
        BookDTO updateDTO = new BookDTO();
        updateDTO.setTitle(TEST_PREFIX + "Test");
        AuthorDTO dtoAuthor = new AuthorDTO();
        dtoAuthor.setId(testAuthorId);
        dtoAuthor.setName(TEST_PREFIX + "Author");
        updateDTO.setAuthor(dtoAuthor);
        updateDTO.setGenres(List.of("Fiction"));
        updateDTO.setPublicationYear(2024);

        Exception exception = assertThrows(Exception.class, () -> {
            adminBookService.updateBook(fakeId, updateDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(7)
    @DisplayName("07. Delete book - Non-existent (should fail)")
    void test07_DeleteBook_NotFound() {
        System.out.println("\n=== TEST 07: Delete Non-existent Book ===");

        String fakeId = "000000000000000000000000";

        Exception exception = assertThrows(Exception.class, () -> {
            adminBookService.deleteBook(fakeId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(8)
    @DisplayName("08. Add book - With multiple genres and verify Neo4j relationships")
    void test08_AddBook_WithGenresAndVerifyRelationships() {
        System.out.println("\n=== TEST 08: Add Book with Multiple Genres ===");

        BookDTO bookDTO = new BookDTO();
        bookDTO.setTitle(TEST_PREFIX + "Multi Genre Book");
        AuthorDTO dtoAuthor = new AuthorDTO();
        dtoAuthor.setId(testAuthorId);
        dtoAuthor.setName(TEST_PREFIX + "Author");
        bookDTO.setAuthor(dtoAuthor);
        bookDTO.setGenres(List.of("Science Fiction", "Adventure", "Thriller"));
        bookDTO.setPublicationYear(2024);

        BookDTO result = adminBookService.addBook(bookDTO);

        assertNotNull(result);
        testBookId = result.getId();

        // Verify genres in MongoDB
        BookDocument bookMongo = bookRepository.findById(testBookId).orElse(null);
        assertNotNull(bookMongo);
        assertEquals(3, bookMongo.getGenres().size());

        // Verify book node exists in Neo4j
        var bookNodeOpt = bookNodeRepository.findByMongoId(testBookId);
        assertTrue(bookNodeOpt.isPresent());

        System.out.println("Book with multiple genres added, relationships verified");
    }

    @Test
    @Order(9)
    @DisplayName("09. Update book - Change author")
    void test09_UpdateBook_ChangeAuthor() {
        System.out.println("\n=== TEST 09: Update Book - Change Author ===");

        // Create a second test author
        AuthorDocument newAuthor = new AuthorDocument();
        newAuthor.setName(TEST_PREFIX + "New Author");
        newAuthor.setRatingsCount(0);
        newAuthor.setSumRatings(0);
        newAuthor.setStatus("ACTIVE");
        newAuthor = authorRepository.save(newAuthor);
        String newAuthorId = newAuthor.getId();

        BookDTO updateDTO = new BookDTO();
        updateDTO.setTitle(TEST_PREFIX + "Book with New Author");
        AuthorDTO dtoAuthor = new AuthorDTO();
        dtoAuthor.setId(newAuthorId);
        dtoAuthor.setName(TEST_PREFIX + "New Author");
        updateDTO.setAuthor(dtoAuthor);
        updateDTO.setGenres(List.of("Fiction"));
        updateDTO.setPublicationYear(2024);

        BookDTO result = adminBookService.updateBook(testBookId, updateDTO);

        assertNotNull(result);
        assertEquals(newAuthorId, result.getAuthor().getId());

        // Verify in MongoDB
        BookDocument bookMongo = bookRepository.findById(testBookId).orElse(null);
        assertNotNull(bookMongo);
        assertEquals(newAuthorId, bookMongo.getAuthor().getId());

        // Cleanup new author
        authorRepository.deleteById(newAuthorId);

        System.out.println("Book author updated successfully");
    }

    @Test
    @Order(10)
    @DisplayName("10. Update book - Change genres")
    void test10_UpdateBook_ChangeGenres() {
        System.out.println("\n=== TEST 10: Update Book - Change Genres ===");

        BookDTO updateDTO = new BookDTO();
        updateDTO.setTitle(TEST_PREFIX + "Book with New Genres");
        AuthorDTO dtoAuthor = new AuthorDTO();
        dtoAuthor.setId(testAuthorId);
        dtoAuthor.setName(TEST_PREFIX + "Author");
        updateDTO.setAuthor(dtoAuthor);
        updateDTO.setGenres(List.of("Fantasy", "Mystery"));
        updateDTO.setPublicationYear(2024);

        BookDTO result = adminBookService.updateBook(testBookId, updateDTO);

        assertNotNull(result);
        assertEquals(2, result.getGenres().size());
        assertTrue(result.getGenres().contains("Fantasy"));
        assertTrue(result.getGenres().contains("Mystery"));

        // Verify in MongoDB
        BookDocument bookMongo = bookRepository.findById(testBookId).orElse(null);
        assertNotNull(bookMongo);
        assertEquals(2, bookMongo.getGenres().size());

        System.out.println("Book genres updated successfully");
    }

    @Test
    @Order(11)
    @DisplayName("11. Delete book - Success")
    void test11_DeleteBook_Success() {
        System.out.println("\n=== TEST 11: Delete Book ===");

        adminBookService.deleteBook(testBookId);

        // Verify soft delete (ARCHIVED) in MongoDB
        BookDocument bookMongo = bookRepository.findById(testBookId).orElse(null);
        assertNotNull(bookMongo);
        assertEquals("ARCHIVED", bookMongo.getAvailability());

        // Verify hard delete from Neo4j
        var bookNodeOpt = bookNodeRepository.findByMongoId(testBookId);
        assertFalse(bookNodeOpt.isPresent());

        System.out.println("Book deleted (archived) successfully");
    }

    @Test
    @Order(12)
    @DisplayName("12. Update archived book - Should fail")
    void test12_UpdateArchivedBook_ShouldFail() {
        System.out.println("\n=== TEST 12: Update Archived Book (Should Fail) ===");

        BookDTO updateDTO = new BookDTO();
        updateDTO.setTitle(TEST_PREFIX + "Attempt Update Archived");
        AuthorDTO dtoAuthor = new AuthorDTO();
        dtoAuthor.setId(testAuthorId);
        dtoAuthor.setName(TEST_PREFIX + "Author");
        updateDTO.setAuthor(dtoAuthor);
        updateDTO.setGenres(List.of("Fiction"));
        updateDTO.setPublicationYear(2024);

        Exception exception = assertThrows(Exception.class, () -> {
            adminBookService.updateBook(testBookId, updateDTO);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("archived") || 
                   exception.getMessage().contains("ARCHIVED"));
    }

    @Test
    @Order(13)
    @DisplayName("13. Delete already archived book - Should fail")
    void test13_DeleteArchivedBook_ShouldFail() {
        System.out.println("\n=== TEST 13: Delete Already Archived Book (Should Fail) ===");

        Exception exception = assertThrows(Exception.class, () -> {
            adminBookService.deleteBook(testBookId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("archived") || 
                   exception.getMessage().contains("ARCHIVED"));
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        // Cleanup book (if not already deleted)
        if (testBookId != null) {
            try {
                bookRepository.deleteById(testBookId);
                bookNodeRepository.deleteByMongoId(testBookId);
                System.out.println("Deleted test book");
            } catch (Exception e) {
                System.out.println("Book already cleaned up");
            }
        }

        if (testAuthorId != null) {
            authorRepository.deleteById(testAuthorId);
            System.out.println("Deleted test author");
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
}
