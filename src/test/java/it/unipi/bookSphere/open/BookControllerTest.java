package it.unipi.bookSphere.open;

import it.unipi.bookSphere.dto.BookDTO;
import it.unipi.bookSphere.model.mongodb.AuthorDocument;
import it.unipi.bookSphere.model.mongodb.BookDocument;
import it.unipi.bookSphere.repository.mongo.AuthorRepository;
import it.unipi.bookSphere.repository.mongo.BookRepository;
import it.unipi.bookSphere.service.open.BookService;
import it.unipi.bookSphere.TestProfile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional Test for Book APIs (OPEN - No authentication required)
 * Tests:
 * - Get book by ID (GET /api/v1/books/{id})
 * - Search books by title (GET /api/v1/books?title=...)
 * - Pagination
 * - Error handling for non-existent books
 */
@SpringBootTest
@TestProfile
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookControllerTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    private static final String TEST_PREFIX = "BookTest ";
    private static String testBookId1;
    private static String testBookId2;
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

        System.out.println("Cleanup completed");
    }

    @Test
    @Order(2)
    @DisplayName("02. Setup - Create test data")
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

        // Create test book 1
        BookDocument book1 = new BookDocument();
        book1.setTitle(TEST_PREFIX + "The Fellowship of the Ring");
        book1.setAuthor(new BookDocument.Author(testAuthorId, author.getName()));
        book1.setGenres(List.of("Fantasy", "Adventure"));
        book1.setPublicationYear(2001);
        book1.setReviews(new ArrayList<>());
        book1.setStatsPerYear(new ArrayList<>());
        book1.setAvailability("ACTIVE");
        book1 = bookRepository.save(book1);
        testBookId1 = book1.getId();
        System.out.println("Created test book 1: " + book1.getTitle());

        // Create test book 2
        BookDocument book2 = new BookDocument();
        book2.setTitle(TEST_PREFIX + "The Two Towers");
        book2.setAuthor(new BookDocument.Author(testAuthorId, author.getName()));
        book2.setGenres(List.of("Fantasy", "Adventure"));
        book2.setPublicationYear(2002);
        book2.setReviews(new ArrayList<>());
        book2.setAvailability("ACTIVE");
        book2.setStatsPerYear(new ArrayList<>());
        book2 = bookRepository.save(book2);
        testBookId2 = book2.getId();
        System.out.println("Created test book 2: " + book2.getTitle());
    }

    @Test
    @Order(3)
    @DisplayName("03. Get book by ID - Success")
    void test03_GetBookById_Success() {
        System.out.println("\n=== TEST 03: Get Book by ID ===");

        BookDTO book = bookService.findById(testBookId1);

        assertNotNull(book);
        assertEquals(testBookId1, book.getId());
        assertEquals(TEST_PREFIX + "The Fellowship of the Ring", book.getTitle());
        assertEquals(TEST_PREFIX + "Author", book.getAuthor().getName());
        assertEquals(2001, book.getPublicationYear());
        assertTrue(book.getGenres().contains("Fantasy"));

        System.out.println("Book retrieved: " + book.getTitle());
    }

    @Test
    @Order(4)
    @DisplayName("04. Get book by ID - Not found")
    void test04_GetBookById_NotFound() {
        System.out.println("\n=== TEST 04: Get Non-existent Book ===");

        String fakeId = "000000000000000000000000";

        Exception exception = assertThrows(Exception.class, () -> {
            bookService.findById(fakeId);
        });

        System.out.println("Expected error caught: " + exception.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("05. Search books by title - Full match")
    void test05_SearchByTitle_FullMatch() {
        System.out.println("\n=== TEST 05: Search Books by Title (Full Match) ===");

        Page<BookDTO> books = bookService.searchByTitle(TEST_PREFIX + "The Fellowship", 0, 20);

        assertNotNull(books);
        assertTrue(books.getTotalElements() >= 1);
        assertTrue(books.getContent().stream()
                .anyMatch(b -> b.getTitle().equals(TEST_PREFIX + "The Fellowship of the Ring")));

        System.out.println("Found " + books.getTotalElements() + " book(s)");
    }

    @Test
    @Order(6)
    @DisplayName("06. Search books by title - Partial match")
    void test06_SearchByTitle_PartialMatch() {
        System.out.println("\n=== TEST 06: Search Books by Title (Partial Match) ===");

        // Text search works with complete words, so we search for "BookTest" (without underscore)
        Page<BookDTO> books = bookService.searchByTitle("BookTest", 0, 20);

        assertNotNull(books);
        assertTrue(books.getTotalElements() >= 2);
        System.out.println("Found " + books.getTotalElements() + " book(s) with text search");
    }

    @Test
    @Order(7)
    @DisplayName("07. Search books - No results")
    void test07_SearchByTitle_NoResults() {
        System.out.println("\n=== TEST 07: Search Books (No Results) ===");

        Page<BookDTO> books = bookService.searchByTitle("NonExistentBookTitle123456", 0, 20);

        assertNotNull(books);
        assertEquals(0, books.getTotalElements());
        System.out.println("No books found as expected");
    }

    @Test
    @Order(8)
    @DisplayName("08. Search books - Pagination")
    void test08_SearchByTitle_Pagination() {
        System.out.println("\n=== TEST 08: Search Books with Pagination ===");

        // Page 0, size 1 - Text search works with complete words
        Page<BookDTO> page1 = bookService.searchByTitle("BookTest", 0, 1);
        assertEquals(1, page1.getContent().size());
        assertTrue(page1.getTotalElements() >= 2);

        // Page 1, size 1
        Page<BookDTO> page2 = bookService.searchByTitle("BookTest", 1, 1);
        assertEquals(1, page2.getContent().size());

        // Verify different books
        assertNotEquals(page1.getContent().get(0).getId(), page2.getContent().get(0).getId());

        System.out.println("Pagination working correctly");
    }

    @Test
    @Order(99)
    @DisplayName("99. Final Cleanup - Remove all test data")
    void test99_FinalCleanup() {
        System.out.println("\n=== TEST 99: Final Cleanup ===");

        // Remove test books
        if (testBookId1 != null) {
            bookRepository.deleteById(testBookId1);
            System.out.println("Deleted test book 1");
        }
        if (testBookId2 != null) {
            bookRepository.deleteById(testBookId2);
            System.out.println("Deleted test book 2");
        }

        // Remove test author
        if (testAuthorId != null) {
            authorRepository.deleteById(testAuthorId);
            System.out.println("Deleted test author");
        }

        System.out.println("Final cleanup completed - All test data removed");
    }
}
